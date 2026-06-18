package com.picoding.fish.controllers

import com.picoding.fish.core.schemas.requests.RefreshRequest
import com.picoding.fish.core.schemas.token.TokenPair
import com.picoding.fish.core.schemas.user.UserLoginBody
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.services.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: UserRegisterBody,
    ) {
        authService.register(body)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: UserLoginBody,
    ): TokenPair = authService.login(body)

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest,
    ): TokenPair = authService.refresh(body.refreshToken)
}
