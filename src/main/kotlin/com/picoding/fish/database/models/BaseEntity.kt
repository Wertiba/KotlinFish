package com.picoding.fish.database.models

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: UUID? = null
        protected set

    @CreatedDate
    @Column(updatable = false)
    var createdAt: Instant? = null
        protected set
}

@MappedSuperclass
abstract class AuditableEntity : BaseEntity() {
    @LastModifiedDate
    var updatedAt: Instant? = null
        protected set
}
