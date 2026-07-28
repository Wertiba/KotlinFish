package com.picoding.fish.api.health

import org.slf4j.LoggerFactory
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class DbHealthIndicator(
    private val dataSource: DataSource,
) : HealthIndicator {
    override fun health(): Health =
        try {
            if (dataSource.connection.use { it.isValid(2) }) {
                Health.up().build()
            } else {
                logger.warn("Readiness check failed: database connection is not valid")
                Health.down().build()
            }
        } catch (e: Exception) {
            logger.warn("Readiness check failed: database is unreachable", e)
            Health.down(e).build()
        }

    companion object {
        private val logger = LoggerFactory.getLogger(DbHealthIndicator::class.java)
    }
}
