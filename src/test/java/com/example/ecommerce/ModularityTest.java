package com.example.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the module boundaries (see docs/DESIGN.md, section 3).
 * <p>
 * The services carried over from the first version still reach into other modules' entities and
 * repositories (checkout reads the cart tables directly, user deletion checks the orders table...). Those
 * known violations are tolerated up to {@link #KNOWN_LEGACY_VIOLATIONS}; the number may only go down. Each
 * module rewrite (Phases 5-11) removes its share, and the last one sets the baseline to 0, at which point
 * this test becomes a plain {@code verify()}.
 */
class ModularityTest {

    /** Ratchet: lower it when a rewrite removes violations; never raise it. */
    private static final int KNOWN_LEGACY_VIOLATIONS = 205;

    private final ApplicationModules modules = ApplicationModules.of(EcommerceApplication.class);

    @Test
    void detectsTheExpectedModules() {
        List<String> names = modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString).toList();
        assertThat(names).containsExactlyInAnyOrder("common", "modules.auth", "modules.user", "modules.customer",
                "modules.category", "modules.product", "modules.inventory", "modules.cart", "modules.order",
                "modules.payment", "modules.promotion", "modules.delivery", "modules.audit");
    }

    @Test
    void commonDependsOnNoModule() {
        List<String> commonViolations = modules.detectViolations().getMessages().stream()
                .filter(v -> v.startsWith("Module 'common'"))
                .toList();
        assertThat(commonViolations).isEmpty();
    }

    @Test
    void noNewBoundaryViolations() {
        List<String> violations = modules.detectViolations().getMessages();
        violations.forEach(v -> System.out.println("[legacy module violation] " + v.lines().findFirst().orElse(v)));
        assertThat(violations.size())
                .as("module boundary violations (only legacy ones are tolerated, and their number may only go down)")
                .isLessThanOrEqualTo(KNOWN_LEGACY_VIOLATIONS);
    }

    @Test
    void writesModuleDocumentation() {
        // PlantUML component diagrams and module canvases in target/spring-modulith-docs
        new Documenter(modules).writeDocumentation();
    }
}
