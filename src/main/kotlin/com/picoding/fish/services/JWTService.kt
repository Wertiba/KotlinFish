package com.picoding.fish.services

import com.picoding.fish.api.exceptions.invalidCredentials
import com.picoding.fish.core.Settings
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JWTService(
    private val settings: Settings,
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(settings.security.jwtSecret))

    private fun parseAllClaims(token: String): Claims? {
        val rawToken =
            if (token.startsWith("Bearer ")) {
                token.removePrefix("Bearer ")
            } else {
                token
            }
        return try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (_: Exception) {
            null
        }
    }

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    private fun validateToken(
        token: String,
        type: String,
    ): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == type
    }

    fun generateAccessToken(userId: String): String = generateToken(userId, "access", settings.security.accessTokenValidMs)

    fun generateRefreshToken(userId: String): String = generateToken(userId, "refresh", settings.security.refreshTokenValidMs)

    fun validateAccessToken(token: String): Boolean = validateToken(token, "access")

    fun validateRefreshToken(token: String): Boolean = validateToken(token, "refresh")

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw invalidCredentials("Invalid or expired token.")
        return claims.subject
    }
}
