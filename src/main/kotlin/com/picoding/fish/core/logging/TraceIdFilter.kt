package com.picoding.fish.core.logging

import com.picoding.fish.core.utils.TraceId
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        MDC.put(TraceId.MDC_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TraceId.MDC_KEY)
        }
    }

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }
}
