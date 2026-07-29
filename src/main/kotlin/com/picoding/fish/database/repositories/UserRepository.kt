package com.picoding.fish.database.repositories

import com.picoding.fish.database.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface UserRepository :
    JpaRepository<User, UUID>,
    JpaSpecificationExecutor<User> {
    fun findByEmail(email: String): User?
}
