package com.jayathu.minstagram.presentation.history

import com.jayathu.minstagram.data.local.SessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// One day's worth of usage, used for the chart.
data class DayBucket(
    val date: LocalDate,
    val totalSeconds: Int,
    val sessionCount: Int
)

// A dated group of sessions for the Chrome-style list.
data class DaySection(
    val label: String,
    val date: LocalDate,
    val sessions: List<SessionEntity>
)

data class RangeSummary(
    val totalSeconds: Int,
    val activeDays: Int,
    val sessionCount: Int
) {
    val avgPerActiveDaySeconds: Int
        get() = if (activeDays == 0) 0 else totalSeconds / activeDays
}

private fun SessionEntity.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startedAtMs).atZone(zone).toLocalDate()

// One bucket per day for the last [rangeDays] days, oldest first so a chart
// reads left to right. Empty days are included with zero, so gaps show.
fun dailyBuckets(
    sessions: List<SessionEntity>,
    rangeDays: Int,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<DayBucket> {
    val byDay = sessions.groupBy { it.localDate(zone) }
    val start = today.minusDays((rangeDays - 1).toLong())
    return (0 until rangeDays).map { i ->
        val day = start.plusDays(i.toLong())
        val ofDay = byDay[day].orEmpty()
        DayBucket(day, ofDay.sumOf { it.actualSeconds }, ofDay.size)
    }
}

fun rangeSummary(buckets: List<DayBucket>): RangeSummary =
    RangeSummary(
        totalSeconds = buckets.sumOf { it.totalSeconds },
        activeDays = buckets.count { it.sessionCount > 0 },
        sessionCount = buckets.sumOf { it.sessionCount }
    )

private val sectionFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

// Group sessions by day, newest first, with Today / Yesterday labels.
fun groupByDay(
    sessions: List<SessionEntity>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<DaySection> =
    sessions
        .groupBy { it.localDate(zone) }
        .entries
        .sortedByDescending { it.key }
        .map { (date, ofDay) ->
            val label = when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(sectionFormat)
            }
            DaySection(label, date, ofDay.sortedByDescending { it.startedAtMs })
        }
