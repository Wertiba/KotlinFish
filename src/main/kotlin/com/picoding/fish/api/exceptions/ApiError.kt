package com.picoding.fish.api.exceptions

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val traceId: String,
    val timestamp: String,
    val path: String,
    val details: Map<String, Any?>? = null,
    val fieldErrors: List<FieldError>? = null,
)

data class FieldError(
    val field: String,
    val issue: String,
    val rejectedValue: Any? = null,
)
