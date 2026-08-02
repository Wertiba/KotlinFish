package com.picoding.fish.api.controllers

import com.picoding.fish.core.dto.user.AdminRegisterUserBody
import com.picoding.fish.core.dto.user.UserAndAccessTokenResponse
import com.picoding.fish.core.dto.user.UserLoginBody
import com.picoding.fish.core.dto.user.UserPutBody
import com.picoding.fish.core.dto.user.UserRegisterBody
import com.picoding.fish.core.dto.user.UserRole
import com.picoding.fish.support.IntegrationTest
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

class UserControllerIT : IntegrationTest() {
    private lateinit var adminToken: String

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")
    }

    @BeforeEach
    fun logInAsAdmin() {
        val result =
            mockMvc
                .perform(
                    post(url("/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserLoginBody("admin@test.local", "TestAdmin123"))),
                ).andExpect(status().isOk)
                .andReturn()
        adminToken = objectMapper.readValue(result.response.contentAsString, UserAndAccessTokenResponse::class.java).accessToken
    }

    private fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

    private fun registerUser(
        email: String = uniqueEmail(),
        password: String = "Password123",
        fullName: String = "Test User",
    ): UserAndAccessTokenResponse {
        val result =
            mockMvc
                .perform(
                    post(url("/auth/register"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UserRegisterBody(email, password, fullName))),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readValue(result.response.contentAsString, UserAndAccessTokenResponse::class.java)
    }

    @Test
    fun `unauthenticated requests to protected endpoints are rejected`() {
        mockMvc.perform(get(url("/users/me"))).andExpect(status().isUnauthorized)
        mockMvc.perform(get(url("/users"))).andExpect(status().isUnauthorized)
    }

    @Test
    fun `a regular user can read their own profile`() {
        val registered = registerUser()

        mockMvc
            .perform(get(url("/users/me")).header(HttpHeaders.AUTHORIZATION, "Bearer ${registered.accessToken}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(registered.user.email))
    }

    @Test
    fun `a regular user cannot list all users`() {
        val registered = registerUser()

        mockMvc
            .perform(get(url("/users")).header(HttpHeaders.AUTHORIZATION, "Bearer ${registered.accessToken}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `a regular user cannot read another user's profile`() {
        val userA = registerUser()
        val userB = registerUser()

        mockMvc
            .perform(
                get(url("/users/${userB.user.id}")).header(HttpHeaders.AUTHORIZATION, "Bearer ${userA.accessToken}"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `an admin can list users`() {
        registerUser()

        mockMvc
            .perform(get(url("/users?page=0&size=20")).header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
    }

    @Test
    fun `an admin can create a user directly`() {
        val email = uniqueEmail()

        mockMvc
            .perform(
                post(url("/users"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(AdminRegisterUserBody(email, "Password123", "Created By Admin", UserRole.USER)),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value(email))
    }

    @Test
    fun `an admin can update another user's role and active state`() {
        val registered = registerUser()

        mockMvc
            .perform(
                put(url("/users/${registered.user.id}"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(UserPutBody("Renamed User", UserRole.ADMIN, false))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.isActive").value(false))
    }

    @Test
    fun `a deactivated user can no longer log in`() {
        val registered = registerUser()

        mockMvc
            .perform(delete(url("/users/${registered.user.id}")).header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken"))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(
                post(url("/auth/login"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(UserLoginBody(registered.user.email, "Password123"))),
            ).andExpect(status().isLocked)
            .andExpect(jsonPath("$.code").value("USER_INACTIVE"))
    }

    @Test
    fun `a regular user cannot change their own role`() {
        val registered = registerUser()

        mockMvc
            .perform(
                put(url("/users/me"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${registered.accessToken}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(UserPutBody("Test User", UserRole.ADMIN, true))),
            ).andExpect(status().isForbidden)
    }
}
