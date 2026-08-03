package com.picoding.fish.services

import com.picoding.fish.api.exceptions.AppException
import com.picoding.fish.core.Settings
import com.picoding.fish.core.dto.user.UserLoginBody
import com.picoding.fish.core.dto.user.UserRegisterBody
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
import com.picoding.fish.support.persisted
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private val jwtService = mockk<JWTService>()
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepo = mockk<RefreshTokenRepository>(relaxed = true)
    private val hashEncoder = mockk<HashEncoder>()
    private val settings =
        Settings(
            security =
                Settings.Security(
                    jwtSecret = "secret",
                    accessTokenExpiration = Duration.ofMinutes(15),
                    refreshTokenExpiration = Duration.ofDays(30),
                ),
            cors = Settings.Cors(emptyList()),
            admin = Settings.Admin("admin@test.local", "pw", "Admin"),
            cookie = Settings.Cookie(secure = true),
        )

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(jwtService, userRepository, refreshTokenRepo, hashEncoder, settings)
        every { refreshTokenRepo.save(any()) } answers { firstArg() }
    }

    private fun hashOf(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    @Test
    fun `register creates a new user and returns a token pair`() {
        every { hashEncoder.encode("Password123") } returns "hashed-password"
        val savedSlot = slot<User>()
        every { userRepository.saveAndFlush(capture(savedSlot)) } answers { savedSlot.captured.persisted() }
        every { jwtService.generateAccessToken(any()) } returns "access-token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh-token"

        val (refreshToken, data) =
            authService.register(UserRegisterBody("john@example.com", "Password123", "John Doe"))

        assertEquals("refresh-token", refreshToken)
        assertEquals("access-token", data.accessToken)
        assertEquals(settings.security.accessTokenExpiration.seconds, data.expiresIn)
        assertEquals("john@example.com", data.user.email)
        assertEquals("hashed-password", savedSlot.captured.password)
        verify(exactly = 1) { refreshTokenRepo.save(any()) }
    }

    @Test
    fun `register fails when the email is already taken`() {
        every { hashEncoder.encode("Password123") } returns "hashed-password"
        every { userRepository.saveAndFlush(any()) } throws DataIntegrityViolationException("duplicate key")

        val ex =
            assertThrows(AppException::class.java) {
                authService.register(UserRegisterBody("john@example.com", "Password123", "John Doe"))
            }

        assertEquals("EMAIL_ALREADY_EXISTS", ex.code)
        assertEquals(HttpStatus.CONFLICT, ex.status)
    }

    @Test
    fun `login succeeds with correct credentials`() {
        val user = User("john@example.com", "hashed-password", "John Doe").persisted()
        every { userRepository.findByEmail("john@example.com") } returns user
        every { hashEncoder.matches("Password123", "hashed-password") } returns true
        every { jwtService.generateAccessToken(any()) } returns "access-token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh-token"

        val (tokenPair, data) = authService.login(UserLoginBody("john@example.com", "Password123"))

        assertEquals("access-token", tokenPair.accessToken)
        assertEquals("refresh-token", tokenPair.refreshToken)
        assertEquals("john@example.com", data.user.email)
    }

    @Test
    fun `login fails for unknown email`() {
        every { userRepository.findByEmail("unknown@example.com") } returns null

        val ex =
            assertThrows(AppException::class.java) {
                authService.login(UserLoginBody("unknown@example.com", "Password123"))
            }

        assertEquals("UNAUTHORIZED", ex.code)
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }

    @Test
    fun `login fails for wrong password`() {
        val user = User("john@example.com", "hashed-password", "John Doe").persisted()
        every { userRepository.findByEmail("john@example.com") } returns user
        every { hashEncoder.matches("WrongPassword", "hashed-password") } returns false

        val ex =
            assertThrows(AppException::class.java) {
                authService.login(UserLoginBody("john@example.com", "WrongPassword"))
            }

        assertEquals("UNAUTHORIZED", ex.code)
    }

    @Test
    fun `login fails for an inactive user`() {
        val user = User("john@example.com", "hashed-password", "John Doe", isActive = false).persisted()
        every { userRepository.findByEmail("john@example.com") } returns user
        every { hashEncoder.matches("Password123", "hashed-password") } returns true

        val ex =
            assertThrows(AppException::class.java) {
                authService.login(UserLoginBody("john@example.com", "Password123"))
            }

        assertEquals("USER_INACTIVE", ex.code)
        assertEquals(HttpStatus.LOCKED, ex.status)
    }

    @Test
    fun `logout deletes the refresh token by its hash`() {
        authService.logout("raw-refresh-token")

        verify(exactly = 1) { refreshTokenRepo.deleteByHashedToken(hashOf("raw-refresh-token")) }
    }

    @Test
    fun `refresh rotates the refresh token and returns a new access token`() {
        val user = User("john@example.com", "hashed-password", "John Doe").persisted()
        val userId = user.id!!
        val hashed = hashOf("old-refresh-token")

        every { jwtService.validateRefreshToken("old-refresh-token") } returns true
        every { jwtService.getUserIdFromToken("old-refresh-token") } returns userId.toString()
        every { userRepository.findById(userId) } returns Optional.of(user)
        every {
            refreshTokenRepo.findByuserIdAndHashedToken(userId, hashed)
        } returns
            com.picoding.fish.database.models
                .RefreshToken(userId, hashed, java.time.Instant.now())
        every { jwtService.generateAccessToken(userId.toString()) } returns "new-access-token"
        every { jwtService.generateRefreshToken(userId.toString()) } returns "new-refresh-token"

        val (tokenPair, data) = authService.refresh("old-refresh-token")

        assertEquals("new-access-token", tokenPair.accessToken)
        assertEquals("new-access-token", data.accessToken)
        verify(exactly = 1) { refreshTokenRepo.deleteByuserIdAndHashedToken(userId, hashed) }
    }

    @Test
    fun `refresh fails for an invalid token`() {
        every { jwtService.validateRefreshToken("bad-token") } returns false

        val ex = assertThrows(AppException::class.java) { authService.refresh("bad-token") }

        assertEquals("UNAUTHORIZED", ex.code)
    }

    @Test
    fun `refresh fails when the user no longer exists`() {
        val userId = UUID.randomUUID()
        every { jwtService.validateRefreshToken("token") } returns true
        every { jwtService.getUserIdFromToken("token") } returns userId.toString()
        every { userRepository.findById(userId) } returns Optional.empty()

        val ex = assertThrows(AppException::class.java) { authService.refresh("token") }

        assertEquals("UNAUTHORIZED", ex.code)
    }

    @Test
    fun `refresh fails when the token is not recognized`() {
        val user = User("john@example.com", "hashed-password", "John Doe").persisted()
        val userId = user.id!!
        every { jwtService.validateRefreshToken("token") } returns true
        every { jwtService.getUserIdFromToken("token") } returns userId.toString()
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { refreshTokenRepo.findByuserIdAndHashedToken(userId, any()) } returns null

        val ex = assertThrows(AppException::class.java) { authService.refresh("token") }

        assertEquals("UNAUTHORIZED", ex.code)
        assertEquals("Refresh token not recognized.", ex.message)
    }
}
