package com.picoding.fish.api.utils.cookie

import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CookieHelper {
    fun getCookie(
        token: String,
        secure: Boolean = false,
    ) = ResponseCookie
        .from("refreshToken", token)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(Duration.ofDays(7))
        .sameSite("Lax")
        .build()

    fun clearCookie() =
        ResponseCookie
            .from("refreshToken", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build()
}
