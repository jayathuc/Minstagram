package com.jayathu.minstagram.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.jayathu.minstagram.MainActivity
import com.jayathu.minstagram.R
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.receiver.EndSessionReceiver
import com.jayathu.minstagram.util.ForegroundAppDetector
import com.jayathu.minstagram.util.INSTAGRAM_PACKAGE
import com.jayathu.minstagram.util.formatDuration
import com.jayathu.minstagram.util.hasUsageAccess

class SessionService : Service() {

    companion object {
        const val EXTRA_INTENTION = "extra_intention"
        const val EXTRA_TIME_LIMIT_SECONDS = "extra_time_limit_seconds"
        const val CHANNEL_ID = "session_channel"
        const val NOTIFICATION_ID = 1

        // leave Instagram this long and the session ends on its own
        private const val ABANDON_AFTER_SECONDS = 60
    }

    private val handler = Handler(Looper.getMainLooper())
    private var detector: ForegroundAppDetector? = null

    private var intention = SessionIntention.JUST_BROWSING
    private var timeLimitSeconds = 300
    private var accumulatedSeconds = 0
    private var backgroundedSeconds = 0
    private var counting = false
    private var expired = false

    private var overlayView: TextView? = null
    private var expiryOverlay: View? = null
    private var windowManager: WindowManager? = null

    private val tick = object : Runnable {
        override fun run() {
            val inInstagram = detector?.isForeground(INSTAGRAM_PACKAGE) ?: true

            if (expired) {
                // expiry overlay is up; if they leave Instagram we're done
                if (!inInstagram) {
                    endSessionQuietly()
                    return
                }
            } else if (inInstagram) {
                if (!counting) resumeCounting()
                accumulatedSeconds++
                backgroundedSeconds = 0
                saveProgress()
                updateOverlay()

                if (accumulatedSeconds >= timeLimitSeconds) {
                    expired = true
                    onTimerExpired()
                }
            } else {
                if (counting) pauseCounting()
                backgroundedSeconds++
                if (backgroundedSeconds >= ABANDON_AFTER_SECONDS) {
                    endSessionQuietly()
                    return
                }
            }

            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs.get(this)

        if (intent != null) {
            val intentionName = intent.getStringExtra(EXTRA_INTENTION) ?: SessionIntention.JUST_BROWSING.name
            intention = SessionIntention.valueOf(intentionName)
            timeLimitSeconds = intent.getIntExtra(EXTRA_TIME_LIMIT_SECONDS, 300)
            accumulatedSeconds = 0
        } else if (prefs.getBoolean(Prefs.SESSION_ACTIVE, false)) {
            // the system restarted us mid-session, pick up where we left off
            intention = SessionIntention.valueOf(
                prefs.getString(Prefs.SESSION_INTENTION, null) ?: SessionIntention.JUST_BROWSING.name
            )
            timeLimitSeconds = prefs.getInt(Prefs.SESSION_LIMIT_SECONDS, 300)
            accumulatedSeconds = prefs.getInt(Prefs.SESSION_ACCUMULATED_SECONDS, 0)
        } else {
            stopSelf()
            return START_NOT_STICKY
        }

        prefs.edit()
            .putBoolean(Prefs.SESSION_ACTIVE, true)
            .putString(Prefs.SESSION_INTENTION, intention.name)
            .putInt(Prefs.SESSION_LIMIT_SECONDS, timeLimitSeconds)
            .putInt(Prefs.SESSION_ACCUMULATED_SECONDS, accumulatedSeconds)
            .apply()

        expired = false
        counting = true
        backgroundedSeconds = 0
        detector = if (hasUsageAccess(this)) ForegroundAppDetector(this) else null

        startForeground(NOTIFICATION_ID, buildCountingNotification())
        showOverlay()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        removeOverlay()
        removeExpiryOverlay()
        Prefs.clearSession(this)
        Prefs.get(this).edit()
            .putLong(Prefs.LAST_SESSION_END_MS, System.currentTimeMillis())
            .apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun saveProgress() {
        Prefs.get(this).edit()
            .putInt(Prefs.SESSION_ACCUMULATED_SECONDS, accumulatedSeconds)
            .apply()
    }

    // --- Pause and resume ---

    private fun resumeCounting() {
        counting = true
        overlayView?.visibility = View.VISIBLE
        notify(buildCountingNotification())
    }

    private fun pauseCounting() {
        counting = false
        overlayView?.visibility = View.GONE
        notify(buildPausedNotification())
    }

    // --- Timer expiry ---

    private fun onTimerExpired() {
        notify(buildExpiredNotification())
        if (isAutoCloseEnabled()) {
            endSession()
        } else {
            showExpiryOverlay()
        }
    }

    // Normal end: bring the app forward, it shows the summary from prefs.
    private fun endSession() {
        savePendingSummary()
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(mainIntent)
        stopSelf()
    }

    // The user walked away from Instagram. Don't interrupt whatever they're
    // doing now, just save the summary for the next time they open the app.
    private fun endSessionQuietly() {
        savePendingSummary()
        stopSelf()
    }

    private fun savePendingSummary() {
        Prefs.get(this).edit()
            .putString(Prefs.PENDING_SUMMARY_INTENTION, intention.name)
            .putInt(Prefs.PENDING_SUMMARY_SECONDS, accumulatedSeconds)
            .apply()
    }

    private fun isAutoCloseEnabled(): Boolean =
        Prefs.get(this).getBoolean(Prefs.AUTO_CLOSE, false)

    // --- Floating overlay banner ---

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) {
            updateOverlay()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val tv = TextView(this).apply {
            text = bannerText()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt())
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(48)
        }

        windowManager?.addView(tv, params)
        overlayView = tv
    }

    private fun bannerText(): String {
        val remaining = maxOf(timeLimitSeconds - accumulatedSeconds, 0)
        return "${intention.emoji} ${intention.label}  |  ${formatDuration(remaining)} left"
    }

    private fun updateOverlay() {
        if (expired) return
        overlayView?.text = bannerText()
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    // --- Full-screen expiry overlay ---

    private fun showExpiryOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            endSession()
            return
        }

        removeOverlay()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xE6000000.toInt())
            setPadding(dpToPx(32), dpToPx(32), dpToPx(32), dpToPx(32))
        }

        val title = TextView(this).apply {
            text = "Time's Up"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "${intention.emoji} ${intention.label}\nYou spent ${formatDuration(accumulatedSeconds)}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(16), 0, dpToPx(32))
        }
        layout.addView(subtitle)

        val exitButton = Button(this).apply {
            text = "Exit Instagram"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dpToPx(32), dpToPx(12), dpToPx(32), dpToPx(12))
            setOnClickListener { endSession() }
        }
        layout.addView(exitButton)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0, // focusable and touchable so the exit button works
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        windowManager?.addView(layout, params)
        expiryOverlay = layout
    }

    private fun removeExpiryOverlay() {
        expiryOverlay?.let {
            windowManager?.removeView(it)
            expiryOverlay = null
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()

    // --- Notification ---

    private fun notify(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun baseNotification(): NotificationCompat.Builder {
        val endIntent = Intent(this, EndSessionReceiver::class.java).apply {
            action = EndSessionReceiver.ACTION_END_SESSION
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            this, 0, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${intention.emoji} ${intention.label}")
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "End Session", endPendingIntent)
    }

    // counts down by itself, no need to repost every second
    private fun buildCountingNotification(): Notification {
        val remainingMs = maxOf(timeLimitSeconds - accumulatedSeconds, 0) * 1000L
        return baseNotification()
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remainingMs)
            .setContentText("remaining")
            .build()
    }

    private fun buildPausedNotification(): Notification {
        val remaining = maxOf(timeLimitSeconds - accumulatedSeconds, 0)
        return baseNotification()
            .setContentText("Paused. ${formatDuration(remaining)} left")
            .build()
    }

    private fun buildExpiredNotification(): Notification {
        return baseNotification()
            .setContentText("Time's up! Session: ${formatDuration(accumulatedSeconds)}")
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Minstagram Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows your active Minstagram session timer"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
