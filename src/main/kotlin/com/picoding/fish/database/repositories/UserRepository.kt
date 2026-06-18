package com.picoding.fish.database.repositories

import com.picoding.fish.database.models.User
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UserRepository : CrudRepository<User, UUID> {
    fun findByEmail(email: String): User?
}
