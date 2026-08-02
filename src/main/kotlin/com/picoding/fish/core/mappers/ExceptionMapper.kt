package com.picoding.fish.core.mappers

import com.picoding.fish.api.exceptions.ApiError
import com.picoding.fish.api.exceptions.AppException
import com.picoding.fish.core.utils.TraceId
import java.time.Instant

fun AppException.toApiError(path: String) =
    ApiError(
        code = code,
        message = message,
        traceId = TraceId.current(),
        timestamp = Instant.now().toString(),
        path = path,
        details = details,
    )
