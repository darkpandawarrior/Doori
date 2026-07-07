package com.mileway.feature.tracking.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_badge_active
import com.mileway.core.ui.resources.tracking_badge_paused
import com.mileway.core.ui.resources.tracking_live_active
import com.mileway.core.ui.resources.tracking_live_all_synced
import com.mileway.core.ui.resources.tracking_live_battery
import com.mileway.core.ui.resources.tracking_live_gps
import com.mileway.core.ui.resources.tracking_live_ok
import com.mileway.core.ui.resources.tracking_live_overview_title
import com.mileway.core.ui.resources.tracking_live_pending_sync
import com.mileway.core.ui.resources.tracking_live_points
import com.mileway.core.ui.resources.tracking_live_recent_events
import com.mileway.core.ui.resources.tracking_live_sync_status
import com.mileway.core.ui.resources.tracking_live_system_health
import com.mileway.core.ui.resources.tracking_stat_distance
import com.mileway.core.ui.resources.tracking_stat_duration
import com.mileway.core.ui.resources.tracking_stat_speed
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun LiveTrackingOverviewCard(
    trackData: CurrentTrackData,
    modifier: Modifier = Modifier,
) {
    val glassGradient =
        Brush.verticalGradient(
            listOf(Color(0xFF0D2137).copy(alpha = 0.92f), Color(0xFF1A237E).copy(alpha = 0.88f)),
        )
    val glassBorder =
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.06f)),
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .border(width = 1.dp, brush = glassBorder, shape = RoundedCornerShape(16.dp))
                .background(glassGradient, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(Res.string.tracking_live_overview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                LiveIndicatorBadge(isPaused = trackData.isPaused)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LiveMetric(
                    label = stringResource(Res.string.tracking_stat_distance),
                    value = "${(trackData.distance / 1000.0).formatDecimal(2)} km",
                    icon = Icons.Default.GpsFixed,
                    color = Color(0xFF80DEEA),
                )
                LiveMetric(
                    label = stringResource(Res.string.tracking_stat_duration),
                    value = formatDuration(Clock.System.now().toEpochMilliseconds() - trackData.startTime),
                    icon = Icons.Default.Timer,
                    color = Color(0xFFB39DDB),
                )
                LiveMetric(
                    label = stringResource(Res.string.tracking_stat_speed),
                    value = "${(trackData.speed * 3.6).formatDecimal(1)} km/h",
                    icon = Icons.Default.Speed,
                    color = Color(0xFF69F0AE),
                )
            }
        }
    }
}

@Composable
fun LiveHealthMonitorCard(
    locationCount: Int,
    unsyncedCount: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.tracking_live_system_health), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                HealthIndicator(
                    label = stringResource(Res.string.tracking_live_gps),
                    value = stringResource(Res.string.tracking_live_active),
                    color = MilewayColors.success,
                    icon = Icons.Default.GpsFixed,
                )
                HealthIndicator(
                    label = stringResource(Res.string.tracking_live_points),
                    value = "$locationCount",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.NetworkCheck,
                )
                HealthIndicator(
                    label = stringResource(Res.string.tracking_live_battery),
                    value = stringResource(Res.string.tracking_live_ok),
                    color = MilewayColors.success,
                    icon = Icons.Default.Battery5Bar,
                )
            }
        }
    }
}

@Composable
fun LiveSyncStatusCard(
    total: Long,
    unsynced: Long,
    modifier: Modifier = Modifier,
) {
    val synced = (total - unsynced).coerceAtLeast(0)
    val progress = if (total > 0) synced.toFloat() / total else 1f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.tracking_live_sync_status), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("$synced/$total", style = MaterialTheme.typography.bodySmall.dataStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                strokeCap = StrokeCap.Round,
                color = if (unsynced == 0L) MilewayColors.success else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (unsynced == 0L) stringResource(Res.string.tracking_live_all_synced) else stringResource(Res.string.tracking_live_pending_sync, unsynced),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RecentEventsCard(
    events: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.tracking_live_recent_events), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            events.takeLast(3).reversed().forEach { (text, time) ->
                RecentEventItem(text, time)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LiveMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.dataStyle(), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthIndicator(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentEventItem(
    text: String,
    time: Long,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(timeAgo(time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LiveIndicatorBadge(isPaused: Boolean) {
    val infinite = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_alpha",
    )
    StatusBadge(
        text = if (isPaused) stringResource(Res.string.tracking_badge_paused) else stringResource(Res.string.tracking_badge_active),
        color = if (isPaused) MilewayColors.warning else MilewayColors.success,
        modifier = if (isPaused) Modifier else Modifier.alpha(alpha),
    )
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0m"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

private fun timeAgo(timestampMs: Long): String {
    val diffSec = (Clock.System.now().toEpochMilliseconds() - timestampMs) / 1000
    return when {
        diffSec < 60 -> "${diffSec}s ago"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        else -> "${diffSec / 3600}h ago"
    }
}
