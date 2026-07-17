package com.jayathu.minstagram.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.QuizBank
import com.jayathu.minstagram.domain.QuizQuestion
import com.jayathu.minstagram.util.INSTAGRAM_PACKAGE

// Watches Instagram's Reels surface. Counts how many Reels go by and
// puts a small question between every few of them. The goal is to keep
// watching a conscious act, not to punish it.
class ReelWatcherService : AccessibilityService() {

    companion object {
        private const val REELS_PER_QUESTION = 3
        private const val SCROLL_DEBOUNCE_MS = 700L
        private const val SURFACE_CHECK_THROTTLE_MS = 300L

        // How long the badge lingers after the last time we saw the pager,
        // so it doesn't flicker during reel transitions.
        private const val BADGE_LINGER_MS = 5000L

        // Only reset the count for a genuinely new sitting, never a flap.
        private const val NEW_SITTING_GAP_MS = 60_000L

        // Instagram's reel pager view ids, may need updates over time
        private val REELS_PAGER_IDS = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_swipe_refresh_container"
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null

    private var inReels = false
    private var lastSurfaceCheckMs = 0L
    private var lastReelsSeenMs = 0L
    private var lastScrollMs = 0L
    private var reelCount = 0
    private var reelsSinceQuiz = 0

    private var badge: TextView? = null
    private var quizOverlay: View? = null
    private var questionView: TextView? = null
    private var answerButtons: List<Button> = emptyList()
    private var question: QuizQuestion? = null

    // media volume saved while a question is up, so the reel goes quiet
    private var savedMusicVolume = -1

    // hides the badge if the pager hasn't been seen for a while
    private val badgeHideCheck = Runnable {
        if (SystemClock.uptimeMillis() - lastReelsSeenMs >= BADGE_LINGER_MS) {
            inReels = false
            removeBadge()
        }
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // a state change into another app means the user left Instagram
                val pkg = event.packageName?.toString()
                if (pkg != null && pkg != INSTAGRAM_PACKAGE && !isOwnOverlay(pkg)) {
                    leaveInstagram()
                } else {
                    updateSurface()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> updateSurface()
            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                if (recentlyInReels() && quizOverlay == null) onReelScrolled()
        }
    }

    override fun onInterrupt() {
        leaveInstagram()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        removeQuiz()
        removeBadge()
        restoreMusic()
    }

    private fun isOwnOverlay(pkg: String) = pkg == packageName

    private fun recentlyInReels(): Boolean =
        inReels && SystemClock.uptimeMillis() - lastReelsSeenMs < BADGE_LINGER_MS

    private fun updateSurface() {
        val now = SystemClock.uptimeMillis()
        if (now - lastSurfaceCheckMs < SURFACE_CHECK_THROTTLE_MS) return
        lastSurfaceCheckMs = now

        val root = rootInActiveWindow ?: return
        val present = REELS_PAGER_IDS.any { id ->
            root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
        }
        // an empty tree read is not proof we left, so only act on a real hit
        if (!present) return

        val gap = now - lastReelsSeenMs
        lastReelsSeenMs = now

        if (!inReels) {
            inReels = true
            if (gap > NEW_SITTING_GAP_MS) {
                reelCount = 0
                reelsSinceQuiz = 0
            }
            showBadge()
        }
        updateBadge()

        handler.removeCallbacks(badgeHideCheck)
        handler.postDelayed(badgeHideCheck, BADGE_LINGER_MS + 1000L)
    }

    // User switched away from Instagram entirely. Tear everything down,
    // including any question that was up.
    private fun leaveInstagram() {
        inReels = false
        handler.removeCallbacks(badgeHideCheck)
        removeQuiz()
        removeBadge()
    }

    private fun onReelScrolled() {
        val now = SystemClock.uptimeMillis()
        val isNewSwipe = now - lastScrollMs > SCROLL_DEBOUNCE_MS
        lastScrollMs = now
        if (!isNewSwipe) return

        reelCount++
        reelsSinceQuiz++
        bumpSessionReelCount()
        updateBadge()

        if (reelsSinceQuiz >= REELS_PER_QUESTION) {
            reelsSinceQuiz = 0
            showQuiz()
        }
    }

    private fun bumpSessionReelCount() {
        val prefs = Prefs.get(this)
        if (prefs.getBoolean(Prefs.SESSION_ACTIVE, false)) {
            prefs.edit()
                .putInt(Prefs.REELS_THIS_SESSION, prefs.getInt(Prefs.REELS_THIS_SESSION, 0) + 1)
                .apply()
        }
    }

    // --- Counter badge ---

    private fun showBadge() {
        if (badge != null) return

        val tv = TextView(this).apply {
            text = badgeText()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt())
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(84)
        }

        windowManager?.addView(tv, params)
        badge = tv
    }

    private fun badgeText(): String {
        val word = if (reelCount == 1) "reel" else "reels"
        return "🎬 $reelCount $word this sitting"
    }

    private fun updateBadge() {
        badge?.text = badgeText()
    }

    private fun removeBadge() {
        badge?.let { windowManager?.removeView(it) }
        badge = null
    }

    // --- Question overlay ---

    private fun showQuiz() {
        if (quizOverlay != null) return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // solid, so the reel behind is fully hidden while answering
            setBackgroundColor(0xFF0B0B0B.toInt())
            setPadding(dpToPx(32), dpToPx(32), dpToPx(32), dpToPx(32))
        }

        val title = TextView(this).apply {
            text = "Quick check before the next one"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(0x99FFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        val questionTv = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(20), 0, dpToPx(28))
        }
        layout.addView(questionTv)
        questionView = questionTv

        val buttons = (0 until 3).map { index ->
            Button(this).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setOnClickListener { onAnswer(index) }
            }
        }
        buttons.forEach { button ->
            layout.addView(
                button,
                LinearLayout.LayoutParams(
                    dpToPx(220),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(10) }
            )
        }
        answerButtons = buttons

        val footer = TextView(this).apply {
            text = "You've watched $reelCount. Still worth your time?"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(0x80FFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(28), 0, 0)
        }
        layout.addView(footer)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0, // focusable and touchable so the buttons work and the reel is blocked
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.CENTER }

        loadQuestion(QuizBank.next())
        windowManager?.addView(layout, params)
        quizOverlay = layout
        muteMusic()
    }

    private fun muteMusic() {
        if (savedMusicVolume >= 0) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        savedMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
    }

    private fun restoreMusic() {
        if (savedMusicVolume < 0) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVolume, 0)
        savedMusicVolume = -1
    }

    private fun loadQuestion(q: QuizQuestion) {
        question = q
        questionView?.text = q.text
        answerButtons.forEachIndexed { index, button ->
            button.text = q.options[index]
        }
    }

    private fun onAnswer(index: Int) {
        val q = question ?: return
        if (index == q.answerIndex) {
            removeQuiz()
        } else {
            // wrong answer costs another question
            loadQuestion(QuizBank.next())
            questionView?.text = "Not quite. ${question?.text}"
        }
    }

    private fun removeQuiz() {
        quizOverlay?.let { windowManager?.removeView(it) }
        quizOverlay = null
        questionView = null
        answerButtons = emptyList()
        question = null
        restoreMusic()
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
}
