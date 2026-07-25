package com.picoding.fish

import com.picoding.fish.api.exceptions.ApiError
import com.picoding.fish.api.exceptions.FieldError
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalValidationHandler(
    private val request: HttpServletRequest,
) {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors =
            e.bindingResult.fieldErrors.map {
                FieldError(
                    field = it.field,
                    issue = it.defaultMessage ?: "Invalid value.",
                    rejectedValue = it.rejectedValue,
                )
            }
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(buildApiError(fieldErrors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(buildApiError(fieldErrors = null, message = "Request body is missing required fields or malformed."))

    private fun buildApiError(
        fieldErrors: List<FieldError>?,
        message: String = "Some fields failed validation.",
    ) = ApiError(
        code = "VALIDATION_FAILED",
        message = message,
        traceId = UUID.randomUUID().toString(),
        timestamp = Instant.now().toString(),
        path = request.requestURI,
        fieldErrors = fieldErrors,
    )
}
