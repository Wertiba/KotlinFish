package com.picoding.fish.core.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HashEncoderTest {
    private val hashEncoder = HashEncoder()

    @Test
    fun `encode produces a hash different from the raw input that matches it`() {
        val encoded = hashEncoder.encode("Password123")

        assertNotEquals("Password123", encoded)
        assertTrue(hashEncoder.matches("Password123", encoded))
    }

    @Test
    fun `matches fails for an incorrect raw value`() {
        val encoded = hashEncoder.encode("Password123")

        assertFalse(hashEncoder.matches("WrongPassword", encoded))
    }

    @Test
    fun `encoding the same value twice yields different salted hashes`() {
        val first = hashEncoder.encode("Password123")
        val second = hashEncoder.encode("Password123")

        assertNotEquals(first, second)
        assertTrue(hashEncoder.matches("Password123", first))
        assertTrue(hashEncoder.matches("Password123", second))
    }
}
