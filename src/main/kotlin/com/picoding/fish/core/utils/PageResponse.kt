package com.picoding.fish.core.utils

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
) {
    companion object {
        fun <T : Any> of(page: Page<T>) =
            PageResponse(
                items = page.content,
                total = page.totalElements,
                page = page.number,
                size = page.size,
            )
    }
}
