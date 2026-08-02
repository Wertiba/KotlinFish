package com.picoding.fish.services

import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.api.exceptions.userNotFound
import com.picoding.fish.api.utils.security.UserPrincipal
import com.picoding.fish.core.dto.user.AdminRegisterUserBody
import com.picoding.fish.core.dto.user.UserFilterQuery
import com.picoding.fish.core.dto.user.UserPutBody
import com.picoding.fish.core.dto.user.UserReadResponse
import com.picoding.fish.core.dto.user.UserRole
import com.picoding.fish.core.mappers.toReadResponse
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.RefreshTokenRepository
import com.picoding.fish.database.repositories.UserRepository
import com.picoding.fish.database.repositories.toSpecification
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        val createdUser =
            User(
                email = data.email,
                password = hashEncoder.encode(data.password),
                fullName = data.fullName,
                role = data.role,
                createdBy = adminId,
            )
        return try {
            userRepository.save(createdUser).toReadResponse()
        } catch (_: DataIntegrityViolationException) {
            throw userAlreadyExists(data.email)
        }
    }

    fun getAllUsers(
        filter: UserFilterQuery,
        page: Int,
        size: Int,
    ): PageResponse<UserReadResponse> {
        val sort = Sort.by(Sort.Order.desc("isActive"), Sort.Order.desc("createdAt"))
        val result =
            userRepository.findAll(
                filter.toSpecification(),
                PageRequest.of(page, size, sort),
            )
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
        val wasActive = user.isActive

        user.fullName = data.fullName
        user.role = data.role
        user.isActive = data.isActive
        val updatedUser = userRepository.save(user)

        revokeSessionsIfJustDeactivated(updatedUser, wasActive)
        return updatedUser.toReadResponse()
    }

    @Transactional
    fun deleteUserById(userId: UUID) {
        val user = getUserByUserId(userId)
        val wasActive = user.isActive

        user.isActive = false
        val deactivatedUser = userRepository.save(user)
        revokeSessionsIfJustDeactivated(deactivatedUser, wasActive)
    }

    private fun revokeSessionsIfJustDeactivated(
        user: User,
        wasActive: Boolean,
    ) {
        if (wasActive && !user.isActive) {
            refreshTokenRepository.deleteByUserId(user.id!!)
        }
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
