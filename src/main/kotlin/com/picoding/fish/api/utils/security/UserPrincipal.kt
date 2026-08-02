package com.picoding.fish.api.utils.security

import com.picoding.fish.core.dto.user.UserRole
import java.util.UUID

data class UserPrincipal(
    val id: UUID,
    val role: UserRole,
)
