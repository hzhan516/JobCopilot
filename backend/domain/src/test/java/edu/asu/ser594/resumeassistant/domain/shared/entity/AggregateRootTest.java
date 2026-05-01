package edu.asu.ser594.resumeassistant.domain.shared.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AggregateRoot 基类单元测试
 * AggregateRoot Base Class Unit Tests
 * 
 * 测试所有聚合根的基类行为：
 * Tests the base class behavior for all aggregate roots:
 * - 基于ID的实体标识
 * - Entity identity based on ID
 * - Equals 和 hashCode 契约
 * - Equals and hashCode contract
 */
@DisplayName("AggregateRoot Base Class Tests")
class AggregateRootTest {

    @Test
    @DisplayName("Should implement Entity interface")
    void shouldImplementEntityInterface() {
        // 给定
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot(UUID.randomUUID());

        // 那么
        // Then
        assertThat(aggregate).isInstanceOf(Entity.class);
    }

    @Test
    @DisplayName("Should consider equal when same ID")
    void shouldConsiderEqualWhenSameId() {
        // 给定
        // Given
        UUID sharedId = UUID.randomUUID();
        TestAggregateRoot aggregate1 = new TestAggregateRoot(sharedId);
        TestAggregateRoot aggregate2 = new TestAggregateRoot(sharedId);

        // 那么
        // Then
        assertThat(aggregate1).isEqualTo(aggregate2);
        assertThat(aggregate1.hashCode()).isEqualTo(aggregate2.hashCode());
    }

    @Test
    @DisplayName("Should consider not equal when different ID")
    void shouldConsiderNotEqualWhenDifferentId() {
        // 给定
        // Given
        TestAggregateRoot aggregate1 = new TestAggregateRoot(UUID.randomUUID());
        TestAggregateRoot aggregate2 = new TestAggregateRoot(UUID.randomUUID());

        // 那么
        // Then
        assertThat(aggregate1).isNotEqualTo(aggregate2);
    }

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
        // 给定
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot(UUID.randomUUID());

        // 那么
        // Then
        assertThat(aggregate).isEqualTo(aggregate);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // 给定
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot(UUID.randomUUID());

        // 那么
        // Then
        assertThat(aggregate).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
        // 给定
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot(UUID.randomUUID());

        // 那么
        // Then
        assertThat(aggregate).isNotEqualTo("not an aggregate");
    }

    @Test
    @DisplayName("Should handle null ID in equals")
    void shouldHandleNullIdInEquals() {
        // 给定
        // Given
        TestAggregateRoot aggregate1 = new TestAggregateRoot(null);
        TestAggregateRoot aggregate2 = new TestAggregateRoot(null);
        TestAggregateRoot aggregate3 = new TestAggregateRoot(UUID.randomUUID());

        // 那么 - 两个null ID被视为相等（相同瞬态）
        // Then - Two null IDs are considered equal (same transient state)
        assertThat(aggregate1).isEqualTo(aggregate2);
        assertThat(aggregate1).isNotEqualTo(aggregate3);
    }

    @Test
    @DisplayName("Should handle null ID in hashCode")
    void shouldHandleNullIdInHashCode() {
        // 给定
        // Given
        TestAggregateRoot aggregate = new TestAggregateRoot(null);

        // 那么 - 不应抛出异常
        // Then - should not throw exception
        int hashCode = aggregate.hashCode();
        assertThat(hashCode).isEqualTo(0);
    }

    /**
     * AggregateRoot 的测试实现，用于测试基类行为
     * Test implementation of AggregateRoot for testing base class behavior
     */
    private static class TestAggregateRoot extends AggregateRoot<UUID> {
        private final UUID id;

        TestAggregateRoot(UUID id) {
            this.id = id;
        }

        @Override
        public UUID getId() {
            return id;
        }
    }
}
