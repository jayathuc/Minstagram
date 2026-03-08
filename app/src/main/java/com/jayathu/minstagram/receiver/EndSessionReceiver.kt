package com.jayathu.minstagram.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jayathu.minstagram.MainActivity
import com.jayathu.minstagram.service.SessionService

class EndSessionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_END_SESSION = "com.jayathu.minstagram.ACTION_END_SESSION"
        const val EXTRA_START_TIME_MS = "extra_start_time_ms"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END_SESSION) return

        val intentionName = intent.getStringExtra(SessionService.EXTRA_INTENTION) ?: return
        val startTimeMs = intent.getLongExtra(EXTRA_START_TIME_MS, 0L)
        val durationSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()

        context.stopService(Intent(context, SessionService::class.java))

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SHOW_SUMMARY, true)
            putExtra(SessionService.EXTRA_INTENTION, intentionName)
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
        }
        context.startActivity(mainIntent)
    }
}
