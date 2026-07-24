package com.picoding.fish.api.exceptions

import org.springframework.http.HttpStatus

fun userAlreadyExists(
    email: String,
    message: String = "A user with this email already exists.",
) = AppException(
    code = "EMAIL_ALREADY_EXISTS",
    message = message,
    status = HttpStatus.CONFLICT,
    details =
        mapOf(
            "field" to "email",
            "value" to email,
        ),
)

fun userNotFound(message: String = "User not found.") =
    AppException(
        code = "NOT_FOUND",
        message = message,
        status = HttpStatus.NOT_FOUND,
    )

fun userInactive(message: String = "User account is inactive.") =
    AppException(
        code = "USER_LOCKED",
        message = message,
        status = HttpStatus.LOCKED,
    )
