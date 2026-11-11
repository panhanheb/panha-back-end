package com.example.ecommerce.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    /**
     * Money is written as a JSON string ({@code "19.90"}), so JavaScript clients never turn it into a
     * binary floating-point number. Incoming numbers and strings are both accepted.
     */
    @Bean
    public JsonMapperBuilderCustomizer moneyAsString() {
        return builder -> builder.addModule(new SimpleModule("money-as-string")
                .addSerializer(BigDecimal.class, ToStringSerializer.instance));
    }
}
