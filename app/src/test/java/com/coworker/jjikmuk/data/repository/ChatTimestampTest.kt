package com.coworker.jjikmuk.data.repository

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ChatTimestampTest {
    @Test fun `local server dates and explicit Z both represent UTC`() {
        val expected = Instant.parse("2026-11-11T01:00:00Z").toEpochMilli()
        assertEquals(expected, parseChatTimestamp("2026-11-11T01:00:00"))
        assertEquals(expected, parseChatTimestamp("2026-11-11T01:00:00Z"))
    }
    @Test fun `fractional server timestamps preserve milliseconds without rounding seconds`() {
        for (fraction in listOf("1", "12", "123", "123456", "123999999")) {
            val value = "2026-11-11T01:00:00.$fraction"
            assertEquals(Instant.parse(value + "Z").toEpochMilli(), parseChatTimestamp(value))
        }
    }
    @Test fun `invalid calendar dates and trailing data are rejected`() {
        for (value in listOf("2026-02-30T01:00:00", "2026-11-11T25:00:00", "2026-11-11T01:00:00trailing", "")) {
            assertTrue(value, runCatching { parseChatTimestamp(value) }.isFailure)
        }
    }
}
