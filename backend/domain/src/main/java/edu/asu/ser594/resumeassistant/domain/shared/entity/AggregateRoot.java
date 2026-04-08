package edu.asu.ser594.resumeassistant.domain.shared.entity;

import java.util.Objects;

/**
 * 聚合根基类
 * Aggregate root base class
 *
 * 注意：不使用 @Data/@EqualsAndHashCode 注解，而是手动实现 equals 和 hashCode
 * 基于实体ID进行相等性判断，这是DDD实体的标准做法
 */
public abstract class AggregateRoot<ID> implements Entity<ID> {

    @Override
    public abstract ID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
