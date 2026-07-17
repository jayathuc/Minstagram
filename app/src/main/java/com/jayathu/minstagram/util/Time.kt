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
