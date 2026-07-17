package com.jayathu.minstagram.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.util.formatDuration

@Composable
fun SessionSummaryScreen(
    intention: SessionIntention,
    durationSeconds: Int,
    onDone: () -> Unit,
    onShowHistory: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val latest by viewModel.latest.collectAsState(initial = null)
    val todayCount by viewModel.todayCount.collectAsState(initial = 0)
    val weekSeconds by viewModel.weekSeconds.collectAsState(initial = 0)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Session Complete",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = intention.emoji,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = intention.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = latest?.let {
                    val reels = if (it.reelsWatched > 0) "  ·  ${it.reelsWatched} reels" else ""
                    "You used ${formatDuration(it.actualSeconds)} of your ${it.plannedSeconds / 60}m plan$reels"
                } ?: "Time spent: ${formatDuration(durationSeconds)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (todayCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Session $todayCount today  ·  ${formatDuration(weekSeconds)} this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onDone) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(onClick = onShowHistory) {
                    Text("View history")
                }
            }
        }
    }
}
