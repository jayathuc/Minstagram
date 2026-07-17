package com.jayathu.minstagram.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jayathu.minstagram.MainActivity
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.service.SessionService

// Handles the "End Session" button on the session notification.
class EndSessionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_END_SESSION = "com.jayathu.minstagram.ACTION_END_SESSION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END_SESSION) return

        // read before stopping the service, stopping clears these
        val prefs = Prefs.get(context)
        val intentionName = prefs.getString(Prefs.SESSION_INTENTION, null)
            ?: SessionIntention.JUST_BROWSING.name
        val durationSeconds = prefs.getInt(Prefs.SESSION_ACCUMULATED_SECONDS, 0)

        prefs.edit()
            .putString(Prefs.PENDING_SUMMARY_INTENTION, intentionName)
            .putInt(Prefs.PENDING_SUMMARY_SECONDS, durationSeconds)
            .apply()

        context.stopService(Intent(context, SessionService::class.java))

        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }
}
