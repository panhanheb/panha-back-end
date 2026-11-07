package org.example.panhabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Entry point of the School Attendance Management System.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PanhaBackEndApplication {

    public static void main(String[] args) {
        applySchoolTimeZone();
        SpringApplication.run(PanhaBackEndApplication.class, args);
    }

    /**
     * "Today" (dashboards, default report dates, teacher edit window) and stored timestamps follow the JVM
     * time zone. Set APP_TIMEZONE (e.g. Asia/Phnom_Penh) so a server running in UTC still uses the school's day.
     */
    static void applySchoolTimeZone() {
        String zone = System.getenv("APP_TIMEZONE");
        if (zone != null && !zone.isBlank()) {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone.trim())));
        }
    }
}
