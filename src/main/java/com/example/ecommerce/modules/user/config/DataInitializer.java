package com.example.ecommerce.modules.user.config;

import com.example.ecommerce.common.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.ecommerce.modules.user.entity.Role;
import com.example.ecommerce.modules.user.entity.User;
import com.example.ecommerce.modules.user.entity.RoleName;
import com.example.ecommerce.modules.user.repository.RoleRepository;
import com.example.ecommerce.modules.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the roles and a bootstrap administrator on first start. Idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = new EnumMap<>(Map.of(
            RoleName.ROLE_ADMIN, "Store administrator - manages catalog, orders and users",
            RoleName.ROLE_CUSTOMER, "Customer - shops, checks out and tracks own orders"));

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name, ROLE_DESCRIPTIONS.get(name)));
                log.info("Seeded role {}", name);
            }
        }
        seedAdmin();
    }

    private void seedAdmin() {
        AppProperties.Bootstrap bootstrap = appProperties.bootstrap();
        if (bootstrap == null || !StringUtils.hasText(bootstrap.adminUsername())
                || !StringUtils.hasText(bootstrap.adminPassword())) {
            log.warn("Bootstrap admin not configured (app.bootstrap.*); skipping");
            return;
        }
        if (userRepository.existsByUsername(bootstrap.adminUsername())) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        User admin = new User();
        admin.setUsername(bootstrap.adminUsername());
        admin.setEmail(bootstrap.adminEmail());
        admin.setPassword(passwordEncoder.encode(bootstrap.adminPassword()));
        admin.setEnabled(true);
        admin.setRoles(new java.util.HashSet<>(Set.of(adminRole)));
        userRepository.save(admin);
        log.warn("Created bootstrap administrator '{}'. Change its password after first login!", admin.getUsername());
    }
}
