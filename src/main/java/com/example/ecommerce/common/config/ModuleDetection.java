package com.example.ecommerce.common.config;

import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.JavaPackage;

import java.util.stream.Stream;

/**
 * Tells Spring Modulith where the application modules are: every package directly under
 * {@code com.example.ecommerce.modules} (auth, user, product, order...) plus the shared {@code common} package.
 * Selected with {@code spring.modulith.detection-strategy} in application.yml.
 */
public class ModuleDetection implements ApplicationModuleDetectionStrategy {

    @Override
    public Stream<JavaPackage> getModuleBasePackages(JavaPackage basePackage) {
        return basePackage.getDirectSubPackages().stream()
                .flatMap(p -> p.getName().endsWith(".modules") ? p.getDirectSubPackages().stream() : Stream.of(p));
    }
}
