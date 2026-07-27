package com.picoding.fish.api.exceptions

import org.springframework.http.HttpStatus

fun invalidCredentials(message: String = "Invalid email or password.") =
    AppException(
        code = "UNAUTHORIZED",
        message = message,
        status = HttpStatus.UNAUTHORIZED,
    )
