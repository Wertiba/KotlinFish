package com.picoding.fish.services

import com.picoding.fish.core.schemas.token.TokenPair
import com.picoding.fish.core.schemas.user.UserLoginBody
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.core.security.HashEncoder
import com.picoding.fish.database.models.RefreshToken
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class AuthService(
    private val jwtService: JWTService,
    private val userRepo: UserRepository,
    private val refreshTokenRepo: RefreshTokenRepository,
    private val hashEncoder: HashEncoder,
) {
    fun register(data: UserRegisterBody): User {
        val (email, password, fullName, role) = data
        val user = userRepo.findByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User already exists")
        }
        return userRepo.save(
            User(
                email = email,
                password = hashEncoder.encode(password),
                fullName = fullName,
                role = role,
            ),
        )
    }

    fun login(data: UserLoginBody): TokenPair {
        val (email, password) = data
        val user =
            userRepo.findByEmail(email)
                ?: throw BadCredentialsException("User not found.")

        if (!hashEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Invalid password.")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user =
            userRepo.findById(UUID.fromString(userId)).orElseThrow {
                ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
            }

        val hashed = hashToken(refreshToken)
        refreshTokenRepo.findByidAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Refresh token not recognized.")

        refreshTokenRepo.deleteByidAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    private fun storeRefreshToken(
        userId: UUID,
        rawRefreshToken: String,
    ) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidMs
        val expiryAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepo.save(
            RefreshToken(
                userId = userId,
                hashedToken = hashed,
                expiresAt = expiryAt,
            ),
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
