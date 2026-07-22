package com.jayathu.minstagram

import com.jayathu.minstagram.util.foregroundDurationMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Event type numbers used by the usage pairing (foreground, background).
private const val FG = 1
private const val BG = 2

class UsageTimeTest {

    @Test
    fun single_session_inside_window_counts_fully() {
        val events = listOf(FG to 100L, BG to 300L)
        assertEquals(200L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun session_starting_before_window_is_clipped_to_since() {
        // opened before midnight, closed after: only the part after counts
        val events = listOf(FG to -50L, BG to 100L)
        assertEquals(100L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun ongoing_session_counts_up_to_now() {
        val events = listOf(FG to 200L)
        assertEquals(800L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun session_running_past_now_is_clipped_to_now() {
        val events = listOf(FG to 500L, BG to 2000L)
        assertEquals(500L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun session_entirely_before_window_counts_nothing() {
        val events = listOf(FG to -500L, BG to -100L)
        assertEquals(0L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun a_lone_unmatched_background_is_ignored() {
        // no foreground to close, and the real one (if any) is caught by the
        // query lookback, so a stray background must not invent time
        val events = listOf(BG to 300L)
        assertEquals(0L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun many_unmatched_backgrounds_do_not_overcount() {
        // Instagram fires many background events; each must not add its own
        // overlapping stretch from the window start (the real over-count bug)
        val events = listOf(BG to 100L, BG to 500L, BG to 900L)
        assertEquals(0L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun foreground_then_stray_backgrounds_stay_bounded() {
        val events = listOf(FG to 100L, BG to 200L, BG to 800L, FG to 850L)
        // [100,200]=100, stray BG@800 ignored, open FG@850 -> [850,1000]=150
        assertEquals(250L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun total_never_exceeds_the_window() {
        val events = listOf(FG to -100L, BG to 200L, BG to 300L, BG to 400L, FG to 500L)
        val total = foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L)
        assertTrue("total $total must be within window", total in 0L..1000L)
    }

    @Test
    fun multiple_sessions_sum() {
        val events = listOf(FG to 100L, BG to 200L, FG to 400L, BG to 500L)
        assertEquals(200L, foregroundDurationMs(events, sinceMs = 0L, nowMs = 1000L))
    }

    @Test
    fun no_events_is_zero() {
        assertEquals(0L, foregroundDurationMs(emptyList(), sinceMs = 0L, nowMs = 1000L))
    }
}
