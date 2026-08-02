package com.picoding.fish.services

import com.picoding.fish.api.exceptions.invalidCredentials
import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.api.exceptions.userInactive
import com.picoding.fish.core.Settings
import com.picoding.fish.core.dto.token.AccessTokenResponse
import com.picoding.fish.core.dto.token.TokenPair
import com.picoding.fish.core.dto.user.UserAndAccessTokenResponse
import com.picoding.fish.core.dto.user.UserLoginBody
import com.picoding.fish.core.dto.user.UserRegisterBody
import com.picoding.fish.core.mappers.toTokenResponse
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.database.models.RefreshToken
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val settings: Settings,
) {
    @Transactional
    fun register(data: UserRegisterBody): Pair<String, UserAndAccessTokenResponse> {
        val (email, password, fullName) = data
        val user =
            User(
                email = email,
                password = hashEncoder.encode(password),
                fullName = fullName,
            )
        try {
            userRepository.save(user)
        } catch (_: DataIntegrityViolationException) {
            throw userAlreadyExists(email)
        }

        val tokenPair = generateTokenPair(user)
        return tokenPair.refreshToken to user.toTokenResponse(tokenPair.accessToken, settings.security.accessTokenExpiration.seconds)
    }

    fun login(data: UserLoginBody): Pair<TokenPair, UserAndAccessTokenResponse> {
        val (email, password) = data
        val user =
            userRepository.findByEmail(email)
                ?: throw invalidCredentials()

        if (!hashEncoder.matches(password, user.password)) {
            throw invalidCredentials()
        }
        isUserActive(user)

        val tokenPair = generateTokenPair(user)
        return tokenPair to user.toTokenResponse(tokenPair.accessToken, settings.security.accessTokenExpiration.seconds)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val hashed = hashToken(refreshToken)
        refreshTokenRepo.deleteByHashedToken(hashed)
    }

    @Transactional
    fun refresh(refreshToken: String): Pair<TokenPair, AccessTokenResponse> {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw invalidCredentials(message = "Invalid or expired refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user =
            userRepository.findById(UUID.fromString(userId)).orElseThrow {
                invalidCredentials("Invalid or expired refresh token.")
            }
        isUserActive(user)

        val hashed = hashToken(refreshToken)
        refreshTokenRepo.findByuserIdAndHashedToken(user.id!!, hashed)
            ?: throw invalidCredentials("Refresh token not recognized.")

        refreshTokenRepo.deleteByuserIdAndHashedToken(user.id!!, hashed)

        val tokenPair = generateTokenPair(user)
        return tokenPair to AccessTokenResponse(tokenPair.accessToken, settings.security.accessTokenExpiration.seconds)
    }

    private fun generateTokenPair(user: User): TokenPair {
        val userId = user.id!!
        val newAccessToken = jwtService.generateAccessToken(userId.toString())
        val newRefreshToken = jwtService.generateRefreshToken(userId.toString())

        storeRefreshToken(userId, newRefreshToken)

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
        val expiryMs = settings.security.refreshTokenValidMs
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

    private fun isUserActive(user: User) {
        if (!user.isActive) {
            throw userInactive()
        }
    }
}
