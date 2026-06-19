package com.picoding.fish.api.utils.security

import com.picoding.fish.api.exceptions.ApiError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Component
class JwtAuthEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_UNAUTHORIZED

        val error =
            ApiError(
                code = "UNAUTHORIZED",
                message = "The token is missing or invalid",
                traceId = UUID.randomUUID().toString(),
                timestamp = Instant.now().toString(),
                path = request.requestURI,
            )

        response.writer.write(objectMapper.writeValueAsString(error))
    }
}
