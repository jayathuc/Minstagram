package com.jayathu.minstagram.util

import java.time.Instant
import java.time.ZoneId

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}

fun startOfTodayMs(
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()

fun sevenDaysAgoMs(nowMs: Long = System.currentTimeMillis()): Long =
    nowMs - 7 * 24 * 60 * 60 * 1000L

// coarse form for stats lines: "6h 12m", "47m", "under a minute"
fun formatHoursMinutes(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "under a minute"
    }
}
