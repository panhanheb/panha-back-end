package com.example.ecommerce.common.security;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Authenticates {@code Authorization: Bearer <access token>} requests.
 * <ul>
 *   <li>No header: the request continues anonymously (public endpoints work, protected ones answer 401).</li>
 *   <li>Expired token: 401 {@code TOKEN_EXPIRED}, so clients know to refresh.</li>
 *   <li>Bad signature, or a session ended by logout-all, password change, disabling or a role change
 *       (session version mismatch): 401 {@code INVALID_TOKEN}.</li>
 * </ul>
 * The sign-in, refresh and password-recovery endpoints skip this filter, so a client that still sends its
 * expired token while refreshing is not rejected.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    static final Set<String> TOKEN_FREE_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/logout");

    private final JwtService jwtService;
    private final SessionVersionLookup sessionVersionLookup;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, SessionVersionLookup sessionVersionLookup,
                                   SecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.sessionVersionLookup = sessionVersionLookup;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return TOKEN_FREE_PATHS.contains(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            chain.doFilter(request, response);
            return;
        }

        JwtService.AccessClaims claims;
        try {
            claims = jwtService.parseAccessToken(header.substring(BEARER.length()).trim());
        } catch (BusinessException ex) {
            errorWriter.write(request, response, ex.getCode(), ex.getMessage());
            return;
        }

        Optional<Integer> currentVersion = sessionVersionLookup.activeSessionVersion(claims.userId());
        if (currentVersion.isEmpty() || currentVersion.get() != claims.sessionVersion()) {
            errorWriter.write(request, response, ErrorCode.INVALID_TOKEN, "This session has ended. Please sign in again");
            return;
        }

        UserPrincipal principal = new UserPrincipal(claims.userId(), claims.username(), "", true,
                claims.sessionVersion(), claims.roles());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        chain.doFilter(request, response);
    }
}
