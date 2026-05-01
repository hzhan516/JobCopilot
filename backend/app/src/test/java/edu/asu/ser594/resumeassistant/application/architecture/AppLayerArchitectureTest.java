package edu.asu.ser594.resumeassistant.application.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 应用层架构测试
 * Application layer architecture tests
 * <p>
 * 验证应用层不直接依赖基础设施层的具体实现，遵循六边形架构的依赖规则。
 * Verifies that the application layer does not directly depend on infrastructure
 * implementations, following hexagonal architecture dependency rules.
 */
@DisplayName("Application Layer Architecture Compliance Tests")
class AppLayerArchitectureTest {

    private static JavaClasses appClasses;

    @BeforeAll
    static void setUp() {
        appClasses = new ClassFileImporter()
                .importPackages("edu.asu.ser594.resumeassistant.application");
    }

    @Test
    @DisplayName("Application layer should not depend on infrastructure")
    void appLayerShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Application layer should depend on domain ports, not infrastructure implementations");

        rule.check(appClasses);
    }
}
