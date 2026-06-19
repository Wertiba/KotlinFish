package com.picoding.fish.api.controllers

import com.picoding.fish.api.utils.cookie.CookieHelper
import com.picoding.fish.core.schemas.requests.RefreshRequest
import com.picoding.fish.core.schemas.token.TokenPair
import com.picoding.fish.core.schemas.user.UserLoginBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.schemas.user.UserRegisterBody
import com.picoding.fish.services.AuthService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
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
    private val cookieHelper: CookieHelper,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody body: UserRegisterBody,
    ): UserReadResponse = authService.register(body)

    @PostMapping("/login")
    fun login(
        @RequestBody body: UserLoginBody,
    ): ResponseEntity<TokenPair> {
        val tokenPair = authService.login(body)
        val cookie = cookieHelper.getCookie(tokenPair.refreshToken)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(tokenPair)
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue("refreshToken") refreshToken: String,
    ): ResponseEntity<Unit> {
        authService.logout(refreshToken)
        val cookie = cookieHelper.clearCookie()

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest,
    ): ResponseEntity<TokenPair> {
        val tokenPair = authService.refresh(body.refreshToken)
        val cookie = cookieHelper.getCookie(tokenPair.refreshToken)
        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(tokenPair)
    }
}
