package edu.asu.ser594.resumeassistant.domain.shared.entity;

import lombok.Data;

import java.util.Objects;

@Data
public abstract class AggregateRoot<ID> implements Entity<ID> {
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
