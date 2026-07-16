package com.mileway.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.components.ProfileGridItem
import com.mileway.core.ui.components.ProfileItemStatus
import com.mileway.core.ui.components.timeline.TimelineStep
import com.mileway.core.ui.components.tracking.ActivitySegment
import com.mileway.core.ui.components.tracking.ActivityType
import com.mileway.core.ui.components.tracking.QuickAction
import com.mileway.core.ui.components.tracking.StatItem
import com.mileway.core.ui.components.tracking.StatusChip
import com.mileway.core.ui.components.tracking.StatusLevel
import com.mileway.core.ui.theme.DesignTokens

/**
 * showcase/T.2: hand-rolled mock data for the curated desktop screenshot gallery — same
 * constraint as [DashboardMockData.kt] (thin `desktopApp`, no `feature:*`/Room-repository
 * dependency, see that file's kdoc), just covering the extra `core:ui` widgets each gallery
 * screen showcases instead of the dashboard's trip list.
 */

fun mockQuickActions(): List<QuickAction> =
    listOf(
        QuickAction(id = "photo", label = "Odometer photo", icon = Icons.Filled.CameraAlt),
        QuickAction(id = "note", label = "Add note", icon = Icons.AutoMirrored.Filled.Notes),
        QuickAction(id = "discard", label = "Discard", icon = Icons.Filled.Close, destructive = true),
    )

fun mockActivitySegments(): List<ActivitySegment> =
    listOf(
        ActivitySegment(ActivityType.DRIVING, 0.62f),
        ActivitySegment(ActivityType.IDLE, 0.23f),
        ActivitySegment(ActivityType.WALKING, 0.15f),
    )

fun mockJourneyStats(): List<StatItem> =
    listOf(
        StatItem("Distance", "18.4 km"),
        StatItem("Duration", "42m"),
        StatItem("Avg speed", "38 km/h"),
        StatItem("Paused", "2m"),
    )

fun mockStatusChips(): List<StatusChip> =
    listOf(
        StatusChip(Icons.Filled.GpsFixed, "GPS", StatusLevel.OK),
        StatusChip(Icons.Filled.BatteryFull, "82%", StatusLevel.OK),
        StatusChip(Icons.Filled.SignalCellularAlt, "Weak signal", StatusLevel.WARN),
    )

fun mockExpenseContext(): ExpenseSourceContext = ExpenseSourceContext.Trip(tripId = "d1", tripLabel = "Client site visit")

fun mockTripTimeline(): List<TimelineStep> =
    listOf(
        TimelineStep("Submitted", Icons.Filled.Receipt, DesignTokens.StatusColors.info, active = true, note = "Auto-generated on trip end"),
        TimelineStep("Under review", Icons.Filled.Schedule, DesignTokens.StatusColors.warning, active = true),
        TimelineStep("Approved", Icons.Filled.CheckCircle, DesignTokens.StatusColors.success, active = false),
    )

/** Standalone (no `feature:approvals` dependency) mock row for the approvals gallery screen. */
data class MockApproval(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
)

fun mockApprovals(): List<MockApproval> =
    listOf(
        MockApproval("a1", "Mumbai – Pune round trip", "Submitted by Rahul K.", "₹1,240"),
        MockApproval("a2", "Client site visit", "Submitted by Priya S.", "₹860"),
        MockApproval("a3", "Airport pickup", "Submitted by Rahul K.", "₹2,100"),
    )

fun mockProfileItems(): List<ProfileGridItem> =
    listOf(
        ProfileGridItem("vehicle", "Vehicle", "Verified", Icons.Filled.DirectionsCar, status = ProfileItemStatus.COMPLETE) {},
        ProfileGridItem("wallet", "Wallet", "2 cards linked", Icons.Filled.AccountBalanceWallet, status = ProfileItemStatus.COMPLETE) {},
        ProfileGridItem("id", "ID proof", "Action needed", Icons.Filled.Badge, status = ProfileItemStatus.NEEDS_ATTENTION) {},
        ProfileGridItem("bank", "Bank details", "Not added", Icons.Filled.CreditCard, status = ProfileItemStatus.INCOMPLETE) {},
    )
