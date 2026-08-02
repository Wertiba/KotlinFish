package com.picoding.fish.services

import com.picoding.fish.api.exceptions.AppException
import com.picoding.fish.core.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class JWTServiceTest {
    private fun settingsWith(
        accessTokenExpiration: Duration = Duration.ofMinutes(15),
        refreshTokenExpiration: Duration = Duration.ofDays(30),
    ) = Settings(
        security =
            Settings.Security(
                jwtSecret = "dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkxMjM0NQ==",
                accessTokenExpiration = accessTokenExpiration,
                refreshTokenExpiration = refreshTokenExpiration,
            ),
        cors = Settings.Cors(emptyList()),
        admin = Settings.Admin("admin@test.local", "pw", "Admin"),
    )

    private lateinit var jwtService: JWTService

    @BeforeEach
    fun setUp() {
        jwtService = JWTService(settingsWith())
    }

    @Test
    fun `access token is valid as an access token but not as a refresh token`() {
        val userId = UUID.randomUUID().toString()
        val token = jwtService.generateAccessToken(userId)

        assertTrue(jwtService.validateAccessToken(token))
        assertFalse(jwtService.validateRefreshToken(token))
        assertEquals(userId, jwtService.getUserIdFromToken(token))
    }

    @Test
    fun `refresh token is valid as a refresh token but not as an access token`() {
        val userId = UUID.randomUUID().toString()
        val token = jwtService.generateRefreshToken(userId)

        assertTrue(jwtService.validateRefreshToken(token))
        assertFalse(jwtService.validateAccessToken(token))
    }

    @Test
    fun `validation strips a Bearer prefix before parsing`() {
        val token = jwtService.generateAccessToken(UUID.randomUUID().toString())

        assertTrue(jwtService.validateAccessToken("Bearer $token"))
    }

    @Test
    fun `malformed tokens are never valid`() {
        assertFalse(jwtService.validateAccessToken("not-a-jwt"))
        assertFalse(jwtService.validateRefreshToken("not-a-jwt"))
    }

    @Test
    fun `reading the user id from a malformed token throws`() {
        val ex = assertThrows(AppException::class.java) { jwtService.getUserIdFromToken("not-a-jwt") }
        assertEquals("UNAUTHORIZED", ex.code)
    }

    @Test
    fun `expired tokens are rejected`() {
        val shortLivedService = JWTService(settingsWith(accessTokenExpiration = Duration.ofMillis(1)))
        val token = shortLivedService.generateAccessToken(UUID.randomUUID().toString())

        Thread.sleep(20)

        assertFalse(shortLivedService.validateAccessToken(token))
    }

    @Test
    fun `a token signed with a different secret is rejected`() {
        val otherService =
            JWTService(
                Settings(
                    security =
                        Settings.Security(
                            jwtSecret = "b3RoZXJzZWNyZXRrZXlmb3J0ZXN0aW5ncHVycG9zZXNvbmx5Njc4OQ==",
                            accessTokenExpiration = Duration.ofMinutes(15),
                            refreshTokenExpiration = Duration.ofDays(30),
                        ),
                    cors = Settings.Cors(emptyList()),
                    admin = Settings.Admin("admin@test.local", "pw", "Admin"),
                ),
            )
        val token = otherService.generateAccessToken(UUID.randomUUID().toString())

        assertFalse(jwtService.validateAccessToken(token))
    }
}
