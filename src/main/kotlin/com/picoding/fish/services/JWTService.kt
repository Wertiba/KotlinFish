package com.picoding.fish.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date

@Service
class JWTService(
    @Value("\${app.security.jwt-secret}") private val jwtSecret: String,
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    private val accessTokenValidMs = 15L * 60L * 1000L
    val refreshTokenValidMs = 30L * 24 * 60 * 60 * 1000L

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

    fun generateAccessToken(userId: String): String = generateToken(userId, "access", accessTokenValidMs)

    fun generateRefreshToken(userId: String): String = generateToken(userId, "refresh", refreshTokenValidMs)

    fun validateAccessToken(token: String): Boolean = validateToken(token, "access")

    fun validateRefreshToken(token: String): Boolean = validateToken(token, "refresh")

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid token.")
        return claims.subject
    }
}
