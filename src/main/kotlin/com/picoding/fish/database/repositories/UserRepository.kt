package com.picoding.fish.database.repositories

import com.picoding.fish.database.models.User
import org.springframework.data.repository.CrudRepository

interface UserRepository: CrudRepository<User, Long>
