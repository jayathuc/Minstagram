package com.jayathu.minstagram.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
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
import com.jayathu.minstagram.domain.QuizCategory
import com.jayathu.minstagram.domain.QuizQuestion
import com.jayathu.minstagram.util.INSTAGRAM_PACKAGE

// Watches Instagram's Reels surface. Counts how many Reels go by and
// puts a small question between every few of them. The goal is to keep
// watching a conscious act, not to punish it.
//
// Counting is anchored to the reels pager view itself: only scroll events
// whose source is the pager count, and the pager's item index is used when
// available. Story trays, the feed, comments and tab swipes scroll other
// views, so they never count.
class ReelWatcherService : AccessibilityService() {

    companion object {
        private const val SCROLL_DEBOUNCE_MS = 700L
        private const val SURFACE_CHECK_THROTTLE_MS = 300L
        private const val BADGE_SHOW_MS = 3000L
        private const val OVERLAY_GUARD_INTERVAL_MS = 1000L

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
    private var lastReelIndex = -1
    private var reelCount = 0
    private var reelsSinceQuiz = 0

    private var badge: TextView? = null
    private var quizOverlay: View? = null
    private var questionView: TextView? = null
    private var answerButtons: List<Button> = emptyList()
    private var question: QuizQuestion? = null

    // media volume saved while a question is up, so the reel goes quiet
    private var savedMusicVolume = -1

    private val badgeHide = Runnable { removeBadge() }

    // Our event filter only covers Instagram, so we never hear about the user
    // going home or switching apps. While anything is on screen, poll the
    // focused window and tear down the moment it isn't Instagram.
    private val overlayGuard = object : Runnable {
        override fun run() {
            val pkg = rootInActiveWindow?.packageName?.toString()
            if (pkg != null && pkg != INSTAGRAM_PACKAGE && pkg != packageName) {
                leaveInstagram()
                return
            }
            if (badge != null || quizOverlay != null) {
                handler.postDelayed(this, OVERLAY_GUARD_INTERVAL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> updateSurface()
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(event)
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

    // --- Detection ---

    private fun handleScroll(event: AccessibilityEvent) {
        if (quizOverlay != null) return

        val sourceId = event.source?.viewIdResourceName
        if (sourceId != null) {
            // identified scroll: count only if it's the reels pager itself
            if (sourceId in REELS_PAGER_IDS) {
                markReelsSeen()
                onPagerScroll(event)
            }
            return
        }

        // unidentified scroll: accept only vertical ones while we're in reels
        if (recentlyInReels() && isVertical(event)) {
            debouncedCount()
        }
    }

    private fun onPagerScroll(event: AccessibilityEvent) {
        val index = event.fromIndex
        if (index >= 0) {
            // index change is one reel advancing, however it happened
            if (index != lastReelIndex) {
                lastReelIndex = index
                debouncedCount()
            }
        } else {
            debouncedCount()
        }
    }

    private fun isVertical(event: AccessibilityEvent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true
        return event.scrollDeltaY != 0 && event.scrollDeltaX == 0
    }

    private fun recentlyInReels(): Boolean =
        inReels && SystemClock.uptimeMillis() - lastReelsSeenMs < BADGE_SHOW_MS + 2000L

    private fun updateSurface() {
        val now = SystemClock.uptimeMillis()
        if (now - lastSurfaceCheckMs < SURFACE_CHECK_THROTTLE_MS) return
        lastSurfaceCheckMs = now

        val root = rootInActiveWindow ?: return
        val present = REELS_PAGER_IDS.any { id ->
            root.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
        }
        // an empty tree read is not proof we left, so only act on a real hit
        if (present) markReelsSeen()
    }

    private fun markReelsSeen() {
        val now = SystemClock.uptimeMillis()
        val gap = now - lastReelsSeenMs
        lastReelsSeenMs = now

        if (gap > NEW_SITTING_GAP_MS) {
            // been away a while, this is a fresh sitting and a reel is
            // already playing, so it counts as the first one
            reelCount = 1
            reelsSinceQuiz = 1
            lastReelIndex = -1
            inReels = true
            flashBadge()
        } else if (!inReels) {
            inReels = true
            flashBadge()
        }
    }

    // User left Instagram. Tear everything down, including any question.
    private fun leaveInstagram() {
        inReels = false
        handler.removeCallbacks(badgeHide)
        removeQuiz()
        removeBadge()
    }

    private fun debouncedCount() {
        val now = SystemClock.uptimeMillis()
        if (now - lastScrollMs < SCROLL_DEBOUNCE_MS) {
            lastScrollMs = now
            return
        }
        lastScrollMs = now

        reelCount++
        reelsSinceQuiz++
        bumpSessionReelCount()
        flashBadge()

        if (reelsSinceQuiz >= reelsPerQuestion()) {
            reelsSinceQuiz = 0
            showQuiz()
        }
    }

    private fun reelsPerQuestion(): Int =
        Prefs.get(this).getInt(Prefs.REELS_PER_QUESTION, Prefs.DEFAULT_REELS_PER_QUESTION)

    private fun quizCategory(): QuizCategory = runCatching {
        QuizCategory.valueOf(
            Prefs.get(this).getString(Prefs.QUIZ_CATEGORY, Prefs.DEFAULT_QUIZ_CATEGORY)!!
        )
    }.getOrDefault(QuizCategory.MIXED)

    private fun bumpSessionReelCount() {
        val prefs = Prefs.get(this)
        if (prefs.getBoolean(Prefs.SESSION_ACTIVE, false)) {
            prefs.edit()
                .putInt(Prefs.REELS_THIS_SESSION, prefs.getInt(Prefs.REELS_THIS_SESSION, 0) + 1)
                .apply()
        }
    }

    // --- Counter badge, appears briefly on each reel then fades ---

    private fun flashBadge() {
        showBadge()
        updateBadge()
        handler.removeCallbacks(badgeHide)
        handler.postDelayed(badgeHide, BADGE_SHOW_MS)
        startOverlayGuard()
    }

    private fun startOverlayGuard() {
        handler.removeCallbacks(overlayGuard)
        handler.postDelayed(overlayGuard, OVERLAY_GUARD_INTERVAL_MS)
    }

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
            // not focusable so Instagram keeps window focus and our guard can
            // see the real foreground app; touch still works for the buttons
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.CENTER }

        loadQuestion(QuizBank.next(quizCategory()))
        windowManager?.addView(layout, params)
        quizOverlay = layout
        muteMusic()
        startOverlayGuard()
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
            loadQuestion(QuizBank.next(quizCategory()))
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
