package com.jayathu.minstagram

import com.jayathu.minstagram.data.local.SessionEntity
import com.jayathu.minstagram.presentation.history.dailyBuckets
import com.jayathu.minstagram.presentation.history.groupByDay
import com.jayathu.minstagram.presentation.history.rangeSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryDataTest {

    private val zone: ZoneId = ZoneId.of("Asia/Colombo")
    private val today = LocalDate.of(2026, 7, 20)

    private fun sessionOn(date: LocalDate, hour: Int, seconds: Int): SessionEntity {
        val ms = ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        return SessionEntity(
            intention = "CHECK_DMS",
            plannedSeconds = 300,
            actualSeconds = seconds,
            startedAtMs = ms,
            endedAtMs = ms + seconds * 1000L,
            wasIntercepted = false,
            endReason = "COMPLETED"
        )
    }

    @Test
    fun `daily buckets cover the whole range including empty days`() {
        val sessions = listOf(
            sessionOn(today, 9, 120),
            sessionOn(today, 11, 60),
            sessionOn(today.minusDays(2), 8, 300)
        )
        val buckets = dailyBuckets(sessions, rangeDays = 7, today = today, zone = zone)

        assertEquals(7, buckets.size)
        assertEquals(today.minusDays(6), buckets.first().date) // oldest first
        assertEquals(today, buckets.last().date)
        assertEquals(180, buckets.last().totalSeconds)         // 120 + 60 today
        assertEquals(2, buckets.last().sessionCount)
        assertEquals(300, buckets[4].totalSeconds)             // two days ago
        assertEquals(0, buckets[5].totalSeconds)               // yesterday empty
    }

    @Test
    fun `range summary counts totals and active days`() {
        val sessions = listOf(
            sessionOn(today, 9, 120),
            sessionOn(today, 11, 60),
            sessionOn(today.minusDays(2), 8, 300)
        )
        val summary = rangeSummary(dailyBuckets(sessions, 7, today, zone))
        assertEquals(480, summary.totalSeconds)
        assertEquals(2, summary.activeDays)
        assertEquals(3, summary.sessionCount)
        assertEquals(240, summary.avgPerActiveDaySeconds)
    }

    @Test
    fun `grouping labels today and yesterday and sorts newest first`() {
        val sessions = listOf(
            sessionOn(today, 9, 60),
            sessionOn(today.minusDays(1), 9, 60),
            sessionOn(today.minusDays(5), 9, 60)
        )
        val sections = groupByDay(sessions, today, zone)
        assertEquals(3, sections.size)
        assertEquals("Today", sections[0].label)
        assertEquals("Yesterday", sections[1].label)
        assertEquals(today.minusDays(5), sections[2].date)
    }

    @Test
    fun `sessions on the same day land in one section`() {
        val sessions = listOf(
            sessionOn(today, 9, 60),
            sessionOn(today, 14, 60),
            sessionOn(today, 20, 60)
        )
        val sections = groupByDay(sessions, today, zone)
        assertEquals(1, sections.size)
        assertEquals(3, sections[0].sessions.size)
    }
}
