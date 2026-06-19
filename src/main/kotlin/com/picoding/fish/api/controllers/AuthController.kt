package com.picoding.fish.api.controllers

import com.picoding.fish.core.schemas.requests.RefreshRequest
import com.picoding.fish.core.schemas.token.TokenPair
import com.picoding.fish.core.schemas.user.UserInfoResponse
import com.picoding.fish.core.schemas.user.UserLoginBody
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.services.AuthService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "API for users authentication")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody body: UserRegisterBody,
    ): UserInfoResponse = authService.register(body)

    @PostMapping("/login")
    fun login(
        @RequestBody body: UserLoginBody,
    ): TokenPair = authService.login(body)

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest,
    ): TokenPair = authService.refresh(body.refreshToken)
}
