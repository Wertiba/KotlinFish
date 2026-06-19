package com.picoding.fish.core.schemas.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import java.util.UUID

interface UserDataBody {
    @get:Email(message = "Invalid email format.")
    val email: String

    @get:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}$",
        message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character.",
    )
    val password: String
}

interface UserDataResponse {
    val id: UUID
    val email: String
    val fullName: String
    val role: UserRole
}

data class UserLoginBody(
    override val email: String,
    override val password: String,
) : UserDataBody

data class UserRegisterBody(
    override val email: String,
    override val password: String,
    @get:Pattern(
        regexp = "^[a-zA-Z_]{2,}$",
        message = "FullName must be at least 2 characters long and contain only uppercase, lowercase and _ symbols.",
    )
    val fullName: String,
    val role: UserRole,
) : UserDataBody

data class UserReadResponse(
    override val id: UUID,
    override val email: String,
    override val fullName: String,
    override val role: UserRole,
) : UserDataResponse
