package org.example.panhabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the School Attendance Management System.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PanhaBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanhaBackEndApplication.class, args);
    }
}
