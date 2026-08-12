package com.mileway.core.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.components.ErrorScreen
import com.mileway.core.ui.components.LoadingScreen
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.SectionLabel
import com.mileway.core.ui.components.StatCard
import com.mileway.core.ui.components.TwoButtonRow
import com.mileway.core.ui.components.tracking.CompactSystemStatusIndicator
import com.mileway.core.ui.components.tracking.ExpandableStatsCard
import com.mileway.core.ui.components.tracking.StatItem
import com.mileway.core.ui.components.tracking.StatusChip
import com.mileway.core.ui.components.tracking.StatusLevel
import com.mileway.core.ui.components.tracking.SystemStatusBanner
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.DefaultErrorState
import com.mileway.core.ui.mvi.PermissionRequiredState
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.siddharth.kmp.common.UiText

// ---------------------------------------------------------------------------
// CoreUiPreviews.kt: Phase 9.1 preview functions for :core:ui
//
// Rules:
// - SampleData is in this same package, no cross-module import needed.
// - @PreviewLightDark / @PreviewMatrix / PreviewSurface are all in this package.
// - No DI, no ViewModel, no Koin.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 1. Shared screen-state composables: Loading / Error / EmptyState
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun LoadingScreenPreview() {
    PreviewSurface {
        LoadingScreen(message = "Loading your journeys…")
    }
}

@PreviewLightDark
@Composable
private fun ErrorScreenPreview() {
    PreviewSurface {
        ErrorScreen(
            message = "Could not load data. Please check your connection.",
            onRetry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    PreviewSurface {
        EmptyState(
            title = "No journeys yet",
            subtitle = "Your completed trips will appear here.",
        )
    }
}

// ---------------------------------------------------------------------------
// 2. SectionCard, the core reusable labelled surface used throughout the app
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun SectionCardPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel(text = "Recent Activity")
            SectionCard(
                title = "Trip Summary",
                subtitle = "19 Jun 2026",
                leadingIcon = Icons.Filled.DirectionsCar,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        label = "Distance",
                        value = "${SampleData.Trip.distanceKm} km",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Reimbursable",
                        value = "₹${SampleData.Trip.reimbursableAmount}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. ExpandableStatsCard, tracking live-journey stats panel (collapsed state)
// ---------------------------------------------------------------------------

@PreviewMatrix
@Composable
private fun ExpandableStatsCardCollapsedPreview() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableStatsCard(
                stats =
                    listOf(
                        StatItem(label = "Distance", value = "${SampleData.Trip.distanceKm} km", icon = Icons.Filled.DirectionsCar),
                        StatItem(label = "Duration", value = "1 hr 0 min"),
                        StatItem(label = "Avg Speed", value = "12.4 km/h", icon = Icons.Filled.Speed),
                        StatItem(label = "Paused", value = "0 min"),
                    ),
                expanded = false,
                onToggle = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. CompactSystemStatusIndicator + SystemStatusBanner, tracking status row
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun SystemStatusRowPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CompactSystemStatusIndicator(
                chips =
                    listOf(
                        StatusChip(icon = Icons.Filled.GpsFixed, label = "GPS", level = StatusLevel.OK),
                        StatusChip(icon = Icons.Filled.NetworkCheck, label = "Network", level = StatusLevel.OK),
                        StatusChip(icon = Icons.Filled.Speed, label = "Accuracy", level = StatusLevel.WARN),
                    ),
            )
            SystemStatusBanner(
                allOk = true,
                message = "All systems nominal: ready to track.",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. TwoButtonRow, the paired primary/secondary action bar used on many screens
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun TwoButtonRowPreview() {
    PreviewSurface {
        TwoButtonRow(
            primaryText = "Submit",
            onPrimary = {},
            secondaryText = "Save Draft",
            onSecondary = {},
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 6. ScreenState switchboard: the shared Empty/Error/Offline/Permission/Partial
// states behind ScreenStateContent — the states every feature screen inherits.
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun ScreenStateEmptyWithCtaPreview() {
    PreviewSurface {
        Box(Modifier.height(320.dp)) {
            DefaultEmptyState(
                title = "No trips yet",
                subtitle = "Your tracked drives will show up here once you start your first trip.",
                ctaLabel = "Start tracking",
                onCta = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenStateErrorWithRetryPreview() {
    PreviewSurface {
        Box(Modifier.height(320.dp)) {
            DefaultErrorState(
                message = UiText.Dynamic("Couldn't load your trip history. The server didn't respond."),
                onRetry = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenStateOfflinePreview() {
    PreviewSurface {
        Box(Modifier.height(320.dp)) {
            ScreenStateContent(
                state = ScreenState.NoNetwork,
                onRetry = {},
            ) { Text("unreachable") }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenStatePermissionDeniedPreview() {
    PreviewSurface {
        Box(Modifier.height(320.dp)) {
            PermissionRequiredState(
                message = UiText.Dynamic("Location permission is needed to track your mileage."),
                isPermanentlyDenied = false,
                onRequestPermission = {},
                onOpenSettings = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenStatePermissionPermanentlyDeniedPreview() {
    PreviewSurface {
        Box(Modifier.height(320.dp)) {
            PermissionRequiredState(
                message = UiText.Dynamic("Location permission is needed to track your mileage."),
                isPermanentlyDenied = true,
                onRequestPermission = {},
                onOpenSettings = {},
            )
        }
    }
}

/** Loaded content, but one section failed to refresh — the PARTIAL state (not a full error). */
@PreviewLightDark
@Composable
private fun ScreenStatePartialErrorPreview() {
    PreviewSurface {
        Box(Modifier.height(220.dp)) {
            ScreenStateContent(
                state =
                    ScreenState.Content(
                        data = "3 trips loaded",
                        partialError = UiText.Dynamic("Couldn't refresh expense totals."),
                    ),
                onRetry = {},
            ) { data ->
                Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) { Text(data) }
            }
        }
    }
}

/** Cache shown on screen while a background refresh is in flight — distinct from initial loading. */
@PreviewLightDark
@Composable
private fun ScreenStateStaleRefreshPreview() {
    PreviewSurface {
        Box(Modifier.height(220.dp)) {
            ScreenStateContent(
                state = ScreenState.Content(data = "3 trips loaded (from cache)", isStale = true),
            ) { data ->
                Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) { Text(data) }
            }
        }
    }
}
