package com.picoding.fish.database.models

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Suppress("unused")
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    var userId: UUID,
    var hashedToken: String,
    var expiresAt: Instant,
) : BaseEntity()
