package com.picoding.fish.api.controllers

import com.picoding.fish.core.schemas.requests.PingResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/health")
class HeathController {
    @GetMapping("/ping")
    fun ping(): PingResponse = PingResponse("ok")
}
