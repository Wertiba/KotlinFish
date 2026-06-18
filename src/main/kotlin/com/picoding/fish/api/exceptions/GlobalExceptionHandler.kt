package com.picoding.fish.api.exceptions

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val request: HttpServletRequest,
) {
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ApiError> {
        val error =
            ApiError(
                code = ex.code,
                message = ex.message,
                traceId = UUID.randomUUID().toString(),
                timestamp = Instant.now().toString(),
                path = request.requestURI,
                details = ex.details,
            )
        return ResponseEntity(error, ex.status)
    }
}
