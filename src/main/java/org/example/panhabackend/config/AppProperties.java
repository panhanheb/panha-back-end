package org.example.panhabackend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Strongly typed binding of all {@code app.*} properties.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt,
                            Cors cors,
                            Attendance attendance,
                            PasswordReset passwordReset,
                            Bootstrap bootstrap) {

    public record Jwt(@NotBlank String secret,
                      @Min(1) long accessTokenExpirationMinutes,
                      @Min(1) long refreshTokenExpirationDays,
                      @NotBlank String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Attendance(boolean countLateAsPresent,
                             @Min(0) int teacherEditWindowDays) {
    }

    public record PasswordReset(@Min(1) long tokenExpirationMinutes,
                                @NotBlank String resetUrl) {
    }

    public record Bootstrap(String adminUsername,
                            String adminEmail,
                            String adminPassword) {
    }
}
