package com.picoding.fish.core.mappers

import com.picoding.fish.core.schemas.user.UserAndAccessTokenResponse
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.database.models.User
import kotlin.String

fun User.toReadResponse() =
    UserReadResponse(
        id = id!!,
        email = email,
        fullName = fullName,
        role = role,
        isActive = isActive,
        createdAt = createdAt!!,
        updatedAt = updatedAt!!,
        createdBy = createdBy!!,
    )

fun User.toTokenResponse(
    accessToken: String,
    expiresIn: Long,
) = UserAndAccessTokenResponse(
    accessToken = accessToken,
    expiresIn = expiresIn,
    user = toReadResponse(),
)
