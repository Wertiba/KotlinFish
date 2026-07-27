package com.picoding.fish.api.controllers

import com.picoding.fish.api.utils.security.UserPrincipal
import com.picoding.fish.core.schemas.user.AdminRegisterUserBody
import com.picoding.fish.core.schemas.user.UserPutBody
import com.picoding.fish.core.schemas.user.UserReadResponse
import com.picoding.fish.core.utils.PageResponse
import com.picoding.fish.services.UserService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.parameters.P
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "Users", description = "API for users CRUD")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") @Min(0, message = "page must be >= 0.") page: Int,
        @RequestParam(defaultValue = "20") @Min(1, message = "size must be >= 1.") @Max(100, message = "size must be <= 100.") size: Int,
    ): PageResponse<UserReadResponse> = userService.getAllUsers(page, size)

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody body: AdminRegisterUserBody,
    ): UserReadResponse = userService.createUser(body, principal.id)

    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): UserReadResponse = userService.getUserById(principal.id)

    @PutMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody body: UserPutBody,
    ): UserReadResponse = userService.updateUserById(principal.id, body, principal)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    fun getUserById(
        @PathVariable @P("id") id: UUID,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): UserReadResponse = userService.getUserById(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    fun updateUserById(
        @PathVariable @P("id") id: UUID,
        @Valid @RequestBody body: UserPutBody,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): UserReadResponse = userService.updateUserById(id, body, principal)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUserById(
        @PathVariable id: UUID,
    ) = userService.deleteUserById(id)
}
