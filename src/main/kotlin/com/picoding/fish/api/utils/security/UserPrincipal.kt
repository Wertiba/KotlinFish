package com.picoding.fish.api.utils.security

import com.picoding.fish.core.schemas.user.UserRole
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val role: UserRole,
)
