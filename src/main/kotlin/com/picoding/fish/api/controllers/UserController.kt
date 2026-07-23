package com.picoding.fish.api.controllers

import com.picoding.fish.core.schemas.user.AdminRegisterUserBody
import com.picoding.fish.core.schemas.user.UserPutBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.services.UserService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole(T(com.picoding.fish.core.schemas.user.UserRole).ADMIN.name())")
@Tag(name = "Users", description = "API for users CRUD")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("")
    fun getAllUsers(
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageResponse<UserReadResponse> = userService.getAllUsers(pageable)

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @RequestBody body: AdminRegisterUserBody,
    ): UserReadResponse = userService.createUser(body)

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable("id") userId: UUID,
    ): UserReadResponse = userService.getUserById(userId)

    @PutMapping("/{id}")
    fun updateUserById(
        @PathVariable("id") userId: UUID,
        @RequestBody body: UserPutBody,
    ): UserReadResponse = userService.updateUserById(userId, body)

    @GetMapping("/me")
    @PreAuthorize(
        "hasAnyRole(" +
            "T(com.picoding.fish.core.schemas.user.UserRole).ADMIN.name(), " +
            "T(com.picoding.fish.core.schemas.user.UserRole).USER.name())",
    )
    fun getMe(
        @AuthenticationPrincipal userId: String,
    ): UserReadResponse = userService.getUserById(UUID.fromString(userId))

    @PutMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal userId: String,
        @RequestBody body: UserPutBody,
    ): UserReadResponse = userService.updateUserById(UUID.fromString(userId), body)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUserById(
        @PathVariable("id") userId: UUID,
    ) = userService.deleteUserById(userId)
}
