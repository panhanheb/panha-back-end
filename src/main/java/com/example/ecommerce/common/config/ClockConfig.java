package com.example.ecommerce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * One UTC clock for token and expiry calculations, replaceable in tests. Also enables scheduled jobs
 * (expired refresh token cleanup, later reservation expiry).
 */
@Configuration
@EnableScheduling
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
