package com.example.ecommerce.security;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Fixed-window rate limits for the public auth endpoints, keyed by client IP and counted in Redis (so the
 * limit holds across several API instances). Over the limit the client gets 429 {@code RATE_LIMITED}
 * with a {@code Retry-After} header. If Redis is unreachable the request is let through and a warning is
 * logged: an outage of the cache must not lock every customer out.
 * <p>
 * Behind a reverse proxy, {@code server.forward-headers-strategy=native} (prod profile) makes
 * {@code getRemoteAddr()} the real client address.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /** Rule name (as configured in app.rate-limit.rules) for each limited POST endpoint. */
    private static final Map<String, String> RULE_BY_PATH = Map.of(
            "/api/v1/auth/login", "login",
            "/api/v1/auth/register", "register",
            "/api/v1/auth/refresh", "refresh",
            "/api/v1/auth/forgot-password", "forgot-password",
            "/api/v1/auth/reset-password", "reset-password");

    /** INCR and set the window on the first hit, atomically; returns {count, milliseconds left}. */
    private static final RedisScript<List> COUNT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return {count, redis.call('PTTL', KEYS[1])}
            """, List.class);

    private final AppProperties.RateLimit config;
    private final StringRedisTemplate redis;
    private final SecurityErrorWriter errorWriter;

    public RateLimitFilter(AppProperties.RateLimit config, StringRedisTemplate redis, SecurityErrorWriter errorWriter) {
        this.config = config;
        this.redis = redis;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !config.enabled() || !HttpMethod.POST.matches(request.getMethod())
                || !RULE_BY_PATH.containsKey(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruleName = RULE_BY_PATH.get(request.getRequestURI().substring(request.getContextPath().length()));
        AppProperties.RateLimit.Rule rule = config.rules().get(ruleName);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = "ecommerce:rate:" + ruleName + ":" + request.getRemoteAddr();
        List<?> result;
        try {
            result = redis.execute(COUNT_SCRIPT, List.of(key), String.valueOf(rule.window().toMillis()));
        } catch (RuntimeException ex) {
            log.warn("Rate limiting skipped, Redis unavailable: {}", ex.getMessage());
            chain.doFilter(request, response);
            return;
        }

        long count = ((Number) result.get(0)).longValue();
        if (count > rule.limit()) {
            long millisLeft = Math.max(((Number) result.get(1)).longValue(), 0);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, (millisLeft + 999) / 1000)));
            errorWriter.write(request, response, ErrorCode.RATE_LIMITED, "Too many attempts. Please wait and try again");
            return;
        }
        chain.doFilter(request, response);
    }
}
