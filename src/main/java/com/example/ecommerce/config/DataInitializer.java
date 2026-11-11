package com.example.ecommerce.config;

import com.example.ecommerce.model.enums.RoleName;
import com.example.ecommerce.model.Role;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.RoleRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Seeds every role and a bootstrap super administrator on start. Idempotent: existing data is only
 * completed (missing roles added), never overwritten.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = new EnumMap<>(Map.of(
            RoleName.ROLE_SUPER_ADMIN, "Super administrator - manages administrators and roles",
            RoleName.ROLE_ADMIN, "Store administrator - manages catalog, orders, customers and coupons",
            RoleName.ROLE_SELLER, "Seller - manages own products and fulfils own order lines",
            RoleName.ROLE_DELIVERY, "Delivery rider - handles assigned shipments",
            RoleName.ROLE_CUSTOMER, "Customer - shops, checks out and tracks own orders"));

    private static final Set<RoleName> BOOTSTRAP_ADMIN_ROLES = Set.of(RoleName.ROLE_SUPER_ADMIN, RoleName.ROLE_ADMIN);

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
        Set<Role> adminRoles = new HashSet<>();
        BOOTSTRAP_ADMIN_ROLES.forEach(name -> adminRoles.add(roleRepository.findByName(name).orElseThrow()));

        userRepository.findByUsername(bootstrap.adminUsername()).ifPresentOrElse(existing -> {
            // accounts created before SUPER_ADMIN existed get it added once
            if (!existing.getRoles().containsAll(adminRoles)) {
                existing.getRoles().addAll(adminRoles);
                log.info("Granted {} to bootstrap administrator '{}'", BOOTSTRAP_ADMIN_ROLES, existing.getUsername());
            }
        }, () -> {
            User admin = new User();
            admin.setUsername(bootstrap.adminUsername());
            admin.setEmail(bootstrap.adminEmail());
            admin.setPassword(passwordEncoder.encode(bootstrap.adminPassword()));
            admin.setEnabled(true);
            admin.setRoles(adminRoles);
            userRepository.save(admin);
            log.warn("Created bootstrap administrator '{}'. Change its password after first login!", admin.getUsername());
        });
    }
}
