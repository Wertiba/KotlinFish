package com.picoding.fish.api.utils.cookie

import com.picoding.fish.core.Settings
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CookieHelper(
    private val settings: Settings,
) {
    fun getCookie(
        token: String,
        secure: Boolean = true,
    ): ResponseCookie =
        ResponseCookie
            .from("refreshToken", token)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(Duration.ofDays(settings.security.refreshTokenExpiration.toDays()))
            .sameSite("Lax")
            .build()

    fun clearCookie(): ResponseCookie =
        ResponseCookie
            .from("refreshToken", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build()
}
