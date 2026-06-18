package com.picoding.fish.database.repositories

import com.picoding.fish.database.models.RefreshToken
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface RefreshTokenRepository : CrudRepository<RefreshToken, UUID> {
    fun findByidAndHashedToken(
        id: UUID,
        hashedToken: String,
    ): RefreshToken?

    fun deleteByidAndHashedToken(
        userId: UUID,
        hashedToken: String,
    )
}
