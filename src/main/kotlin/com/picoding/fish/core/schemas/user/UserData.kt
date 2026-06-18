package com.picoding.fish.core.schemas.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

interface UserData {
    @get:Email(message = "Invalid email format.")
    val email: String

    @get:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{9,}$",
        message = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character.",
    )
    val password: String
}

data class UserLoginBody(
    override val email: String,
    override val password: String,
) : UserData

data class UserRegisterBody(
    override val email: String,
    override val password: String,
    @field:Pattern(
        regexp = "^[a-zA-Z_]{2,}$", // Рекомендуется добавить ^ в начало регулярки
        message = "FullName must be at least 2 characters long and contain only uppercase, lowercase and _ symbols.",
    )
    val fullName: String,
    val role: UserRole,
) : UserData
