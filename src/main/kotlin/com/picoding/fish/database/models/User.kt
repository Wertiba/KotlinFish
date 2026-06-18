package com.picoding.fish.database.models

import com.picoding.fish.core.schemas.user.UserRole
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    val id: UUID = UUID.randomUUID(),
    val email: String,
    val password: String,
    val fullName: String,
    val role: UserRole,
)
