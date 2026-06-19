package com.picoding.fish.api.controllers

import com.picoding.fish.core.schemas.requests.PingResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "API health")
class HeathController {
    @GetMapping("/ping")
    fun ping(): PingResponse = PingResponse("ok")
}
