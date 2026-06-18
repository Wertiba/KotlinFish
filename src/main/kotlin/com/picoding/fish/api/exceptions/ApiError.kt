package com.picoding.fish.api.exceptions

data class ApiError(
    val code: String,
    val message: String,
    val traceId: String,
    val timestamp: String,
    val path: String,
    val details: Map<String, Any?>? = null,
)
