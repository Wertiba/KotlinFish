package com.picoding.fish.core.schemas.token

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
