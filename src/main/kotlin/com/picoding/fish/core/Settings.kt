package com.picoding.fish.core

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "app")
data class Settings
    @ConstructorBinding
    constructor(
        val security: Security,
        val cors: Cors,
        val admin: Admin,
        val cookie: Cookie = Cookie(secure = true),
    ) {
        data class Security(
            @field:NotBlank
            val jwtSecret: String,
            @DefaultValue("15m")
            val accessTokenExpiration: Duration,
            @DefaultValue("30d")
            val refreshTokenExpiration: Duration,
        ) {
            val accessTokenValidMs: Long get() = accessTokenExpiration.toMillis()
            val refreshTokenValidMs: Long get() = refreshTokenExpiration.toMillis()

            override fun toString(): String =
                "Security(jwtSecret=***, accessTokenExpiration=$accessTokenExpiration, refreshTokenExpiration=$refreshTokenExpiration)"
        }

        data class Cors(
            @DefaultValue
            val allowedOrigins: List<String> = emptyList(),
        )

        data class Admin(
            val email: String,
            val password: String,
            val fullname: String,
        )

        data class Cookie(
            @DefaultValue("true")
            val secure: Boolean,
        )
    }
