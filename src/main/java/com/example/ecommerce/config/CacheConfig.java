package com.example.ecommerce.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Redis-backed Spring Cache with a 10 minute default lifetime and an {@code ecommerce:} key prefix.
 * Nothing is cached yet: the catalog services add their caches, their lifetimes and the JSON value format
 * in Phase 6, together with tests that read cached values back.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .prefixCacheNameWith("ecommerce:");
    }
}
