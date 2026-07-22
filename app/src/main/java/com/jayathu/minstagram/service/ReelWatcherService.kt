package com.jayathu.minstagram.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.QuizBank
import com.jayathu.minstagram.domain.QuizQuestion
import com.jayathu.minstagram.domain.QuizTopic
import com.jayathu.minstagram.util.INSTAGRAM_PACKAGE
import com.jayathu.minstagram.util.leaveToChosenApp

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

        // Wrong answers get a growing pause before the buttons work again,
        // so blindly spamming one option can't brute force past a question.
        private const val WRONG_COOLDOWN_STEP_MS = 1500L
        private const val WRONG_COOLDOWN_MAX_MS = 6000L

        // Instagram's reel pager view ids, may need updates over time
        private val REELS_PAGER_IDS = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_swipe_refresh_container"
        )

        // System UI (the notification shade, quick settings) briefly becomes
        // the active window when pulled down. That is not leaving Instagram,
        // so the overlay guard must not treat it as an app switch.
        private val TRANSIENT_PACKAGES = setOf("com.android.systemui", "android")

        @Volatile
        private var instance: ReelWatcherService? = null

        // Called by SessionService when the timer ends. A question may be up
        // covering the whole screen, so drop it before the summary or the
        // time's-up screen tries to show underneath it.
        fun dismissActiveQuiz() {
            val svc = instance ?: return
            svc.handler.post { svc.leaveInstagram() }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null

    private var inReels = false
    private var lastSurfaceCheckMs = 0L
    private var lastReelsSeenMs = 0L
    private var lastScrollMs = 0L
    // furthest reel position reached this sitting; only going past it counts,
    // so scrolling back and forth over the same reels does not inflate
    private var maxReelIndex = -1
    private var reelCount = 0
    private var reelsSinceQuiz = 0

    private var badge: TextView? = null
    private var quizOverlay: View? = null
    private var questionView: TextView? = null
    private var titleView: TextView? = null
    private var footerView: TextView? = null
    private var leaveButtonView: Button? = null
    private var answerButtons: List<Button> = emptyList()
    private var question: QuizQuestion? = null

    // true while a wrong-answer cooldown is running and the buttons are dead
    private var buttonsLocked = false
    private var wrongStreak = 0

    // media volume saved while a question is up, so the reel goes quiet
    private var savedMusicVolume = -1

    private val badgeHide = Runnable { removeBadge() }
    private val cooldownEnd = Runnable { endCooldown() }
    private val confirmDismiss = Runnable { removeQuiz() }

    // Our event filter only covers Instagram, so we never hear about the user
    // going home or switching apps. While anything is on screen, poll the
    // focused window and tear down the moment it isn't Instagram.
    private val overlayGuard = object : Runnable {
        override fun run() {
            val pkg = rootInActiveWindow?.packageName?.toString()
            if (pkg != null &&
                pkg != INSTAGRAM_PACKAGE &&
                pkg != packageName &&
                pkg !in TRANSIENT_PACKAGES
            ) {
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
        instance = this
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
        if (instance === this) instance = null
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
        if (index < 0) {
            // no position info, fall back to counting debounced swipes
            debouncedCount()
            return
        }
        // count only new ground; revisiting earlier reels does not
        if (index > maxReelIndex) {
            maxReelIndex = index
            registerReel()
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
            maxReelIndex = -1
            inReels = true
            flashBadge()
        } else if (!inReels) {
            inReels = true
            flashBadge()
        }
    }

    // Ads carry a "Sponsored" label. They aren't reels the user chose to watch,
    // so we don't count them toward the sitting or the next question. Best
    // effort and English only: we check node text and, since some ads only
    // label it on an image, content descriptions too.
    private fun currentReelIsAd(): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.findAccessibilityNodeInfosByText("Sponsored").isNotEmpty()) return true
        return mentionsSponsored(root, 0)
    }

    private fun mentionsSponsored(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 24) return false
        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains("Sponsored", ignoreCase = true)) return true
        for (i in 0 until node.childCount) {
            if (mentionsSponsored(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    // A reel playing audio is a video, where a center tap is Instagram's own
    // play/pause. With no audio it's likely a still image or slideshow, where
    // that same tap counts as a click and can open an advertiser's page. So we
    // only tap when audio is actually playing.
    private fun isVideoReelPlaying(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.isMusicActive
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
        registerReel()
    }

    private fun registerReel() {
        if (currentReelIsAd()) return

        reelCount++
        reelsSinceQuiz++
        bumpSessionReelCount()
        flashBadge()

        if (reelsSinceQuiz >= reelsPerQuestion()) {
            reelsSinceQuiz = 0
            pauseThenShowQuiz()
        }
    }

    private fun reelsPerQuestion(): Int =
        Prefs.get(this).getInt(Prefs.REELS_PER_QUESTION, Prefs.DEFAULT_REELS_PER_QUESTION)

    // topics the user left enabled in settings; unset or empty means all of them
    private fun enabledTopics(): Set<QuizTopic> {
        val names = Prefs.get(this).getStringSet(Prefs.QUIZ_TOPICS, null)
            ?: return QuizTopic.entries.toSet()
        return names
            .mapNotNull { name -> runCatching { QuizTopic.valueOf(name) }.getOrNull() }
            .toSet()
            .ifEmpty { QuizTopic.entries.toSet() }
    }

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
        // never let the badge surface over a question
        if (quizOverlay != null) return
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

    // Pause the reel before covering it, so it isn't quietly playing on behind
    // the question and past its start when the user comes back. A single tap in
    // the middle of the screen is Instagram's own play/pause toggle. The tap has
    // to land before the opaque overlay goes up, so it's shown in the callback.
    private fun pauseThenShowQuiz() {
        if (quizOverlay != null) return
        // don't tap unless it's a real video reel, or the tap could click
        // through a still image or an ad. Just put the question over it instead.
        if (currentReelIsAd() || !isVideoReelPlaying()) {
            showQuiz()
            return
        }
        val gesture = centerTapGesture()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) {
                showQuiz()
            }

            override fun onCancelled(g: GestureDescription?) {
                showQuiz()
            }
        }, handler)
        if (!dispatched) showQuiz()
    }

    private fun centerTapGesture(): GestureDescription {
        val metrics = resources.displayMetrics
        // dead center: Instagram's play/pause zone. Higher up sits the mute
        // control, so tapping there flips sound instead of pausing.
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f
        val path = Path().apply { moveTo(x, y) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 40L))
            .build()
    }

    private fun showQuiz() {
        if (quizOverlay != null) return
        wrongStreak = 0
        buttonsLocked = false

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // solid, so the paused reel behind is fully hidden while answering
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
        titleView = title

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
        footerView = footer

        // the easy way out, made the most inviting thing on the screen. Leaving
        // is the win here, so it gets the filled, warm treatment.
        val leaveButton = Button(this).apply {
            text = "I've got better things to do  →"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(0xFF07130F.toInt())
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(26).toFloat()
                setColor(0xFF00C9A7.toInt())
            }
            stateListAnimator = null
            setPadding(dpToPx(28), dpToPx(15), dpToPx(28), dpToPx(15))
            setOnClickListener { leaveApp() }
        }
        layout.addView(
            leaveButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(28) }
        )
        leaveButtonView = leaveButton

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // not focusable so Instagram keeps window focus and our guard can
            // see the real foreground app; touch still works for the buttons
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.CENTER }

        loadQuestion(QuizBank.next(enabledTopics()))
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
        titleView?.text = "Quick check before the next one"
        questionView?.text = q.text
        answerButtons.forEachIndexed { index, button ->
            button.text = q.options[index]
        }
    }

    private fun onAnswer(index: Int) {
        if (buttonsLocked) return
        val q = question ?: return
        if (index == q.answerIndex) {
            wrongStreak = 0
            showCorrectThenLinger(q)
        } else {
            wrongStreak++
            startCooldown()
        }
    }

    // Correct answer: don't snap straight back to the reel. Hold a calm
    // confirmation for a few seconds, an interesting fact where we have one,
    // otherwise a short well-done. Then take the overlay down. The reel stays
    // paused, so the user resumes it themselves when they're ready.
    private fun showCorrectThenLinger(q: QuizQuestion) {
        buttonsLocked = true
        handler.removeCallbacks(cooldownEnd)
        answerButtons.forEach { it.visibility = View.GONE }
        footerView?.visibility = View.GONE
        leaveButtonView?.visibility = View.GONE

        val fact = q.fact
        if (fact != null) {
            titleView?.visibility = View.VISIBLE
            titleView?.text = "✓  Correct"
            titleView?.setTextColor(0xFF00C9A7.toInt())
            titleView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            questionView?.text = fact
            questionView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            questionView?.setTextColor(0xFFEDEDED.toInt())
        } else {
            titleView?.visibility = View.GONE
            questionView?.text = QuizBank.affirmation()
            questionView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            questionView?.setTextColor(0xFF00C9A7.toInt())
        }
        handler.postDelayed(confirmDismiss, if (fact != null) 4000L else 2500L)
    }

    // A wrong answer locks the buttons for a moment that grows each time, then
    // brings up a fresh question. Reading and picking beats spamming a guess.
    private fun startCooldown() {
        buttonsLocked = true
        answerButtons.forEach { it.isEnabled = false }
        val waitMs = minOf(WRONG_COOLDOWN_STEP_MS * wrongStreak, WRONG_COOLDOWN_MAX_MS)
        titleView?.text = "Not quite. Look again in ${(waitMs / 1000L)}s"
        handler.removeCallbacks(cooldownEnd)
        handler.postDelayed(cooldownEnd, waitMs)
    }

    private fun endCooldown() {
        if (quizOverlay == null) return
        buttonsLocked = false
        answerButtons.forEach { it.isEnabled = true }
        loadQuestion(QuizBank.next(enabledTopics()))
    }

    // Bail out to wherever the user chose to go (home screen by default). The
    // session's own timeout ends things once we're no longer in Instagram.
    private fun leaveApp() {
        removeQuiz()
        leaveToChosenApp(this)
    }

    private fun removeQuiz() {
        handler.removeCallbacks(cooldownEnd)
        handler.removeCallbacks(confirmDismiss)
        buttonsLocked = false
        wrongStreak = 0
        quizOverlay?.let { windowManager?.removeView(it) }
        quizOverlay = null
        questionView = null
        titleView = null
        footerView = null
        leaveButtonView = null
        answerButtons = emptyList()
        question = null
        restoreMusic()
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
}
