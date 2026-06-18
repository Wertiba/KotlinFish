package com.picoding.fish.database.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val hashedToken: String,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
)
