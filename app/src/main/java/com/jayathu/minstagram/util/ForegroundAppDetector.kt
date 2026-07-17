package com.jayathu.minstagram.util

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager

// Tracks the foreground app via usage events. Needs usage access permission.
class ForegroundAppDetector(context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private var lastEventTime = System.currentTimeMillis() - LOOKBACK_MS
    private var foregroundPackage: String? = null

    fun isForeground(packageName: String): Boolean {
        // screen off or locked counts as not in the app
        if (!powerManager.isInteractive || keyguardManager.isKeyguardLocked) return false

        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastEventTime, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            }
            if (event.timeStamp > lastEventTime) lastEventTime = event.timeStamp
        }
        return foregroundPackage == packageName
    }

    companion object {
        private const val LOOKBACK_MS = 60_000L
    }
}
