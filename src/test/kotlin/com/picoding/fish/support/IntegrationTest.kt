package com.picoding.fish.support

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

// Each concrete subclass declares its own static @Container/@ServiceConnection Postgres container
// (see e.g. AuthControllerIT). A container shared via a base class' companion object was found to
// get killed and recreated when Spring rotated between test classes' contexts, so every class owns one.
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
abstract class IntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // MockMvc dispatches directly to the DispatcherServlet, bypassing the server.servlet.context-path
    // that only applies to a real embedded container, so controller paths are used as-is here.
    fun url(path: String): String = path
}
