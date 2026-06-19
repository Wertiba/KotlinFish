package com.picoding.fish.core.mappers

import com.picoding.fish.api.exceptions.ApiError
import com.picoding.fish.api.exceptions.AppException
import java.time.Instant
import java.util.UUID

fun AppException.toApiError(path: String) =
    ApiError(
        code = code,
        message = message,
        traceId = UUID.randomUUID().toString(),
        timestamp = Instant.now().toString(),
        path = path,
        details = details,
    )
