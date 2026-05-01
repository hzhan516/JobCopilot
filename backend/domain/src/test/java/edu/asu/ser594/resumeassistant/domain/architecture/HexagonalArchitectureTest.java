package edu.asu.ser594.resumeassistant.domain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 六边形架构测试
 * Hexagonal Architecture Tests
 * 
 * 验证DDD六边形架构合规性的测试：
 * Tests that verify DDD Hexagonal Architecture compliance:
 * - 领域层独立性
 * - Domain layer independence
 * - 依赖方向
 * - Dependency direction
 * - 领域层无基础设施依赖
 * - No infrastructure dependencies in domain
 */
@DisplayName("Hexagonal Architecture Compliance Tests")
class HexagonalArchitectureTest {

    private static JavaClasses domainClasses;

    @BeforeAll
    static void setUp() {
        domainClasses = new ClassFileImporter()
                .importPackages("edu.asu.ser594.resumeassistant.domain");
    }

    @Test
    @DisplayName("Domain layer should not depend on infrastructure")
    void domainLayerShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Domain layer should be independent of infrastructure");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on trigger")
    void domainLayerShouldNotDependOnTrigger() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..trigger..")
                .because("Domain layer should be independent of trigger layer");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring framework")
    void domainLayerShouldNotDependOnSpringFramework() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .because("Domain layer should be framework-agnostic");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("Domain entities should be in correct package")
    void domainEntitiesShouldBeInCorrectPackage() {
        ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith("Entity")
                .or()
                .haveSimpleNameEndingWith("Aggregate")
                .should()
                .resideInAPackage("..entity..")
                .because("Domain entities should be in entity package");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("Domain repositories should be interfaces")
    void domainRepositoriesShouldBeInterfaces() {
        ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .and()
                .resideInAPackage("..repository..")
                .should()
                .beInterfaces()
                .because("Domain repositories should be interfaces defined in domain layer");

        rule.check(domainClasses);
    }

    @Test
    @DisplayName("Domain should be free of cycles")
    void domainShouldBeFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching("edu.asu.ser594.resumeassistant.domain.(*)..")
                .should()
                .beFreeOfCycles()
                .check(domainClasses);
    }
}
