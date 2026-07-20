package com.jayathu.minstagram.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.jayathu.minstagram.MainActivity
import com.jayathu.minstagram.R
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.util.ForegroundAppDetector
import com.jayathu.minstagram.util.INSTAGRAM_PACKAGE

// Watches for Instagram being opened directly and puts the intention gate in front of it.
class UsageMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "usage_monitor_channel"
        const val NOTIFICATION_ID = 2
        private const val POLL_INTERVAL_MS = 2000L
        private const val POST_SESSION_GRACE_MS = 15_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var detector: ForegroundAppDetector

    private val pollTask = object : Runnable {
        override fun run() {
            if (shouldIntercept() && detector.isForeground(INSTAGRAM_PACKAGE)) {
                launchIntentionGate()
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private fun shouldIntercept(): Boolean {
        val prefs = Prefs.get(this)
        if (prefs.getBoolean(Prefs.SESSION_ACTIVE, false)) return false
        val now = System.currentTimeMillis()
        if (now < prefs.getLong(Prefs.SNOOZE_UNTIL_MS, 0L)) return false
        // grace period after a session ends, usage events can lag behind
        return now - prefs.getLong(Prefs.LAST_SESSION_END_MS, 0L) > POST_SESSION_GRACE_MS
    }

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(pollTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollTask)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun launchIntentionGate() {
        // flag goes through prefs, extras get dropped when the task already exists
        Prefs.get(this).edit().putBoolean(Prefs.INTERCEPTED_PROMPT, true).apply()
        // launching from a service is allowed because we hold the overlay permission
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_minstagram)
            .setContentTitle("Minstagram")
            .setContentText("Monitoring mindful usage")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitor",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Background monitoring for mindful Instagram usage"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
