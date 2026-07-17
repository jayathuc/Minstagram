package com.jayathu.minstagram.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jayathu.minstagram.service.UsageMonitorService
import com.jayathu.minstagram.util.hasUsageAccess

// Restarts the usage monitor after a reboot so direct Instagram opens stay gated.
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!hasUsageAccess(context)) return
        context.startForegroundService(Intent(context, UsageMonitorService::class.java))
    }
}
