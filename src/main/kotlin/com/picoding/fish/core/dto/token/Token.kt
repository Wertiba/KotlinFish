package com.picoding.fish.core.dto.token

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

data class AccessTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
)
