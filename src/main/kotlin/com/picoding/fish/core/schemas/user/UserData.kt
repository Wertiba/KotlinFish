package com.picoding.fish.core.schemas.user

import com.picoding.fish.core.validation.ValidEmail
import com.picoding.fish.core.validation.ValidFullName
import com.picoding.fish.core.validation.ValidPassword
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class UserLoginBody(
    @get:ValidEmail
    val email: String,
    @get:NotBlank(message = "Password must not be blank.")
    val password: String,
)

data class UserRegisterBody(
    @get:ValidEmail
    val email: String,
    @get:ValidPassword
    val password: String,
    @get:ValidFullName
    val fullName: String,
)

data class AdminRegisterUserBody(
    @get:ValidEmail
    val email: String,
    @get:ValidPassword
    val password: String,
    @get:ValidFullName
    val fullName: String,
    val role: UserRole,
)

data class UserPutBody(
    @get:ValidFullName
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean,
)

data class UserReadResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: UUID,
)

data class UserAndAccessTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val user: UserReadResponse,
)

enum class UserRole {
    USER,
    ADMIN,
}
