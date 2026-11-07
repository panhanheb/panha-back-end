package org.example.panhabackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI definition. UI: /swagger-ui.html, JSON: /v3/api-docs.
 * Click "Authorize" and paste the access token from POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI schoolAttendanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("School Attendance Management System API")
                        .version("1.0.0")
                        .description("REST API for managing students, teachers, classes, subjects and daily attendance, "
                                + "with reports and role-based dashboards (ADMIN, TEACHER, STUDENT).")
                        .contact(new Contact().name("School IT"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .name(BEARER)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
