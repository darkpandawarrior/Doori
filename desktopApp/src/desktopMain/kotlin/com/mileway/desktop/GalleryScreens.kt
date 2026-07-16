package com.mileway.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.ui.components.GridProfileTile
import com.mileway.core.ui.components.ProfileCompletionBanner
import com.mileway.core.ui.components.ProfileSectionHeader
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.expense.ExpenseContextSummaryCard
import com.mileway.core.ui.components.scaffold.DetailSection
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.core.ui.components.scaffold.TransactionDetailScaffold
import com.mileway.core.ui.components.timeline.TransactionTimeline
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.components.topbar.TrackingStatus
import com.mileway.core.ui.components.topbar.TrackingTopBar
import com.mileway.core.ui.components.tracking.CompactSystemStatusIndicator
import com.mileway.core.ui.components.tracking.ExpandableStatsCard
import com.mileway.core.ui.components.tracking.GaugeMode
import com.mileway.core.ui.components.tracking.GaugeSignal
import com.mileway.core.ui.components.tracking.HeroTrackingCard
import com.mileway.core.ui.components.tracking.SystemStatusBanner
import com.mileway.core.ui.components.tracking.ThreeButtonFabSystem
import com.mileway.core.ui.mvi.asContent
import com.mileway.core.ui.theme.DesignTokens

/**
 * showcase/T.2: curated desktop screenshot gallery — hand-rolled screen shells over shared
 * `core:ui` widgets, mirroring [DashboardScreenForScreenshot]'s pattern in `Main.kt`. `desktopApp`
 * has no `feature:*` dependency (Option b, see D.2's kdoc + CLAUDE.md module boundaries), so every
 * screen here is composed directly from `core:ui` components + `core:data` mock models rather than
 * a real feature screen. Internal (not private): shared with `DesktopScreenshotGalleryTest`.
 */

@Composable
internal fun LiveTrackingScreenForScreenshot() {
    Scaffold(
        topBar = { TrackingTopBar(title = "Tracking", status = TrackingStatus.TRACKING) },
        bottomBar = {
            ThreeButtonFabSystem(
                isActive = true,
                isPaused = false,
                actions = mockQuickActions(),
                onHero = {},
                onPauseResume = {},
                onAction = {},
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(scaffoldPadding).padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            HeroTrackingCard(
                distanceText = "18.4 km",
                durationText = "00:42:10",
                vehicleName = "4 Wheeler",
                bearingDegrees = 128f,
                speedKmh = 38f,
                signalQuality = GaugeSignal.GOOD,
                segments = mockActivitySegments(),
                gaugeMode = GaugeMode.COMPASS,
                onToggleMode = {},
                isActive = true,
                historyCount = 12,
                trackingActivity = "Driving",
            )
            ExpandableStatsCard(stats = mockJourneyStats(), expanded = true, onToggle = {})
            CompactSystemStatusIndicator(chips = mockStatusChips())
            SystemStatusBanner(allOk = true, message = "All systems OK")
        }
    }
}

@Composable
internal fun TripHistoryScreenForScreenshot(trips: List<TrackDisplayData>) {
    HistoryListScaffold(
        title = "Trip History",
        onBack = {},
        state = trips.asContent(),
        onRetry = {},
        tabs = listOf("All", "Submitted", "Pending"),
        itemKey = { it.token },
        itemContent = { trip ->
            SectionCard(title = trip.name.orEmpty(), subtitle = trip.getFormattedDuration()) {
                Text(trip.getFormattedDistance(), style = MaterialTheme.typography.bodyLarge)
            }
        },
    )
}

@Composable
internal fun TripDetailScreenForScreenshot() {
    TransactionDetailScaffold(
        title = "Client site visit",
        subtitle = "18.4 km · 42m",
        tabs = listOf(DetailSection.Details, DetailSection.Timeline),
        selectedTab = DetailSection.Timeline,
        onSelectTab = {},
        onBack = {},
    ) { section ->
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            when (section) {
                DetailSection.Timeline -> TransactionTimeline(steps = mockTripTimeline(), title = "Status")
                else -> SectionCard(title = "Details") { Text("18.4 km · 4 Wheeler · ₹276 reimbursable") }
            }
        }
    }
}

@Composable
internal fun LogExpenseScreenForScreenshot() {
    FormSubmissionScaffold(
        title = "Log Expense",
        subtitle = "Linked to a trip",
        onBack = {},
        onSubmit = {},
        onSaveDraft = {},
    ) { formPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(formPadding).padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            ExpenseContextSummaryCard(context = mockExpenseContext())
            SectionCard(title = "Amount") { Text("₹1,240.00", style = MaterialTheme.typography.headlineSmall) }
            SectionCard(title = "Category") { Text("Fuel", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
internal fun ApprovalsScreenForScreenshot() {
    HistoryListScaffold(
        title = "Approvals",
        onBack = {},
        state = mockApprovals().asContent(),
        onRetry = {},
        tabs = listOf("Pending", "Approved", "Rejected"),
        itemKey = { it.id },
        itemContent = { approval ->
            SectionCard(title = approval.title, subtitle = approval.subtitle) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(approval.amount, style = MaterialTheme.typography.bodyLarge)
                    StatusChip(label = "Pending", tone = StatusTone.Warning)
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileScreenForScreenshot() {
    Scaffold(
        topBar = { DepthAwareTopBar(title = "Profile", depth = DesignTokens.NavigationDepth.ROOT) },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(scaffoldPadding).padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            ProfileCompletionBanner(
                completionPercentage = 75,
                completedCount = 6,
                totalCount = 8,
                missingItems = emptyList(),
                categories = emptyList(),
            )
            ProfileSectionHeader(title = "Account")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                mockProfileItems().forEach { item ->
                    GridProfileTile(item = item, modifier = Modifier.width(150.dp))
                }
            }
        }
    }
}
