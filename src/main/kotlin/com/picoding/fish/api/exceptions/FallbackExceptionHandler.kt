package com.picoding.fish.api.exceptions

import com.picoding.fish.core.utils.TraceId
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class FallbackExceptionHandler(
    private val objectMapper: ObjectMapper,
) : HandlerExceptionResolver {
    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView {
        logger.error("Unhandled exception on {}", request.requestURI, ex)

        val error =
            ApiError(
                code = "INTERNAL_ERROR",
                message = "An unexpected error occurred.",
                traceId = TraceId.current(),
                timestamp = Instant.now().toString(),
                path = request.requestURI,
            )

        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(error))

        return ModelAndView()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FallbackExceptionHandler::class.java)
    }
}
