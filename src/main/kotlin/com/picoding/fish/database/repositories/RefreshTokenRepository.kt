package com.picoding.fish.database.repositories

import com.picoding.fish.database.models.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByidAndHashedToken(
        id: UUID,
        hashedToken: String,
    ): RefreshToken?

    fun deleteByidAndHashedToken(
        userId: UUID,
        hashedToken: String,
    )

    @Modifying
    @Transactional
    fun deleteByHashedToken(hashedToken: String)
}
