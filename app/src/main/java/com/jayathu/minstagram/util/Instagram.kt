package com.jayathu.minstagram.util

import android.app.AppOpsManager
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

// Real Instagram screen time from the system, sessions or not.
fun instagramUsageMs(context: Context, sinceMs: Long): Long {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return 0L
    val now = System.currentTimeMillis()
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sinceMs, now)
        ?: return 0L
    return stats
        .filter { it.packageName == INSTAGRAM_PACKAGE && it.lastTimeUsed >= sinceMs }
        .sumOf { it.totalTimeInForeground }
}

fun isReelWatcherEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(context.packageName) && enabled.contains("ReelWatcherService")
}
