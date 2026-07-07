package com.mileway.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.feature.agent.model.PopularQuestion
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.profile.model.CardStatus
import com.mileway.feature.profile.model.CardType
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemApproved
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemPending
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemRejected
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemWithViolation
import com.mileway.feature.payables.ui.previews.PreviewPoCardApproved
import com.mileway.feature.payables.ui.previews.PreviewPoCardPendingApproval
import com.mileway.feature.payables.ui.previews.PreviewPoLineItemsMatrix
import com.mileway.feature.payables.ui.previews.PreviewPoListMatrix
import com.mileway.feature.tracking.debug.DebugSectionCard
import com.mileway.feature.tracking.ui.previews.PreviewSetupGuideScreen
import com.mileway.feature.tracking.ui.previews.PreviewTrackLoadingCustomMessage
import com.mileway.feature.tracking.ui.previews.PreviewTrackLoadingDefault
import com.mileway.feature.tracking.ui.previews.PreviewTrackSettingsScreen
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessClean
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessWithViolation
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessWithVoucher
import com.mileway.feature.travel.ui.previews.PreviewBookingCardActiveFlight
import com.mileway.feature.travel.ui.previews.PreviewBookingCardCompletedFlight
import com.mileway.feature.travel.ui.previews.PreviewBookingCardUpcomingTrain
import com.mileway.feature.travel.ui.previews.PreviewBookingListMatrix

// ---------------------------------------------------------------------------
// Component metadata, each entry describes one showcaseable composable
// ---------------------------------------------------------------------------

data class ShowcaseEntry(
    val name: String,
    val group: String,
    val description: String,
    val content: @Composable () -> Unit,
)

private val MOCK_TIMESTAMP = 1_718_200_000_000L

val ALL_SHOWCASES: List<ShowcaseEntry> = listOf(

    // ── Approvals ───────────────────────────────────────────────────────────
    ShowcaseEntry(
        name = "Approval Card – Pending",
        group = "Approvals",
        description = "Standard pending card shown in the team approvals list.",
        content = {
            ApprovalCardShowcase(
                id = "A001", type = ApprovalType.MILEAGE,
                requester = "Priya Sharma", summary = "Client visit – 48 km trip",
                amount = 576.0, status = ApprovalStatus.PENDING,
                policyViolation = false, selectionMode = false, isSelected = false,
            )
        },
    ),
    ShowcaseEntry(
        name = "Approval Card – Policy Violation",
        group = "Approvals",
        description = "Pending card with the policy-violation flag set.",
        content = {
            ApprovalCardShowcase(
                id = "A003", type = ApprovalType.TRAVEL,
                requester = "Aisha Khan", summary = "Bangalore–Pune flight",
                amount = 8400.0, status = ApprovalStatus.PENDING,
                policyViolation = true, selectionMode = false, isSelected = false,
            )
        },
    ),
    ShowcaseEntry(
        name = "Approval Card – Selected",
        group = "Approvals",
        description = "Card in multi-select mode with the checkbox checked.",
        content = {
            ApprovalCardShowcase(
                id = "A002", type = ApprovalType.EXPENSE,
                requester = "Rahul Mehra", summary = "Business dinner – ₹3,200",
                amount = 3200.0, status = ApprovalStatus.PENDING,
                policyViolation = false, selectionMode = true, isSelected = true,
            )
        },
    ),
    ShowcaseEntry(
        name = "Approval Card – Approved",
        group = "Approvals",
        description = "Resolved card with APPROVED status chip.",
        content = {
            ApprovalCardShowcase(
                id = "A005", type = ApprovalType.EXPENSE,
                requester = "Neha Patel", summary = "Office supplies ₹680",
                amount = 680.0, status = ApprovalStatus.APPROVED,
                policyViolation = false, selectionMode = false, isSelected = false,
            )
        },
    ),

    // ── Agent ────────────────────────────────────────────────────────────────
    ShowcaseEntry(
        name = "Popular Question Row",
        group = "Agent",
        description = "A single row from the Popular Questions tab.",
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = DesignTokens.Shape.button,
            ) {
                PopularQuestionRow(
                    question = PopularQuestion(
                        id = "PQ-001",
                        question = "What is the mileage reimbursement rate?",
                        module = "Mileage",
                        askCount = 248,
                        isTrending = true,
                    )
                )
            }
        },
    ),
    ShowcaseEntry(
        name = "Popular Question Row – Not Trending",
        group = "Agent",
        description = "A row from the Popular Questions tab without the trending badge.",
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = DesignTokens.Shape.button,
            ) {
                PopularQuestionRow(
                    question = PopularQuestion(
                        id = "PQ-004",
                        question = "Can I log a manual mileage entry?",
                        module = "Mileage",
                        askCount = 102,
                        isTrending = false,
                    )
                )
            }
        },
    ),

    // ── QR ──────────────────────────────────────────────────────────────────
    ShowcaseEntry(
        name = "QR Card Chip",
        group = "QR",
        description = "Card chip row shown in the linked-cards LazyRow on the QR screen.",
        content = {
            QrCardChipShowcase(
                card = CorporateCard(
                    id = "CARD-001",
                    lastFourDigits = "4821",
                    cardType = CardType.VISA,
                    holderName = "Priya Sharma",
                    balanceRupees = 48000.0,
                    status = CardStatus.ACTIVE,
                    expiryDate = "12/26",
                    creditLimitRupees = 100000.0,
                )
            )
        },
    ),
    ShowcaseEntry(
        name = "QR Info Row",
        group = "QR",
        description = "Three-tile stat row showing Daily Limit / Received / Month totals.",
        content = { QrInfoRowShowcase() },
    ),

    // ── Debug ────────────────────────────────────────────────────────────────
    ShowcaseEntry(
        name = "Debug Section Card",
        group = "Debug",
        description = "Collapsible section card used in the Developer Options screen.",
        content = {
            DebugSectionCard(title = "Location & Tracking", icon = Icons.Default.BugReport) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Allow Mock Locations", style = MaterialTheme.typography.bodyMedium)
                    Text("High Accuracy Mode", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
    ),

    // ── Tracking ─────────────────────────────────────────────────────────────
    ShowcaseEntry("Track Loading: default", "Tracking", "Loading screen with a single status message.") {
        PreviewTrackLoadingDefault()
    },
    ShowcaseEntry("Track Loading: sub-statuses", "Tracking", "Loading screen with an animated list of sub-statuses.") {
        PreviewTrackLoadingCustomMessage()
    },
    ShowcaseEntry("Success: clean", "Tracking", "Trip submitted without any violations or voucher.") {
        PreviewTrackingSuccessClean()
    },
    ShowcaseEntry("Success: violation", "Tracking", "Trip submitted with a policy violation banner.") {
        PreviewTrackingSuccessWithViolation()
    },
    ShowcaseEntry("Success: voucher", "Tracking", "Trip submitted with a voucher number and amount.") {
        PreviewTrackingSuccessWithVoucher()
    },
    ShowcaseEntry("Track Settings", "Tracking", "GPS accuracy and sensor settings screen.") {
        PreviewTrackSettingsScreen()
    },
    ShowcaseEntry("Setup Guide", "Tracking", "First-run guide for enabling location permissions.") {
        PreviewSetupGuideScreen()
    },

    // ── Payables ─────────────────────────────────────────────────────────────
    ShowcaseEntry("PO Card: approved", "Payables", "Purchase order card in the Approved state.") {
        PreviewPoCardApproved()
    },
    ShowcaseEntry("PO Card: pending", "Payables", "Purchase order card awaiting approval.") {
        PreviewPoCardPendingApproval()
    },
    ShowcaseEntry("PO List matrix", "Payables", "Two PO cards rendered side by side.") {
        PreviewPoListMatrix()
    },
    ShowcaseEntry("PO Line items", "Payables", "Breakdown of individual line items in a PO.") {
        PreviewPoLineItemsMatrix()
    },

    // ── Travel ───────────────────────────────────────────────────────────────
    ShowcaseEntry("Flight: active", "Travel", "Active flight booking card with gate and boarding info.") {
        PreviewBookingCardActiveFlight()
    },
    ShowcaseEntry("Train: upcoming", "Travel", "Upcoming train booking card.") {
        PreviewBookingCardUpcomingTrain()
    },
    ShowcaseEntry("Flight: completed", "Travel", "Completed flight booking card.") {
        PreviewBookingCardCompletedFlight()
    },
    ShowcaseEntry("Booking list", "Travel", "Full list of mixed bookings (flight, train).") {
        PreviewBookingListMatrix()
    },
)

// ---------------------------------------------------------------------------
// Showcase screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcaseScreen(onBack: () -> Unit) {
    val groups = ALL_SHOWCASES.map { it.group }.distinct()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedEntry by remember { mutableIntStateOf(-1) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (selectedEntry >= 0) {
        val entry = ALL_SHOWCASES[selectedEntry]
        ComponentDetailScreen(entry = entry, onBack = { selectedEntry = -1 })
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Component Showcase",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${ALL_SHOWCASES.size} components",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                groups.forEachIndexed { idx, group ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(group) },
                        icon = { Icon(groupIcon(group), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            val currentGroup = groups[selectedTab]
            val entries = ALL_SHOWCASES
                .mapIndexed { i, e -> i to e }
                .filter { (_, e) -> e.group == currentGroup }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries) { (globalIdx, entry) ->
                    ShowcaseEntryCard(
                        entry = entry,
                        onClick = { selectedEntry = globalIdx },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseEntryCard(entry: ShowcaseEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        entry.group,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.Shape.button)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                entry.content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentDetailScreen(entry: ShowcaseEntry, onBack: () -> Unit) {
    MilewayTheme {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(text = entry.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = entry.group, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                entry.content()
            }
        }
    }
}

private fun groupIcon(group: String): ImageVector = when (group) {
    "Approvals" -> Icons.Default.Approval
    "Agent" -> Icons.Default.AutoAwesome
    "QR" -> Icons.Default.QrCode
    "Debug" -> Icons.Default.BugReport
    "Home" -> Icons.Default.DirectionsCar
    "Profile" -> Icons.Default.AccountCircle
    "Tracking" -> Icons.Default.Route
    "Payables" -> Icons.Default.Payments
    "Travel" -> Icons.Default.FlightTakeoff
    else -> Icons.Default.Widgets
}

// ---------------------------------------------------------------------------
// Inline stubs that render the real composables with preview-quality mock data
// (these delegate to the real composables via their public APIs or inline
//  equivalent rendering where the real composable is private)
// ---------------------------------------------------------------------------

@Composable
private fun ApprovalCardShowcase(
    id: String,
    type: ApprovalType,
    requester: String,
    summary: String,
    amount: Double,
    status: ApprovalStatus,
    policyViolation: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
) {
    val item = ApprovalItem(
        id = id,
        type = type,
        requesterName = requester,
        summary = summary,
        amountRupees = amount,
        status = status,
        timestampMs = MOCK_TIMESTAMP - 3_600_000L,
        policyViolation = policyViolation,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DesignTokens.Shape.roundedSm)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(requester, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (policyViolation) {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("Policy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${amount.toLong()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Badge(
                containerColor = when (status) {
                    ApprovalStatus.APPROVED -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                    ApprovalStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    ApprovalStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Text(
                    status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (status) {
                        ApprovalStatus.APPROVED -> androidx.compose.ui.graphics.Color.White
                        ApprovalStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                        ApprovalStatus.PENDING -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun PopularQuestionRow(question: PopularQuestion) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(question.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${question.module} · ${question.askCount} asks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (question.isTrending) {
            Badge(containerColor = androidx.compose.ui.graphics.Color(0xFFFFA000)) {
                Text("🔥 Trending", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

@Composable
private fun QrCardChipShowcase(card: CorporateCard) {
    Surface(
        shape = DesignTokens.Shape.button,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                "•••• ${card.lastFourDigits}  ${card.cardType.name}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun QrInfoRowShowcase() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Daily Limit" to "₹50,000", "Today" to "₹0", "This Month" to "₹12,400").forEach { (label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
