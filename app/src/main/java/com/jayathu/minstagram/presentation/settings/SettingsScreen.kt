package com.jayathu.minstagram.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.QuizTopic
import com.jayathu.minstagram.util.LaunchableApp
import com.jayathu.minstagram.util.exitTargetLabel
import com.jayathu.minstagram.util.installedLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOLD_MS = 5000L

// update once the policy is hosted publicly
private const val PRIVACY_URL = "https://github.com/jayathuc/minstagram/blob/main/PRIVACY.md"

// Asymmetric friction: tightening a protection applies on tap,
// loosening one needs a five second hold.
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShowSupport: () -> Unit = {}
) {
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
    var enabledTopics by remember {
        mutableStateOf(
            prefs.getStringSet(Prefs.QUIZ_TOPICS, null)?.toSet() ?: QuizTopic.DEFAULT_ENABLED
        )
    }
    var autoClose by remember {
        mutableStateOf(prefs.getBoolean(Prefs.AUTO_CLOSE, false))
    }
    var exitTargetPkg by remember {
        mutableStateOf(prefs.getString(Prefs.EXIT_TARGET_PACKAGE, null))
    }
    var showAppPicker by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Question topics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Turn kinds of question on or off. Keep at least one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            TopicToggles(
                enabled = enabledTopics,
                onToggle = { topic ->
                    val on = topic.name in enabledTopics
                    val next = if (on) enabledTopics - topic.name else enabledTopics + topic.name
                    if (next.isNotEmpty()) {
                        enabledTopics = next
                        prefs.edit().putStringSet(Prefs.QUIZ_TOPICS, next).apply()
                    }
                }
            )

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

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "When you leave",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Where \"I've got better things to do\" takes you. Your call, " +
                    "not ours. Home screen unless you pick an app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showAppPicker = true }
                ) {
                    Text(
                        text = exitTargetLabel(context, exitTargetPkg),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showAppPicker = true }) { Text("Change") }
                if (exitTargetPkg != null) {
                    TextButton(onClick = {
                        exitTargetPkg = null
                        prefs.edit().remove(Prefs.EXIT_TARGET_PACKAGE).apply()
                    }) { Text("Reset") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reel questions need the Minstagram accessibility service. " +
                    "Pick Watch Reels on the home screen to set it up.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Minstagram ${appVersion(context)}. Free. No ads, no tracking, " +
                    "nothing leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = onShowSupport) {
                    Text("Support the project")
                }
                TextButton(onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(PRIVACY_URL)
                        )
                    )
                }) {
                    Text("Privacy")
                }
            }
            TextButton(onClick = {
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "I use Minstagram to keep Instagram from eating my time. " +
                            "It asks why you're opening it, keeps a timer, and puts " +
                            "a question between Reels."
                    )
                }
                context.startActivity(
                    android.content.Intent.createChooser(share, "Share Minstagram")
                )
            }) {
                Text("Tell a friend")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAppPicker) {
        ExitAppPicker(
            onDismiss = { showAppPicker = false },
            onPick = { pkg ->
                exitTargetPkg = pkg
                prefs.edit().apply {
                    if (pkg == null) remove(Prefs.EXIT_TARGET_PACKAGE)
                    else putString(Prefs.EXIT_TARGET_PACKAGE, pkg)
                }.apply()
                showAppPicker = false
            }
        )
    }
}

// Simple list of installed apps to send the exit to, home screen first. No
// ranking or judgement, just the user's own pick.
@Composable
private fun ExitAppPicker(
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit
) {
    val context = LocalContext.current
    val apps by produceState<List<LaunchableApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { installedLaunchableApps(context) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Take me to…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    item { PickerRow("Home screen", onClick = { onPick(null) }) }
                    val list = apps
                    if (list == null) {
                        item {
                            Text(
                                text = "Loading apps…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                            )
                        }
                    } else {
                        items(list) { app ->
                            PickerRow(app.label, onClick = { onPick(app.packageName) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
}.getOrDefault("")

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicToggles(
    enabled: Set<String>,
    onToggle: (QuizTopic) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuizTopic.entries.forEach { topic ->
            ToggleChip(
                label = topic.label,
                selected = topic.name in enabled,
                onClick = { onToggle(topic) }
            )
        }
    }
}

// Plain on/off chip. Topics are a content choice, so both directions are a
// single tap, unlike the protection chips that need a hold to loosen.
@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
