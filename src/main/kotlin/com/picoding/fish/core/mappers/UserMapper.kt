package com.picoding.fish.core.mappers

import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.database.models.User

fun User.toReadResponse() =
    UserReadResponse(
        id = id!!,
        email = email,
        fullName = fullName,
        role = role,
    )
