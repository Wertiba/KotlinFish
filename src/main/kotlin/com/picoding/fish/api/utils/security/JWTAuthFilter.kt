package com.picoding.fish.api.utils.security

import com.picoding.fish.api.exceptions.AppException
import com.picoding.fish.api.exceptions.userInactive
import com.picoding.fish.api.exceptions.userNotFound
import com.picoding.fish.database.repositories.UserRepository
import com.picoding.fish.services.JWTService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.util.UUID

@Component
class JWTAuthFilter(
    private val jwtService: JWTService,
    private val userRepository: UserRepository,
    @Qualifier("handlerExceptionResolver") private val exceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                if (jwtService.validateAccessToken(authHeader)) {
                    val userId = jwtService.getUserIdFromToken(authHeader)
                    val user =
                        userRepository.findById(UUID.fromString(userId)).orElse(null)
                            ?: throw userNotFound("User not found (invalid userId).")

                    if (!user.isActive) {
                        throw userInactive()
                    }

                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role}"))
                    val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        } catch (ex: AppException) {
            exceptionResolver.resolveException(request, response, null, ex)
            return
        }

        filterChain.doFilter(request, response)
    }
}
