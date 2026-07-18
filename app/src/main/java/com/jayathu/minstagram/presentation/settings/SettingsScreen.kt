package com.jayathu.minstagram.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jayathu.minstagram.data.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_MS = 5000L

// Asymmetric friction: tightening a protection applies on tap,
// loosening one needs a five second hold.
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }

    var unlockDelay by remember {
        mutableIntStateOf(prefs.getInt(Prefs.UNLOCK_DELAY_SECONDS, Prefs.DEFAULT_UNLOCK_DELAY_SECONDS))
    }
    var snoozeMinutes by remember {
        mutableIntStateOf(prefs.getInt(Prefs.SNOOZE_MINUTES, Prefs.DEFAULT_SNOOZE_MINUTES))
    }
    var reelsPerQuestion by remember {
        mutableIntStateOf(prefs.getInt(Prefs.REELS_PER_QUESTION, Prefs.DEFAULT_REELS_PER_QUESTION))
    }
    var category by remember {
        mutableStateOf(prefs.getString(Prefs.QUIZ_CATEGORY, Prefs.DEFAULT_QUIZ_CATEGORY)!!)
    }
    var autoClose by remember {
        mutableStateOf(prefs.getBoolean(Prefs.AUTO_CLOSE, false))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Tightening a protection is instant. Loosening one takes a five second hold.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingSection(
                title = "Pause before a session",
                subtitle = "Grows a little with every session today."
            ) {
                listOf(3, 5, 10, 15).forEach { seconds ->
                    FrictionChip(
                        label = "${seconds}s",
                        selected = unlockDelay == seconds,
                        loosening = seconds < unlockDelay,
                        onCommit = {
                            unlockDelay = seconds
                            prefs.edit().putInt(Prefs.UNLOCK_DELAY_SECONDS, seconds).apply()
                        }
                    )
                }
            }

            SettingSection(title = "Snooze length", subtitle = "How long the gate stays out of the way.") {
                listOf(15, 30, 60).forEach { minutes ->
                    FrictionChip(
                        label = "${minutes}m",
                        selected = snoozeMinutes == minutes,
                        loosening = minutes > snoozeMinutes,
                        onCommit = {
                            snoozeMinutes = minutes
                            prefs.edit().putInt(Prefs.SNOOZE_MINUTES, minutes).apply()
                        }
                    )
                }
            }

            SettingSection(title = "Question every how many Reels?", subtitle = null) {
                listOf(3, 4, 5).forEach { n ->
                    FrictionChip(
                        label = "$n",
                        selected = reelsPerQuestion == n,
                        loosening = n > reelsPerQuestion,
                        onCommit = {
                            reelsPerQuestion = n
                            prefs.edit().putInt(Prefs.REELS_PER_QUESTION, n).apply()
                        }
                    )
                }
            }

            SettingSection(title = "Question type", subtitle = null) {
                listOf(
                    "MATH" to "Math",
                    "GENERAL" to "General",
                    "MIXED" to "Mixed"
                ).forEach { (value, label) ->
                    FrictionChip(
                        label = label,
                        selected = category == value,
                        loosening = false,
                        onCommit = {
                            category = value
                            prefs.edit().putString(Prefs.QUIZ_CATEGORY, value).apply()
                        }
                    )
                }
            }

            SettingSection(
                title = "Auto-close when time's up",
                subtitle = "Leaves Instagram for you when the timer ends."
            ) {
                FrictionChip(
                    label = "On",
                    selected = autoClose,
                    loosening = false,
                    onCommit = {
                        autoClose = true
                        prefs.edit().putBoolean(Prefs.AUTO_CLOSE, true).apply()
                    }
                )
                FrictionChip(
                    label = "Off",
                    selected = !autoClose,
                    loosening = true,
                    onCommit = {
                        autoClose = false
                        prefs.edit().putBoolean(Prefs.AUTO_CLOSE, false).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reel questions need the Minstagram accessibility service. " +
                    "Pick Watch Reels on the home screen to set it up.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit
) {
    Spacer(modifier = Modifier.height(28.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    if (subtitle != null) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        content()
    }
}

// Tap to tighten. Hold to loosen, with a fill showing the hold progress.
@Composable
private fun FrictionChip(
    label: String,
    selected: Boolean,
    loosening: Boolean,
    onCommit: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(10.dp)

    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val interaction = when {
        selected -> Modifier
        !loosening -> Modifier.clickable { onCommit() }
        else -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    val job = scope.launch {
                        val steps = 100
                        for (i in 1..steps) {
                            delay(HOLD_MS / steps)
                            progress = i / steps.toFloat()
                        }
                        onCommit()
                        progress = 0f
                    }
                    tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                }
            )
        }
    }

    Surface(
        shape = shape,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = interaction
    ) {
        Box {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    )
                }
            }
            Text(
                text = if (progress > 0f) "hold…" else label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}
