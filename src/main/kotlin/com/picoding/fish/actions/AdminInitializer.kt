package com.picoding.fish.actions

import com.picoding.fish.core.Settings
import com.picoding.fish.core.schemas.user.UserRole
import com.picoding.fish.core.utils.HashEncoder
import com.picoding.fish.database.models.User
import com.picoding.fish.database.repositories.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminInitializer(
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val settings: Settings,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val existingAdmin = userRepository.findByEmail(settings.admin.email)
        if (existingAdmin != null) {
            logger.warn("Admin already exists!")
            return
        }

        val admin =
            userRepository.save(
                User(
                    email = settings.admin.email,
                    password = hashEncoder.encode(settings.admin.password),
                    fullName = settings.admin.fullname,
                    role = UserRole.ADMIN,
                ),
            )

        logger.info("Initial admin user created: ${admin.email}")
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(AdminInitializer::class.java)
    }
}
