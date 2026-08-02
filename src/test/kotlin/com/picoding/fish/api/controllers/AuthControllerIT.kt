package com.picoding.fish.api.controllers

import com.picoding.fish.core.dto.user.UserAndAccessTokenResponse
import com.picoding.fish.core.dto.user.UserLoginBody
import com.picoding.fish.core.dto.user.UserRegisterBody
import com.picoding.fish.support.IntegrationTest
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

class AuthControllerIT : IntegrationTest() {
    private fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")
    }

    private fun registerRaw(
        email: String,
        password: String = "Password123",
        fullName: String = "Test User",
    ): ResultActions =
        mockMvc.perform(
            post(url("/auth/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UserRegisterBody(email, password, fullName))),
        )

    private fun loginRaw(
        email: String,
        password: String,
    ): ResultActions =
        mockMvc.perform(
            post(url("/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UserLoginBody(email, password))),
        )

    private fun register(
        email: String = uniqueEmail(),
        password: String = "Password123",
        fullName: String = "Test User",
    ): Pair<UserAndAccessTokenResponse, Cookie> {
        val result = registerRaw(email, password, fullName).andExpect(status().isCreated).andReturn()
        val body = objectMapper.readValue(result.response.contentAsString, UserAndAccessTokenResponse::class.java)
        val cookie = result.response.getCookie("refreshToken")
        assertNotNull(cookie)
        return body to cookie!!
    }

    @Test
    fun `register creates a user and returns an access token with a refresh cookie`() {
        registerRaw(uniqueEmail())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
    }

    @Test
    fun `register rejects a duplicate email`() {
        val email = uniqueEmail()
        register(email = email)

        val result =
            registerRaw(email = email)
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andReturn()

        val traceIdHeader = result.response.getHeader("X-Trace-Id")
        val traceIdBody = objectMapper.readTree(result.response.contentAsString).get("traceId").asString()
        assertNotNull(traceIdHeader)
        assertEquals(traceIdHeader, traceIdBody)
    }

    @Test
    fun `register rejects an invalid payload`() {
        registerRaw(email = "not-an-email", password = "short", fullName = "A")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.length()", greaterThan(2)))
    }

    @Test
    fun `login succeeds with correct credentials`() {
        val email = uniqueEmail()
        register(email = email, password = "Password123")

        loginRaw(email, "Password123")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.email").value(email))
    }

    @Test
    fun `login fails with the wrong password`() {
        val email = uniqueEmail()
        register(email = email, password = "Password123")

        loginRaw(email, "WrongPassword1")
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `login fails for an unknown email`() {
        loginRaw(uniqueEmail(), "Password123").andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh issues a new access token and rotates the refresh cookie`() {
        val (_, cookie) = register()

        val result =
            mockMvc
                .perform(post(url("/auth/refresh")).cookie(cookie))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").isNotEmpty)
                .andReturn()

        assertNotNull(result.response.getCookie("refreshToken"))
    }

    @Test
    fun `refresh without a cookie is unauthorized`() {
        mockMvc.perform(post(url("/auth/refresh"))).andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh fails after the token has already been rotated once`() {
        val (_, cookie) = register()

        mockMvc.perform(post(url("/auth/refresh")).cookie(cookie)).andExpect(status().isOk)
        mockMvc.perform(post(url("/auth/refresh")).cookie(cookie)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout revokes the refresh token`() {
        val (_, cookie) = register()

        mockMvc.perform(post(url("/auth/logout")).cookie(cookie)).andExpect(status().isNoContent)
        mockMvc.perform(post(url("/auth/refresh")).cookie(cookie)).andExpect(status().isUnauthorized)
    }
}
