package org.example.panhabackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs method, path, status and duration of every API call, and tags log lines with a request id
 * (returned to the client as X-Request-Id). Request bodies are never logged, so passwords stay out of logs.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            log.info("[{}] {} {} -> {} ({} ms)", requestId, request.getMethod(), request.getRequestURI(),
                    response.getStatus(), millis);
            MDC.remove("requestId");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }
}
