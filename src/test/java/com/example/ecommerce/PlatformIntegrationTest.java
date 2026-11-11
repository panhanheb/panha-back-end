package com.example.ecommerce;

import com.example.ecommerce.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2 acceptance: the application starts on real PostgreSQL and Redis, the Flyway schema matches
 * the entities, and the cross-cutting pieces (health, Swagger, security errors, CORS, cache, JSON
 * conventions) behave as documented.
 */
@IntegrationTest
class PlatformIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void flywayCreatedTheSchemaAndSeedDataExists() {
        // the context only starts when Hibernate's ddl-auto=validate agrees with the migration
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versions).contains("1");
        assertThat(jdbc.queryForList("select name from roles", String.class))
                .contains("ROLE_ADMIN", "ROLE_CUSTOMER");
        assertThat(jdbc.queryForObject("select count(*) from users where username = 'admin'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void healthReportsUpIncludingDatabaseAndRedis() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("E-Commerce API")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")));
    }

    @Test
    void protectedEndpointsAnswerWithTheJsonErrorEnvelope() throws Exception {
        mvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.path", is("/api/v1/orders")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void corsPreflightIsAllowedForConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/v1/products")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mvc.perform(options("/api/v1/products")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void redisBackedCacheStoresAndReturnsValues() {
        Cache cache = cacheManager.getCache("phase2-smoke");
        assertThat(cache).isNotNull();
        cache.put("greeting", "hello");
        assertThat(cache.get("greeting", String.class)).isEqualTo("hello");
        cache.evict("greeting");
    }

    @Test
    void moneyIsSerialisedAsAString() {
        assertThat(jsonMapper.writeValueAsString(new BigDecimal("19.90"))).isEqualTo("\"19.90\"");
    }
}
