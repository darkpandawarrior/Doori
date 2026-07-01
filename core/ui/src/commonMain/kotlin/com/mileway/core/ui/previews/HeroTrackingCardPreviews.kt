package com.mileway.core.ui.previews

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.tracking.ActivitySegment
import com.mileway.core.ui.components.tracking.ActivityType
import com.mileway.core.ui.components.tracking.GaugeMode
import com.mileway.core.ui.components.tracking.GaugeSignal
import com.mileway.core.ui.components.tracking.HeroTrackingCard
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

// ---------------------------------------------------------------------------
// Public previews for the matrix-re-tokened HeroTrackingCard (Task 3).
//
// Active: accent-warmed gradient + glowing breathing border (glow-on-active).
// Idle:   calm raised surface + outline. Renders in each curated scheme so the
// accent-derived gradient is verifiably coherent (Matrix green vs Ion blue).
// ---------------------------------------------------------------------------

@Composable
private fun HeroInScheme(
    isActive: Boolean,
    theme: MilewayThemeVariant = MilewayThemeVariant.MATRIX,
) {
    PreviewSurface(theme = theme) {
        HeroTrackingCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            distanceText = if (isActive) "4.20 km" else "0.00 km",
            durationText = if (isActive) "00:12:30" else "00:00",
            vehicleName = "Four Wheeler (Petrol)",
            bearingDegrees = 45f,
            speedKmh = if (isActive) 38f else null,
            signalQuality = GaugeSignal.GOOD,
            segments =
                if (isActive) {
                    listOf(ActivitySegment(ActivityType.DRIVING, 1f))
                } else {
                    emptyList()
                },
            gaugeMode = GaugeMode.COMPASS,
            onToggleMode = {},
            isActive = isActive,
            historyCount = if (isActive) 18 else 0,
            trackingActivity = if (isActive) "Driving" else "",
        )
    }
}

@Preview(name = "Hero card · Active (matrix)", showBackground = true, widthDp = 380, heightDp = 360)
@Composable
fun PreviewHeroTrackingCardActive() = HeroInScheme(isActive = true)

@Preview(name = "Hero card · Idle (matrix)", showBackground = true, widthDp = 380, heightDp = 360)
@Composable
fun PreviewHeroTrackingCardIdle() = HeroInScheme(isActive = false)

@Preview(name = "Hero card · Active (ion)", showBackground = true, widthDp = 380, heightDp = 360)
@Composable
fun PreviewHeroTrackingCardActiveIon() = HeroInScheme(isActive = true, theme = MilewayThemeVariant.ION)
