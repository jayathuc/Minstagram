package com.jayathu.minstagram.presentation.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayathu.minstagram.data.local.SessionEntity
import com.jayathu.minstagram.domain.model.EndReason
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.util.formatDuration
import com.jayathu.minstagram.util.formatHoursMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

private enum class HistoryRange(val days: Int, val label: String, val rangeText: String, val dayLabels: Boolean) {
    WEEK(7, "Week", "last 7 days", true),
    MONTH(30, "Month", "last 30 days", false)
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var range by remember { mutableStateOf(HistoryRange.WEEK) }

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val buckets = remember(sessions, range) { dailyBuckets(sessions, range.days, today, zone) }
    val summary = remember(buckets) { rangeSummary(buckets) }
    val sections = remember(sessions) { groupByDay(sessions, today, zone) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your history",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp
                )
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryRange.entries.forEach { r ->
                            FilterChip(
                                selected = range == r,
                                onClick = { range = r },
                                label = { Text(r.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    UsageCard(range = range, buckets = buckets, summary = summary)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (sections.isEmpty()) {
                    item {
                        Text(
                            text = "No sessions yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }

                sections.forEach { section ->
                    item(key = "h-${section.date}") {
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(section.sessions, key = { it.id }) { session ->
                        SessionRow(session, zone)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageCard(range: HistoryRange, buckets: List<DayBucket>, summary: RangeSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(formatHoursMinutes(summary.totalSeconds.toLong() * 1000L))
                    }
                    append("  in the ${range.rangeText}")
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${summary.sessionCount}") }
                    append(if (summary.sessionCount == 1) " session" else " sessions")
                    if (summary.activeDays > 0) {
                        append("  ·  avg ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(formatHoursMinutes(summary.avgPerActiveDaySeconds.toLong() * 1000L))
                        }
                        append(" a day")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            UsageBarChart(buckets = buckets, dayLabels = range.dayLabels)
        }
    }
}

@Composable
private fun UsageBarChart(buckets: List<DayBucket>, dayLabels: Boolean) {
    val barColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxSeconds = buckets.maxOfOrNull { it.totalSeconds } ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val n = buckets.size.coerceAtLeast(1)
            val slot = size.width / n
            val gap = 3.dp.toPx()
            val barW = (slot - gap).coerceAtLeast(1f)

            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
            if (maxSeconds <= 0) return@Canvas

            buckets.forEachIndexed { i, b ->
                if (b.totalSeconds <= 0) return@forEachIndexed
                val h = (b.totalSeconds / maxSeconds.toFloat()) * size.height
                val x = i * slot + (slot - barW) / 2f
                val r = min(4.dp.toPx(), h / 2f)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(r, r)
                )
            }
        }

        if (dayLabels) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                buckets.forEach { b ->
                    Text(
                        text = b.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun SessionRow(session: SessionEntity, zone: ZoneId) {
    val intention = runCatching { SessionIntention.valueOf(session.intention) }
        .getOrDefault(SessionIntention.JUST_BROWSING)
    val reason = runCatching { EndReason.valueOf(session.endReason) }
        .getOrDefault(EndReason.COMPLETED)
    val time = Instant.ofEpochMilli(session.startedAtMs).atZone(zone).format(timeFormat)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = intention.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = intention.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatDuration(session.actualSeconds)} of ${session.plannedSeconds / 60}m" +
                        "  ·  ${reason.label}" +
                        (if (session.reelsWatched > 0) "  ·  ${session.reelsWatched} reels" else "") +
                        (if (session.wasIntercepted) "  ·  caught" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
