package com.joprelys.backend.auth.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionFirstControllerPolicyTest {

    private static final Set<String> INTENTIONAL_NON_RBAC_CONTROLLERS = Set.of(
            "AuthController.java",
            "PasswordRecoveryController.java",
            "PatientAuthController.java",
            "PharmacyController.java",
            "LabResultUploadController.java",
            "PublicDemoRequestController.java");

    @Test
    void controllersNeverAuthorizeWithRoleNames() throws IOException {
        for (Path controller : controllerSources()) {
            String source = Files.readString(controller);
            assertThat(source)
                    .as("politique permission-first de %s", controller)
                    .doesNotContain(
                            "@PreAuthorize(\"hasRole",
                            "@PreAuthorize(\"hasAnyRole",
                            "@PreAuthorize(\"!hasRole",
                            "@PreAuthorize(\"!hasAnyRole");
        }
    }

    @Test
    void everyNonPublicControllerDeclaresMethodSecurity() throws IOException {
        for (Path controller : controllerSources()) {
            if (INTENTIONAL_NON_RBAC_CONTROLLERS.contains(controller.getFileName().toString())) {
                continue;
            }
            assertThat(Files.readString(controller))
                    .as("protection @PreAuthorize de %s", controller)
                    .contains("@PreAuthorize");
        }
    }

    private List<Path> controllerSources() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        try (var paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .toList();
        }
    }
}
