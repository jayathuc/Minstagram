package com.jayathu.minstagram.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.widget.Toast

const val INSTAGRAM_PACKAGE = "com.instagram.android"

fun launchInstagram(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
    if (intent != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Instagram is not installed", Toast.LENGTH_SHORT).show()
    }
}

fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

// Foreground and background event types. These numbers match both
// UsageEvents.Event.ACTIVITY_RESUMED/ACTIVITY_PAUSED and the older
// MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND, so the same check works on every
// supported version. Kept as plain ints so the pairing stays unit testable.
private const val EVENT_FOREGROUND = 1
private const val EVENT_BACKGROUND = 2

// look back so a stretch that began before the window is still paired up
private const val FOREGROUND_LOOKBACK_MS = 12 * 60 * 60 * 1000L

// Real Instagram screen time from the system, from sinceMs until now. Built
// from raw usage events and clipped to the window, so "today" stays honest
// across midnight and the seven day figure keeps rolling. queryUsageStats was
// too coarse here: its daily buckets aren't clipped to the window and don't
// line up with local midnight, which over-counted the edges.
fun instagramUsageMs(context: Context, sinceMs: Long): Long {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return 0L
    val now = System.currentTimeMillis()
    val events = usm.queryEvents(sinceMs - FOREGROUND_LOOKBACK_MS, now)
    val instagramEvents = ArrayList<Pair<Int, Long>>()
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        if (event.packageName == INSTAGRAM_PACKAGE) {
            instagramEvents.add(event.eventType to event.timeStamp)
        }
    }
    return foregroundDurationMs(instagramEvents, sinceMs, now)
}

// Pure and testable. Pairs foreground and background events into stretches and
// adds up only the part of each stretch that falls inside [sinceMs, nowMs].
internal fun foregroundDurationMs(
    events: List<Pair<Int, Long>>,
    sinceMs: Long,
    nowMs: Long
): Long {
    var total = 0L
    var tracking = false
    var foregroundSince = 0L

    fun addClipped(start: Long, end: Long) {
        val from = maxOf(start, sinceMs)
        val until = minOf(end, nowMs)
        if (until > from) total += until - from
    }

    for ((type, timeStamp) in events) {
        when (type) {
            EVENT_FOREGROUND -> {
                foregroundSince = timeStamp
                tracking = true
            }
            EVENT_BACKGROUND -> {
                // only a background that closes a foreground we're tracking
                // counts. A real cross-window stretch has its foreground caught
                // by the lookback; a stray unmatched background (Instagram fires
                // many) must be ignored or overlapping intervals over-count.
                if (tracking) {
                    addClipped(foregroundSince, timeStamp)
                    tracking = false
                }
            }
        }
    }
    // still in the foreground at the end of the window
    if (tracking) addClipped(foregroundSince, nowMs)
    return total
}

fun isReelWatcherEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(context.packageName) && enabled.contains("ReelWatcherService")
}
