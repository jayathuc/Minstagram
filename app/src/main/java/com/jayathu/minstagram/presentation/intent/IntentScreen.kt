package com.jayathu.minstagram.presentation.intent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.util.formatHoursMinutes

data class SessionConfig(
    val intention: SessionIntention,
    val timeLimitMinutes: Int,
    val intercepted: Boolean = false
)

@Composable
fun IntentScreen(
    onSessionStart: (SessionConfig) -> Unit,
    intercepted: Boolean = false,
    onSnooze: () -> Unit = {},
    onShowHistory: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    viewModel: IntentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sessionsToday by viewModel.sessionsToday.collectAsState(initial = 0)
    var sheetIntention by rememberSaveable { mutableStateOf<SessionIntention?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Minstagram",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                MirrorCard(
                    usageTodayMs = viewModel.usageTodayMs,
                    usageWeekMs = viewModel.usageWeekMs,
                    sessionsToday = sessionsToday
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = if (intercepted) "Hold on a second" else "What brings you here?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (intercepted) {
                        "You just opened Instagram. What do you need it for?"
                    } else {
                        "Pick a reason. That's the whole point."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                SessionIntention.entries.forEach { intention ->
                    IntentionCard(
                        intention = intention,
                        onClick = { sheetIntention = intention }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onShowHistory) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = onShowSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    sheetIntention?.let { intention ->
        val prefs = remember { Prefs.get(context) }
        SessionSheet(
            intention = intention,
            sessionsToday = sessionsToday,
            usageTodayMs = viewModel.usageTodayMs,
            intercepted = intercepted,
            snoozeMinutes = prefs.getInt(Prefs.SNOOZE_MINUTES, Prefs.DEFAULT_SNOOZE_MINUTES),
            onBegin = { config ->
                sheetIntention = null
                onSessionStart(config)
            },
            onSnooze = {
                sheetIntention = null
                onSnooze()
            },
            onDismiss = { sheetIntention = null }
        )
    }
}

@Composable
private fun MirrorCard(usageTodayMs: Long, usageWeekMs: Long, sessionsToday: Int) {
    if (usageTodayMs < 0) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Today ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(formatHoursMinutes(usageTodayMs))
                    }
                    append("   ·   7 days ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(formatHoursMinutes(usageWeekMs))
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (sessionsToday > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Session ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${sessionsToday + 1}")
                        }
                        append(" today")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IntentionCard(
    intention: SessionIntention,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = intention.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = intention.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = intention.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
