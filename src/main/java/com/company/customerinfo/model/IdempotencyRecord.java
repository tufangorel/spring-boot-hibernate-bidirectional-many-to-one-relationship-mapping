package com.company.customerinfo.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "idempotency_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idempotency_key", "entity_type"})
})
public class IdempotencyRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "resource_id", nullable = false)
    private Integer resourceId;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String entityType, Integer resourceId) {
        this.idempotencyKey = idempotencyKey;
        this.entityType = entityType;
        this.resourceId = resourceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }
}
