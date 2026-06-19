package com.picoding.fish.api.exceptions

import com.picoding.fish.core.mappers.toApiError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val request: HttpServletRequest,
) {
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ApiError> = ResponseEntity(ex.toApiError(request.requestURI), ex.status)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ApiError> =
        ResponseEntity(
            ApiError(
                code = "INVALID_CREDENTIALS",
                message = ex.message ?: "Invalid credentials.",
                traceId = UUID.randomUUID().toString(),
                timestamp = Instant.now().toString(),
                path = request.requestURI,
            ),
            HttpStatus.UNAUTHORIZED,
        )

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity(
            ApiError(
                code = "UNAUTHORIZED",
                message = ex.reason ?: "Unauthorized.",
                traceId = UUID.randomUUID().toString(),
                timestamp = Instant.now().toString(),
                path = request.requestURI,
            ),
            ex.statusCode,
        )
}
