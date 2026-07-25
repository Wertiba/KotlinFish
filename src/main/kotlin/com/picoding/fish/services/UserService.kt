package com.picoding.fish.services

import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.api.exceptions.userNotFound
import com.picoding.fish.core.mappers.toReadResponse
import com.picoding.fish.core.schemas.user.AdminRegisterUserBody
import com.picoding.fish.core.schemas.user.UserPutBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
) {
    fun createUser(
        data: AdminRegisterUserBody,
        adminId: String,
    ): UserReadResponse {
        ensureEmailAvailable(data.email)
        val createdUser =
            User(
                email = data.email,
                password = hashEncoder.encode(data.password),
                fullName = data.fullName,
                role = data.role,
                createdBy = UUID.fromString(adminId),
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

    fun updateUserById(
        userId: UUID,
        data: UserPutBody,
    ): UserReadResponse {
        val user = getUserByUserId(userId)
        val updatedUser =
            userRepository.save(
                user.copy(
                    fullName = data.fullName,
                    role = data.role,
                    isActive = data.isActive,
                    updatedAt = Instant.now(),
                ),
            )
        return updatedUser.toReadResponse()
    }

    fun deleteUserById(userId: UUID) {
        val user = getUserByUserId(userId)
        userRepository.save(
            user.copy(
                isActive = false,
            ),
        )
    }

    private fun ensureEmailAvailable(email: String) {
        if (userRepository.findByEmail(email) != null) throw userAlreadyExists(email)
    }

    private fun getUserByUserId(userId: UUID): User =
        userRepository.findById(userId).orElse(null)
            ?: throw userNotFound()
}
