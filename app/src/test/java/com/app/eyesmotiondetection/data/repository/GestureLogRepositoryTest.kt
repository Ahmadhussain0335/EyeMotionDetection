package com.app.eyesmotiondetection.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class GestureLogRepositoryTest {

    @Test
    fun `start of day uses local midnight, not UTC`() {
        val karachi = ZoneId.of("Asia/Karachi") // UTC+5
        // 02:00 local on 8 Oct = 21:00 UTC on 7 Oct
        val now = Instant.parse("2026-10-07T21:00:00Z")
        val expected = Instant.parse("2026-10-07T19:00:00Z").toEpochMilli() // 00:00 local
        assertEquals(expected, startOfDayMillis(now, karachi))
    }

    @Test
    fun `start of day at exactly midnight is that midnight`() {
        val utc = ZoneId.of("UTC")
        val midnight = Instant.parse("2026-10-08T00:00:00Z")
        assertEquals(midnight.toEpochMilli(), startOfDayMillis(midnight, utc))
    }
}
