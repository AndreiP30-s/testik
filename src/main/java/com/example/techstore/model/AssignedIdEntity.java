package com.example.techstore.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/** Ensures that a new object with a natural key is inserted, never merged into an existing row. */
@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<String> {
    @Transient private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markStored() {
        newEntity = false;
    }
}
