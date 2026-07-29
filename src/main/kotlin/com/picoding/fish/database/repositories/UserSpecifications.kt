package com.picoding.fish.database.repositories

import com.picoding.fish.core.schemas.user.UserFilterQuery
import com.picoding.fish.core.specification.combineFilters
import com.picoding.fish.core.specification.whereAfterOrEqual
import com.picoding.fish.core.specification.whereBeforeOrEqual
import com.picoding.fish.core.specification.whereContainsIgnoreCase
import com.picoding.fish.core.specification.whereEqual
import com.picoding.fish.database.models.User
import org.springframework.data.jpa.domain.Specification

fun UserFilterQuery.toSpecification(): Specification<User> =
    combineFilters(
        whereContainsIgnoreCase("email", email),
        whereContainsIgnoreCase("fullName", fullName),
        whereEqual("role", role),
        whereEqual("isActive", isActive),
        whereAfterOrEqual("createdAt", createdFrom),
        whereBeforeOrEqual("createdAt", createdTo),
    )
