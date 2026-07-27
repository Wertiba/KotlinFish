package com.picoding.fish.api.exceptions

import com.picoding.fish.core.mappers.toApiError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val request: HttpServletRequest,
) {
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ApiError> = ResponseEntity(ex.toApiError(request.requestURI), ex.status)

    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingCookie(ex: MissingRequestCookieException): ResponseEntity<ApiError> =
        ResponseEntity(
            error("UNAUTHORIZED", "Required cookie '${ex.cookieName}' is missing."),
            HttpStatus.UNAUTHORIZED,
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(): ResponseEntity<ApiError> =
        ResponseEntity(
            error("FORBIDDEN", "Insufficient rights to perform the operation."),
            HttpStatus.FORBIDDEN,
        )

    private fun error(
        code: String,
        message: String,
    ) = ApiError(
        code = code,
        message = message,
        traceId = UUID.randomUUID().toString(),
        timestamp = Instant.now().toString(),
        path = request.requestURI,
    )
}
