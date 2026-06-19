package com.picoding.fish.services

import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.core.mappers.toReadResponse
import com.picoding.fish.core.schemas.token.TokenPair
import com.picoding.fish.core.schemas.user.UserLoginBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.database.models.RefreshToken
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
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
    private val userRepository: UserRepository,
    private val refreshTokenRepo: RefreshTokenRepository,
    private val hashEncoder: HashEncoder,
) {
    fun register(data: UserRegisterBody): UserReadResponse {
        val (email, password, fullName, role) = data
        var user = userRepository.findByEmail(email.trim())
        if (user != null) {
            throw userAlreadyExists(email)
        }
        user =
            userRepository.save(
                User(
                    email = email,
                    password = hashEncoder.encode(password),
                    fullName = fullName,
                    role = role,
                ),
            )
        return user.toReadResponse()
    }

    fun login(data: UserLoginBody): TokenPair {
        val (email, password) = data
        val user =
            userRepository.findByEmail(email)
                ?: throw BadCredentialsException("User not found.")

        if (!hashEncoder.matches(password, user.password)) {
            throw BadCredentialsException("Invalid password.")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toString())

        storeRefreshToken(user.id!!, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        val hashed = hashToken(refreshToken)
        refreshTokenRepo.deleteByHashedToken(hashed)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user =
            userRepository.findById(UUID.fromString(userId)).orElseThrow {
                ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
            }

        val hashed = hashToken(refreshToken)
        refreshTokenRepo.findByidAndHashedToken(user.id!!, hashed)
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
