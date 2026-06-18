package com.picoding.fish.api.exceptions

import org.springframework.http.HttpStatus

class AppException(
    val code: String,
    override val message: String,
    val status: HttpStatus,
    val details: Map<String, Any?>? = null,
) : RuntimeException(message)
