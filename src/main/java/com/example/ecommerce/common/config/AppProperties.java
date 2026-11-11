package com.example.ecommerce.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Strongly typed binding of all {@code app.*} properties (see application.yml).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid @NotNull Jwt jwt,
                            @Valid @NotNull RateLimit rateLimit,
                            @Valid @NotNull Cors cors,
                            @Valid @NotNull Shop shop,
                            @Valid @NotNull Storage storage,
                            PasswordReset passwordReset,
                            Bootstrap bootstrap) {

    /**
     * @param secret          Base64 HMAC key of at least 256 bits ({@code openssl rand -base64 64})
     * @param accessTokenTtl  lifetime of access tokens (short: they cannot be revoked one by one)
     * @param refreshTokenTtl lifetime of refresh tokens (each use replaces the token)
     */
    public record Jwt(@NotBlank String secret,
                      @NotBlank String issuer,
                      @NotNull Duration accessTokenTtl,
                      @NotNull Duration refreshTokenTtl) {
    }

    /**
     * Per-client-IP limits on the public auth endpoints, counted in Redis so they hold across instances.
     *
     * @param rules limit per rule name: login, register, refresh, forgot-password, reset-password
     */
    public record RateLimit(boolean enabled, @NotNull Map<String, Rule> rules) {

        public record Rule(@Min(1) int limit, @NotNull Duration window) {
        }
    }

    /** Browser origins allowed to call the API (web storefront, admin dashboard). Mobile apps are unaffected. */
    public record Cors(@NotNull List<String> allowedOrigins) {
    }

    /**
     * @param currency              ISO 4217 code shown on carts and orders
     * @param shippingFee           flat shipping fee per order
     * @param freeShippingThreshold subtotal from which shipping is free; 0 disables free shipping
     */
    public record Shop(@NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
                       @NotNull @DecimalMin("0.00") BigDecimal shippingFee,
                       @NotNull @DecimalMin("0.00") BigDecimal freeShippingThreshold) {
    }

    /**
     * S3-compatible object storage for product images (AWS S3, or MinIO locally).
     *
     * @param endpoint      leave empty for AWS S3; set to the MinIO URL locally
     * @param publicBaseUrl base URL clients use to load stored files
     * @param pathStyle     true for local S3-compatible servers (bucket in the path instead of the host name)
     * @param createBucket  create the bucket on startup when it is missing (local and Docker Compose only)
     */
    public record Storage(@NotBlank String bucket,
                          @NotBlank String region,
                          String endpoint,
                          String accessKey,
                          String secretKey,
                          @NotBlank String publicBaseUrl,
                          boolean pathStyle,
                          boolean createBucket) {
    }

    /**
     * @param resetUrl front-end page that receives {@code ?token=...}
     */
    public record PasswordReset(@Min(1) long tokenExpirationMinutes,
                                @NotBlank String resetUrl) {
    }

    public record Bootstrap(String adminUsername,
                            String adminEmail,
                            String adminPassword) {
    }
}
