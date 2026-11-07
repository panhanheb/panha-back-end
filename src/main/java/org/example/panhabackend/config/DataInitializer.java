package org.example.panhabackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.panhabackend.entity.Role;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.RoleName;
import org.example.panhabackend.repository.RoleRepository;
import org.example.panhabackend.repository.UserRepository;
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
 * Seeds the three roles and a bootstrap administrator on first start. Idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = new EnumMap<>(Map.of(
            RoleName.ROLE_ADMIN, "School administrator - full access",
            RoleName.ROLE_TEACHER, "Teacher - records attendance for assigned classes",
            RoleName.ROLE_STUDENT, "Student - views personal attendance"));

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
