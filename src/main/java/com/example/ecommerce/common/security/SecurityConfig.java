package com.example.ecommerce.common.security;

import com.example.ecommerce.common.config.AppProperties;
import com.example.ecommerce.common.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Stateless JWT security.
 * <ul>
 *   <li>Public: sign-up, sign-in, refresh, password recovery, health, API docs.</li>
 *   <li>{@code /api/v1/admin/**}: ADMIN (SUPER_ADMIN inherits it through the role hierarchy).</li>
 *   <li>Everything else: any signed-in user; finer rules use {@code @PreAuthorize} and ownership checks.</li>
 * </ul>
 * Errors are JSON in the standard envelope, never HTML or a login redirect.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/error"
    };

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/logout"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   JwtService jwtService,
                                                   SessionVersionLookup sessionVersionLookup,
                                                   SecurityErrorWriter errorWriter,
                                                   StringRedisTemplate redis,
                                                   AppProperties appProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // no cookies: bearer tokens are not sent automatically
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> errorWriter.write(request, response,
                                ErrorCode.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, ex) -> errorWriter.write(request, response,
                                ErrorCode.FORBIDDEN, "You do not have permission to perform this action")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new RateLimitFilter(appProperties.rateLimit(), redis, errorWriter),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, sessionVersionLookup, errorWriter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** SUPER_ADMIN can do everything ADMIN can; used by URL rules and {@code @PreAuthorize}. */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Idempotency-Key", "X-Request-Id"));
        config.setExposedHeaders(List.of("Location", "X-Request-Id", "Retry-After"));
        config.setAllowCredentials(false); // bearer tokens, no cookies
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
