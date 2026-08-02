package com.picoding.fish.core.utils

import org.slf4j.MDC
import java.util.UUID

object TraceId {
    const val MDC_KEY = "traceId"

    fun current(): String = MDC.get(MDC_KEY) ?: UUID.randomUUID().toString()
}
