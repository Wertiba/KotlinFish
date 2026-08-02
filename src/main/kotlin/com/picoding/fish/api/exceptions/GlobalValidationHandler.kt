package com.picoding.fish.api.exceptions

import com.picoding.fish.core.utils.TraceId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

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

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ResponseEntity<ApiError> {
        val fieldErrors =
            e.constraintViolations.map {
                FieldError(
                    field = it.propertyPath.toString().substringAfterLast('.'),
                    issue = it.message ?: "Invalid value.",
                    rejectedValue = it.invalidValue,
                )
            }
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(buildApiError(fieldErrors))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiError> {
        val fieldErrors =
            listOf(
                FieldError(
                    field = e.name,
                    issue = "Invalid value for type ${e.requiredType?.simpleName ?: "unknown"}.",
                    rejectedValue = e.value,
                ),
            )
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(buildApiError(fieldErrors))
    }

    private fun buildApiError(
        fieldErrors: List<FieldError>?,
        message: String = "Some fields failed validation.",
    ) = ApiError(
        code = "VALIDATION_FAILED",
        message = message,
        traceId = TraceId.current(),
        timestamp = Instant.now().toString(),
        path = request.requestURI,
        fieldErrors = fieldErrors,
    )
}
