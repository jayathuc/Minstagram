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
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.jayathu.minstagram.R
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.receiver.EndSessionReceiver

class SessionService : Service() {

    companion object {
        const val EXTRA_INTENTION = "extra_intention"
        const val CHANNEL_ID = "session_channel"
        const val NOTIFICATION_ID = 1
    }

    private val handler = Handler(Looper.getMainLooper())
    private var startTimeMs = 0L
    private var intention = SessionIntention.JUST_BROWSING

    private var overlayView: TextView? = null
    private var windowManager: WindowManager? = null

    private val tick = object : Runnable {
        override fun run() {
            val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            updateNotification(elapsed)
            updateOverlay(elapsed)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intentionName = intent?.getStringExtra(EXTRA_INTENTION) ?: SessionIntention.JUST_BROWSING.name
        intention = SessionIntention.valueOf(intentionName)
        startTimeMs = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification(0))
        showOverlay()
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Floating overlay banner ---

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val tv = TextView(this).apply {
            text = "${intention.emoji} ${intention.label}  |  0s"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt()) // 60% black
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
            y = dpToPx(48) // Below the status bar
        }

        windowManager?.addView(tv, params)
        overlayView = tv
    }

    private fun updateOverlay(elapsedSeconds: Int) {
        overlayView?.text = "${intention.emoji} ${intention.label}  |  ${formatDuration(elapsedSeconds)}"
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()

    // --- Notification ---

    private fun updateNotification(elapsedSeconds: Int) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(elapsedSeconds))
    }

    private fun buildNotification(elapsedSeconds: Int): Notification {
        val endIntent = Intent(this, EndSessionReceiver::class.java).apply {
            action = EndSessionReceiver.ACTION_END_SESSION
            putExtra(EXTRA_INTENTION, intention.name)
            putExtra(EndSessionReceiver.EXTRA_START_TIME_MS, startTimeMs)
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            this, 0, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${intention.emoji} ${intention.label}")
            .setContentText("Session time: ${formatDuration(elapsedSeconds)}")
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "End Session", endPendingIntent)
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

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
    }
}
