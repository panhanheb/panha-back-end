package com.example.ecommerce.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at {@code /swagger-ui.html}, OpenAPI JSON at {@code /v3/api-docs}. Every operation uses the
 * bearer token unless it is marked public.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI ecommerceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API")
                        .version("v1")
                        .description("""
                                REST API for the Flutter app, the web storefront and the admin dashboard.

                                * Every response is wrapped: `{ "success": true, "data": ... }` or \
                                `{ "success": false, "error": { "code": "...", "message": "..." } }`.
                                * Money is a string with two decimals (`"19.90"`); times are UTC ISO-8601.
                                * Lists accept `page`, `size` (max 100) and `sort=field,asc|desc`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
