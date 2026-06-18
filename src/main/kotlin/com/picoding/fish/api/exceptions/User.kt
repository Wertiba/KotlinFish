package com.picoding.fish.api.exceptions

import org.springframework.http.HttpStatus

fun userAlreadyExists(email: String) =
    AppException(
        code = "EMAIL_ALREADY_EXISTS",
        message = "A user with this email already exists.",
        status = HttpStatus.CONFLICT,
        details =
            mapOf(
                "field" to "email",
                "value" to email,
            ),
    )
