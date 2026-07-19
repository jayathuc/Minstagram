package com.jayathu.minstagram.presentation.intent

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.domain.unlockDelaySeconds
import com.jayathu.minstagram.util.formatHoursMinutes
import com.jayathu.minstagram.util.isReelWatcherEnabled
import kotlinx.coroutines.delay

// The pause before a session. Controls are visible but locked while a
// short countdown runs, so the impulse has a moment to pass. Leaving
// is always instant.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSheet(
    intention: SessionIntention,
    sessionsToday: Int,
    usageTodayMs: Long,
    intercepted: Boolean,
    snoozeMinutes: Int,
    onBegin: (SessionConfig) -> Unit,
    onSnooze: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }

    val totalDelay = remember {
        unlockDelaySeconds(
            prefs.getInt(Prefs.UNLOCK_DELAY_SECONDS, Prefs.DEFAULT_UNLOCK_DELAY_SECONDS),
            sessionsToday
        )
    }
    var remaining by remember { mutableIntStateOf(totalDelay) }
    val locked = remaining > 0

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    var timeLimit by remember { mutableIntStateOf(5) }
    var autoClose by remember {
        mutableStateOf(prefs.getBoolean(Prefs.AUTO_CLOSE, false))
    }

    // open fully so the Begin and leave buttons are visible without dragging
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${intention.emoji}  ${intention.label}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (usageTodayMs >= 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today so far: ${formatHoursMinutes(usageTodayMs)} on Instagram",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (locked) {
                // The decision moment. Keep it short so the way out is on
                // screen with no scrolling, and make leaving the one obvious
                // button. Session options appear once the timer ends.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { remaining / totalDelay.toFloat() },
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "$remaining",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "A moment to decide. No rush to stay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLeave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "I've got better things to do",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Begin unlocks when the timer ends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Back")
                }
                return@Column
            }

            if (intention == SessionIntention.WATCH_REELS && !isReelWatcherEnabled(context)) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text("Turn on reel questions in Accessibility settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            listOf(listOf(1, 5), listOf(10, 15)).forEach { rowMinutes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowMinutes.forEach { minutes ->
                        FilterChip(
                            selected = timeLimit == minutes,
                            onClick = { timeLimit = minutes },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            label = {
                                Text(
                                    text = if (minutes == 1) "1 minute" else "$minutes minutes",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto-close when time's up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = autoClose,
                    onCheckedChange = {
                        autoClose = it
                        prefs.edit().putBoolean(Prefs.AUTO_CLOSE, it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onBegin(SessionConfig(intention, timeLimit, intercepted)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Begin  ·  $timeLimit min",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Still one tap out, even after the wait.
            OutlinedButton(
                onClick = onLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("I've got better things to do")
            }

            if (intercepted) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onSnooze) {
                    Text("Open Instagram anyway, snooze $snoozeMinutes min")
                }
            }

            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    }
}
