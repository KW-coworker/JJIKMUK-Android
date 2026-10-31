package com.coworker.jjikmuk.feature.auth.presentation.common

import org.junit.Assert.*
import org.junit.Test

class AuthPasswordValidationTest {
    @Test fun `new password requires eight characters letters digits and no whitespace or Hangul`() {
        for (password in listOf("abc123", "abcdefgh", "12345678", "abc1234 ", "abc1234\n", "abc1234가", "abc1234ᄀ", "abc1234é", "abc1234🙂", "abc1234\u007f", "abc1234\u0000", "abc1234\u200b")) {
            assertFalse(password, isAuthPasswordValid(password, password))
        }
        assertTrue(isAuthPasswordValid("abc12345", "abc12345"))
        assertTrue(isAuthPasswordValid("Abcd123!", "Abcd123!"))
        assertFalse(isAuthPasswordValid("Abcd123!", "abcd123!"))
    }
    @Test fun `UTF8 byte limit is enforced without trimming or modifying input`() {
        val exactly72 = "a1" + "!".repeat(70)
        assertTrue(isAuthPasswordValid(exactly72, exactly72))
        assertFalse(isAuthPasswordValid(exactly72 + "!", exactly72 + "!"))
        val over72 = "a1" + "é".repeat(36)
        assertFalse(isAuthPasswordValid(over72, over72))
    }
}
