package com.picoding.fish.services

import com.picoding.fish.api.exceptions.AppException
import com.picoding.fish.api.utils.security.UserPrincipal
import com.picoding.fish.core.schemas.user.AdminRegisterUserBody
import com.picoding.fish.core.schemas.user.UserFilterQuery
import com.picoding.fish.core.schemas.user.UserPutBody
import com.picoding.fish.core.schemas.user.UserRole
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
import java.util.UUID

class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private val hashEncoder = mockk<HashEncoder>()

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, refreshTokenRepository, hashEncoder)
    }

    @Test
    fun `createUser saves a new user attributed to the calling admin`() {
        val adminId = UUID.randomUUID()
        every { userRepository.findByEmail("new@example.com") } returns null
        every { hashEncoder.encode("Password123") } returns "hashed-password"
        val savedSlot = slot<User>()
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured.persisted() }

        val result =
            userService.createUser(
                AdminRegisterUserBody("new@example.com", "Password123", "New User", UserRole.USER),
                adminId,
            )

        assertEquals("new@example.com", result.email)
        assertEquals(adminId, result.createdBy)
        assertEquals("hashed-password", savedSlot.captured.password)
    }

    @Test
    fun `createUser fails when the email is already taken`() {
        every { userRepository.findByEmail("new@example.com") } returns User("new@example.com", "hash", "Someone").persisted()

        val ex =
            assertThrows(AppException::class.java) {
                userService.createUser(
                    AdminRegisterUserBody("new@example.com", "Password123", "New User", UserRole.USER),
                    UUID.randomUUID(),
                )
            }

        assertEquals("EMAIL_ALREADY_EXISTS", ex.code)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `getAllUsers maps the repository page into a PageResponse`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        val pageable = PageRequest.of(0, 20)
        every { userRepository.findAll(any<Specification<User>>(), any<PageRequest>()) } returns PageImpl(listOf(user), pageable, 1)

        val result = userService.getAllUsers(UserFilterQuery(), page = 0, size = 20)

        assertEquals(1, result.items.size)
        assertEquals(1L, result.total)
        assertEquals("john@example.com", result.items.first().email)
    }

    @Test
    fun `getUserById returns the mapped user`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)

        val result = userService.getUserById(user.id!!)

        assertEquals(user.id, result.id)
    }

    @Test
    fun `getUserById fails when the user does not exist`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns Optional.empty()

        val ex = assertThrows(AppException::class.java) { userService.getUserById(id) }

        assertEquals("NOT_FOUND", ex.code)
    }

    @Test
    fun `updateUserById as admin can change role and active state`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }
        val admin = UserPrincipal(id = UUID.randomUUID(), role = UserRole.ADMIN)

        val result =
            userService.updateUserById(user.id!!, UserPutBody("John Updated", UserRole.ADMIN, false), admin)

        assertEquals("John Updated", result.fullName)
        assertEquals(UserRole.ADMIN, result.role)
        assertEquals(false, result.isActive)
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(user.id!!) }
    }

    @Test
    fun `updateUserById does not revoke sessions when the user stays active`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }
        val admin = UserPrincipal(id = UUID.randomUUID(), role = UserRole.ADMIN)

        userService.updateUserById(user.id!!, UserPutBody("John Updated", UserRole.USER, true), admin)

        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
    }

    @Test
    fun `updateUserById rejects a non-admin trying to change their own role`() {
        val user = User("john@example.com", "hash", "John Doe", role = UserRole.USER).persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        val caller = UserPrincipal(id = user.id!!, role = UserRole.USER)

        assertThrows(AccessDeniedException::class.java) {
            userService.updateUserById(user.id!!, UserPutBody("John Doe", UserRole.ADMIN, true), caller)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `updateUserById rejects a non-admin trying to change their own active state`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        val caller = UserPrincipal(id = user.id!!, role = UserRole.USER)

        assertThrows(AccessDeniedException::class.java) {
            userService.updateUserById(user.id!!, UserPutBody("John Doe", UserRole.USER, false), caller)
        }
    }

    @Test
    fun `deleteUserById deactivates the user and revokes their sessions`() {
        val user = User("john@example.com", "hash", "John Doe").persisted()
        every { userRepository.findById(user.id!!) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        userService.deleteUserById(user.id!!)

        assertEquals(false, user.isActive)
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(user.id!!) }
    }

    @Test
    fun `deleteUserById fails when the user does not exist`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns Optional.empty()

        assertThrows(AppException::class.java) { userService.deleteUserById(id) }
    }
}
