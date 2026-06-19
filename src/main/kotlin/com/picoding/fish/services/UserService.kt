package com.picoding.fish.services

import com.picoding.fish.api.exceptions.userAlreadyExists
import com.picoding.fish.api.exceptions.userNotFound
import com.picoding.fish.core.mappers.toReadResponse
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    private fun getUserByUserId(userId: UUID): User {
        val user =
            userRepository.findById(userId).orElse(null)
                ?: throw userNotFound()
        return user
    }

    fun getAllUsers(pageable: Pageable): PageResponse<UserReadResponse> {
        val page = userRepository.findAll(pageable)
        return PageResponse.of(page.map { it.toReadResponse() })
    }

    fun createUser(data: UserRegisterBody): UserReadResponse {
        userRepository.findByEmail(data.email) ?: throw userAlreadyExists(data.email)
        val createdUser =
            User(
                email = data.email,
                password = data.password,
                fullName = data.fullName,
                role = data.role,
            )
        return userRepository.save(createdUser).toReadResponse()
    }

    fun getUserById(userId: UUID): UserReadResponse = getUserByUserId(userId).toReadResponse()

    fun updateUserById(
        userId: UUID,
        data: UserRegisterBody,
    ): UserReadResponse {
        val user = getUserByUserId(userId)
        val updatedUser =
            userRepository.save(
                user.copy(
                    email = data.email,
                    password = data.password,
                    fullName = data.fullName,
                    role = data.role,
                ),
            )
        return updatedUser.toReadResponse()
    }

    fun deleteUserById(userId: UUID) {
        getUserByUserId(userId)
        userRepository.deleteById(userId)
    }
}
