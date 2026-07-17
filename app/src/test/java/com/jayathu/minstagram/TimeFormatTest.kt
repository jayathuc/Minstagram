package com.jayathu.minstagram

import com.jayathu.minstagram.util.formatDuration
import com.jayathu.minstagram.util.sevenDaysAgoMs
import com.jayathu.minstagram.util.startOfTodayMs
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeFormatTest {

    @Test
    fun `seconds only`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("59s", formatDuration(59))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("1m 0s", formatDuration(60))
        assertEquals("2m 5s", formatDuration(125))
        assertEquals("15m 0s", formatDuration(900))
    }

    @Test
    fun `start of today is midnight in the given zone`() {
        val zone = ZoneId.of("Asia/Colombo")
        val now = ZonedDateTime.of(2026, 7, 17, 10, 30, 0, 0, zone)
        val expectedMidnight = ZonedDateTime.of(2026, 7, 17, 0, 0, 0, 0, zone)

        assertEquals(
            expectedMidnight.toInstant().toEpochMilli(),
            startOfTodayMs(now.toInstant().toEpochMilli(), zone)
        )
    }

    @Test
    fun `start of today just after midnight stays on the same day`() {
        val zone = ZoneId.of("Asia/Colombo")
        val now = ZonedDateTime.of(2026, 7, 17, 0, 0, 1, 0, zone)
        val expectedMidnight = ZonedDateTime.of(2026, 7, 17, 0, 0, 0, 0, zone)

        assertEquals(
            expectedMidnight.toInstant().toEpochMilli(),
            startOfTodayMs(now.toInstant().toEpochMilli(), zone)
        )
    }

    @Test
    fun `seven days ago`() {
        assertEquals(0L, sevenDaysAgoMs(7 * 24 * 60 * 60 * 1000L))
    }
}
