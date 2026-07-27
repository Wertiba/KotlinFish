package com.picoding.fish.services

import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.api.exceptions.userNotFound
import com.picoding.fish.api.utils.security.UserPrincipal
import com.picoding.fish.core.mappers.toReadResponse
import com.picoding.fish.core.schemas.user.AdminRegisterUserBody
import com.picoding.fish.core.schemas.user.UserPutBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.schemas.user.UserRole
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val hashEncoder: HashEncoder,
) {
    fun createUser(
        data: AdminRegisterUserBody,
        adminId: UUID,
    ): UserReadResponse {
        ensureEmailAvailable(data.email)
        val createdUser =
            User(
                email = data.email,
                password = hashEncoder.encode(data.password),
                fullName = data.fullName,
                role = data.role,
                createdBy = adminId,
            )
        return userRepository.save(createdUser).toReadResponse()
    }

    fun getAllUsers(
        page: Int,
        size: Int,
    ): PageResponse<UserReadResponse> {
        val result = userRepository.findAllByOrderByIsActiveDescCreatedAtDesc(PageRequest.of(page, size))
        return PageResponse.of(result.map { it.toReadResponse() })
    }

    fun getUserById(userId: UUID): UserReadResponse = getUserByUserId(userId).toReadResponse()

    @Transactional
    fun updateUserById(
        userId: UUID,
        data: UserPutBody,
        caller: UserPrincipal,
    ): UserReadResponse {
        val user = getUserByUserId(userId)
        ensureCanChangeRoleAndActiveState(user, data, caller)
        val updatedUser =
            userRepository.save(
                user.copy(
                    fullName = data.fullName,
                    role = data.role,
                    isActive = data.isActive,
                    updatedAt = Instant.now(),
                ),
            )
        revokeSessionsIfJustDeactivated(user, updatedUser)
        return updatedUser.toReadResponse()
    }

    @Transactional
    fun deleteUserById(userId: UUID) {
        val user = getUserByUserId(userId)
        val deactivatedUser =
            userRepository.save(
                user.copy(
                    isActive = false,
                    updatedAt = Instant.now(),
                ),
            )
        revokeSessionsIfJustDeactivated(user, deactivatedUser)
    }

    private fun revokeSessionsIfJustDeactivated(
        before: User,
        after: User,
    ) {
        if (before.isActive && !after.isActive) {
            refreshTokenRepository.deleteByUserId(after.id!!)
        }
    }

    private fun ensureEmailAvailable(email: String) {
        if (userRepository.findByEmail(email) != null) throw userAlreadyExists(email)
    }

    private fun ensureCanChangeRoleAndActiveState(
        user: User,
        data: UserPutBody,
        caller: UserPrincipal,
    ) {
        if (caller.role == UserRole.ADMIN) return
        if (data.role != user.role) {
            throw AccessDeniedException("Only ADMIN can change role.")
        }
        if (data.isActive != user.isActive) {
            throw AccessDeniedException("Only ADMIN can change isActive.")
        }
    }

    private fun getUserByUserId(userId: UUID): User =
        userRepository.findById(userId).orElse(null)
            ?: throw userNotFound()
}
