package org.example.panhabackend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;access token&gt;".
 * Invalid tokens leave the request unauthenticated so the entry point answers 401.
 * Deliberately not a Spring bean, so it is not also registered as a plain servlet filter.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute read by {@link RestAuthenticationEntryPoint} to explain a 401. */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            JwtService.TokenClaims claims = jwtService.parse(token, JwtService.TokenType.ACCESS);
            UserPrincipal principal = userDetailsService.loadUserByUsername(claims.username());

            if (!principal.isEnabled()) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Account is disabled");
                return;
            }
            if (principal.getTokenVersion() != claims.tokenVersion() || !principal.getId().equals(claims.userId())) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token has been revoked");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid access token");
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Account no longer exists");
        }
    }
}
