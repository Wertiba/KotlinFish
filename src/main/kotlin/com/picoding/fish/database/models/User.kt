package com.picoding.fish.database.models

import com.picoding.fish.core.dto.user.UserRole
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    var email: String,
    var password: String,
    var fullName: String,
    @Enumerated(EnumType.STRING) var role: UserRole = UserRole.USER,
    var isActive: Boolean = true,
    var createdBy: UUID? = null,
) : AuditableEntity() {
    @PrePersist
    fun defaultCreatedBy() {
        if (createdBy == null) {
            createdBy = id
        }
    }
}
