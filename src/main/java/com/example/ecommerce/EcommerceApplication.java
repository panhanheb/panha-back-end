package com.example.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Entry point of the E-Commerce API.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EcommerceApplication {

    public static void main(String[] args) {
        applyStoreTimeZone();
        SpringApplication.run(EcommerceApplication.class, args);
    }

    /**
     * Order timestamps and order numbers (which contain the date) follow the JVM time zone. Set APP_TIMEZONE
     * (e.g. Asia/Phnom_Penh) so a server running in UTC still uses the store's local day.
     */
    static void applyStoreTimeZone() {
        String zone = System.getenv("APP_TIMEZONE");
        if (zone != null && !zone.isBlank()) {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone.trim())));
        }
    }
}
