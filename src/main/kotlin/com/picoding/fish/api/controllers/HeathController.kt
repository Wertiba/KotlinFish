package com.picoding.fish.api.controllers

import com.picoding.fish.core.schemas.requests.PingResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "API health")
class HeathController(
    private val dataSource: DataSource,
) {
    @GetMapping("/ping")
    fun ping(): PingResponse = PingResponse("ok")

    @GetMapping("/ready")
    fun ready(): ResponseEntity<PingResponse> {
        val isDbUp =
            try {
                dataSource.connection.use { it.isValid(2) }
            } catch (e: Exception) {
                logger.warn("Readiness check failed: database is unreachable", e)
                false
            }

        return if (isDbUp) {
            logger.info("Database is ready, starting up application")
            ResponseEntity.ok(PingResponse("ok"))
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(PingResponse("down"))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeathController::class.java)
    }
}
