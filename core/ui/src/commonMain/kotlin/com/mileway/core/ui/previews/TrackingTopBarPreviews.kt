package com.mileway.core.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.TrackingStatus
import com.mileway.core.ui.components.topbar.TrackingStatusPill
import com.mileway.core.ui.components.topbar.TrackingTopBar
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

// ---------------------------------------------------------------------------
// Public previews for the minimal, matrix-themed tracking top bar (Task 1).
//
// These render the dark/surface bar (no pink gradient) with the embedded status
// pill in each state, plus the three pill states side by side. Used by the
// Roborazzi catalog to prove the off-brand header is gone and the active pill
// reads matrix-green.
// ---------------------------------------------------------------------------

@Composable
private fun TopBarInScheme(
    status: TrackingStatus,
    theme: MilewayThemeVariant = MilewayThemeVariant.MATRIX,
) {
    PreviewSurface(theme = theme) {
        TrackingTopBar(
            title = "Track Miles",
            status = status,
            actions = {
                if (status == TrackingStatus.TRACKING) {
                    Icon(
                        imageVector = Icons.Filled.CloudQueue,
                        contentDescription = "Sync queued",
                        tint = MilewayColors.warning,
                    )
                }
            },
        )
    }
}

@Preview(name = "Tracking top bar · Active (matrix)", showBackground = true, widthDp = 380, heightDp = 96)
@Composable
fun PreviewTrackingTopBarActive() = TopBarInScheme(TrackingStatus.TRACKING)

@Preview(name = "Tracking top bar · Paused", showBackground = true, widthDp = 380, heightDp = 96)
@Composable
fun PreviewTrackingTopBarPaused() = TopBarInScheme(TrackingStatus.PAUSED)

@Preview(name = "Tracking top bar · Idle", showBackground = true, widthDp = 380, heightDp = 96)
@Composable
fun PreviewTrackingTopBarIdle() = TopBarInScheme(TrackingStatus.IDLE)

@Preview(name = "Tracking status pills", showBackground = true, widthDp = 380, heightDp = 200)
@Composable
fun PreviewTrackingStatusPills() {
    PreviewSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrackingStatusPill(status = TrackingStatus.TRACKING)
            TrackingStatusPill(status = TrackingStatus.PAUSED)
            TrackingStatusPill(status = TrackingStatus.IDLE)
        }
    }
}

@Preview(name = "Tracking top bar · Active (daybreak)", showBackground = true, widthDp = 380, heightDp = 96)
@Composable
fun PreviewTrackingTopBarActiveDaybreak() = TopBarInScheme(TrackingStatus.TRACKING, MilewayThemeVariant.DAYBREAK)
