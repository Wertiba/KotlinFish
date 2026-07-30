package com.picoding.fish.core.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@Email(message = "Invalid email format.")
@Size(max = 254, message = "Email must be at most 254 characters long.")
annotation class ValidEmail(
    val message: String = "Invalid email format.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}\$",
    message = "Password must be between 8 and 72 characters long and contain at least one digit, uppercase and lowercase character.",
)
annotation class ValidPassword(
    val message: String = "Password must be at least 9 characters long and contain at least one digit, uppercase and lowercase character.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@Pattern(
    regexp = "^[а-яА-Яa-zA-Z0-9 _-]{2,200}$",
    message = "FullName must be between 2 and 200 characters long and contain only uppercase, lowercase, digits, space, - and _ symbols.",
)
annotation class ValidFullName(
    val message: String = "FullName must be at least 2 characters long and contain only uppercase, lowercase and _ symbols.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
