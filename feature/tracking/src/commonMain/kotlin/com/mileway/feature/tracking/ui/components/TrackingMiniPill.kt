package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.platform.TrackingPresenceSnapshot
import kotlin.math.roundToLong

/**
 * P-D.2: shared CMP in-app mini-pill shown as a floating overlay while a tracking session is
 * active in the foreground (both Android and iOS). Surfaces distance · speed · activity at a
 * glance without requiring the user to look at the full hero gauge.
 *
 * Placement: the host screen wraps its content in a [Box] and anchors this pill to the top-center
 * using [Alignment.TopCenter]. The pill is visible regardless of which sheet/overlay is active,
 * so the user always knows the session is running.
 */
@Composable
fun TrackingMiniPill(
    snapshot: TrackingPresenceSnapshot,
    modifier: Modifier = Modifier,
) {
    val bgColor =
        if (snapshot.isPaused) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    val textColor =
        if (snapshot.isPaused) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    Row(
        modifier =
            modifier
                .background(color = bgColor, shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${snapshot.distanceKm.fmt2()} km",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
        if (!snapshot.isPaused) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "·",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${snapshot.speedKmh.roundToLong()} km/h",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "·",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = snapshot.activityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Paused",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}

private fun Double.fmt2(): String {
    val scaled = (this * 100).toLong()
    val i = scaled / 100
    val f = (scaled % 100).let { if (it < 0) -it else it }
    return "$i.${f.toString().padStart(2, '0')}"
}
