package com.jayathu.minstagram.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "minstagram_prefs"

    const val AUTO_CLOSE = "auto_close_on_expiry"

    // active session, lets the service recover if it gets restarted
    const val SESSION_ACTIVE = "session_active"
    const val SESSION_INTENTION = "session_intention"
    const val SESSION_LIMIT_SECONDS = "session_limit_seconds"
    const val SESSION_ACCUMULATED_SECONDS = "session_accumulated_seconds"
    const val SESSION_STARTED_AT_MS = "session_started_at_ms"
    const val SESSION_WAS_INTERCEPTED = "session_was_intercepted"

    // bumped by the reel watcher while a session runs
    const val REELS_THIS_SESSION = "reels_this_session"

    // set when a session ends while the user is in another app,
    // shown next time they open Minstagram
    const val PENDING_SUMMARY_INTENTION = "pending_summary_intention"
    const val PENDING_SUMMARY_SECONDS = "pending_summary_seconds"

    const val SNOOZE_UNTIL_MS = "snooze_until_ms"

    // set by the monitor right before it throws the gate in front of Instagram
    const val INTERCEPTED_PROMPT = "intercepted_prompt"

    // usage events lag a little, so the monitor waits a moment after a
    // session ends before it starts intercepting again
    const val LAST_SESSION_END_MS = "last_session_end_ms"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun clearSession(context: Context) {
        get(context).edit()
            .remove(SESSION_ACTIVE)
            .remove(SESSION_INTENTION)
            .remove(SESSION_LIMIT_SECONDS)
            .remove(SESSION_ACCUMULATED_SECONDS)
            .remove(SESSION_STARTED_AT_MS)
            .remove(SESSION_WAS_INTERCEPTED)
            .apply()
    }
}
