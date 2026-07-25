package com.picoding.fish.database.models

import com.picoding.fish.core.schemas.user.UserRole
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    val id: UUID? = null,
    val email: String,
    val password: String,
    val fullName: String,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    var createdBy: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    @PrePersist
    fun defaultCreatedBy() {
        if (createdBy == null) {
            createdBy = id
        }
    }
}
