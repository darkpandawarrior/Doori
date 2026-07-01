package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    LaunchedEffect(expenseId) { viewModel.onAction(ExpenseAction.OpenDetail(expenseId)) }
    val expense = ui.detailState.dataOrNull

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Expense Details",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (expense == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Expense not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.Spacing.l)
                        .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))

                ReceiptPlaceholder(expense)

                LineItemsCard(expense)

                ApprovalTimelineCard(expense)

                if (expense.note.isNotBlank()) {
                    Card(
                        shape = DesignTokens.Shape.roundedMd,
                        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.l),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                        ) {
                            Text("Note", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(expense.note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text("Seek Clarification")
                }

                Spacer(Modifier.height(DesignTokens.Spacing.l))
            }
        }
    }
}

@Composable
private fun ReceiptPlaceholder(expense: ExpenseRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = expense.category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = expense.merchantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "₹${expense.amountRupees.formatDecimal(2)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatFullDate(expense.dateMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expense.requiresApproval) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = "Approval Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LineItemsCard(expense: ExpenseRecord) {
    val lineItems = mockLineItems(expense)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = "Line Items",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Description",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text("Qty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(DesignTokens.Spacing.l))
                Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()

            lineItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${item.qty}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(DesignTokens.Spacing.l))
                    Text("₹${item.amount.toLong()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("₹${expense.amountRupees.formatDecimal(2)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApprovalTimelineCard(expense: ExpenseRecord) {
    val steps = buildTimelineSteps(expense)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                text = "Approval Timeline",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            steps.forEach { step ->
                TimelineRow(step = step)
            }
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        if (step.active) step.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = if (step.active) step.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = step.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (step.active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (step.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            if (step.timestamp.isNotBlank()) {
                Text(
                    text = step.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TimelineStep(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val active: Boolean,
    val timestamp: String = "",
)

private fun buildTimelineSteps(expense: ExpenseRecord): List<TimelineStep> {
    val submitted =
        TimelineStep(
            label = "Submitted",
            icon = Icons.Filled.Receipt,
            color = StatusColors.info,
            active = true,
            timestamp = formatFullDate(expense.dateMs),
        )
    val underReview =
        TimelineStep(
            label = "Under Review",
            icon = Icons.Filled.HourglassBottom,
            color = StatusColors.warning,
            active = expense.status != ExpenseStatus.DRAFT,
            timestamp = if (expense.status != ExpenseStatus.DRAFT) "Sent to manager" else "",
        )
    val terminal =
        when (expense.status) {
            ExpenseStatus.APPROVED ->
                TimelineStep(
                    label = "Approved",
                    icon = Icons.Filled.CheckCircle,
                    color = StatusColors.success,
                    active = true,
                    timestamp = "Reimbursement in progress",
                )
            ExpenseStatus.REJECTED ->
                TimelineStep(
                    label = "Rejected",
                    icon = Icons.Filled.Error,
                    color = StatusColors.error,
                    active = true,
                    timestamp = "Contact your manager",
                )
            else ->
                TimelineStep(
                    label = "Awaiting Decision",
                    icon = Icons.Filled.HourglassBottom,
                    color = StatusColors.neutral,
                    active = false,
                )
        }
    return listOf(submitted, underReview, terminal)
}

private data class LineItem(val description: String, val qty: Int, val amount: Double)

private fun mockLineItems(expense: ExpenseRecord): List<LineItem> {
    val total = expense.amountRupees
    return when (expense.category) {
        com.mileway.feature.logging.model.ExpenseCategory.FOOD ->
            listOf(
                LineItem("Food & Beverages", 1, total * 0.9),
                LineItem("GST (5%)", 1, total * 0.1),
            )
        com.mileway.feature.logging.model.ExpenseCategory.TRAVEL ->
            listOf(
                LineItem("Transportation", 1, total * 0.85),
                LineItem("Toll / Parking", 1, total * 0.1),
                LineItem("Service Fee", 1, total * 0.05),
            )
        com.mileway.feature.logging.model.ExpenseCategory.ACCOMMODATION ->
            listOf(
                LineItem("Room Rent", 1, total * 0.80),
                LineItem("GST (12%)", 1, total * 0.12),
                LineItem("Service Charge", 1, total * 0.08),
            )
        else ->
            listOf(
                LineItem(expense.category.label, 1, total * 0.88),
                LineItem("Tax & Charges", 1, total * 0.12),
            )
    }
}

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatFullDate(ms: Long): String {
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val amPm = if (ldt.hour < 12) "AM" else "PM"
    val h = if (ldt.hour % 12 == 0) 12 else ldt.hour % 12
    return "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}, $h:${ldt.minute.toString().padStart(2, '0')} $amPm"
}
