package com.picoding.fish.core.specification

import org.springframework.data.jpa.domain.Specification
import java.time.Instant

fun <T : Any> whereEqual(
    field: String,
    value: Any?,
): Specification<T>? = value?.let { Specification { root, _, cb -> cb.equal(root.get<Any>(field), it) } }

fun <T : Any> whereContainsIgnoreCase(
    field: String,
    value: String?,
): Specification<T>? =
    value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { term -> Specification { root, _, cb -> cb.like(cb.lower(root.get(field)), "%${term.lowercase()}%") } }

fun <T : Any> whereAfterOrEqual(
    field: String,
    value: Instant?,
): Specification<T>? = value?.let { Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get(field), it) } }

fun <T : Any> whereBeforeOrEqual(
    field: String,
    value: Instant?,
): Specification<T>? = value?.let { Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get(field), it) } }

fun <T : Any> combineFilters(vararg specs: Specification<T>?): Specification<T> = Specification.allOf(specs.filterNotNull())
