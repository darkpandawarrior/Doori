package com.mileway.webpreview.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.terminalStyle
import com.mileway.webpreview.DemoExpenseStore
import com.mileway.webpreview.DemoTrackingEngine
import com.mileway.webpreview.ExpenseStatus
import com.mileway.webpreview.formatInr
import com.mileway.webpreview.formatKm

@Composable
fun DashboardScreen(
    engine: DemoTrackingEngine,
    store: DemoExpenseStore,
    onStartTracking: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracking by engine.state.collectAsState()
    val expenses by store.expenses.collectAsState()
    val pending = expenses.filter { it.status != ExpenseStatus.APPROVED }.sumOf { it.amountInr }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DesignTokens.Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Column {
            Text(
                text = "MILEWAY // WEB PREVIEW",
                style = MaterialTheme.typography.labelMedium.terminalStyle(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = "Good to see you, Sid",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Simulated data — every screen runs offline, in your browser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            StatCard(
                label = "TRACKED",
                value = formatKm(128.4 + tracking.distanceKm),
                caption = "this week",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "PENDING",
                value = formatInr(pending),
                caption = "${expenses.count { it.status != ExpenseStatus.APPROVED }} expenses",
                modifier = Modifier.weight(1f),
            )
        }

        if (tracking.isTracking) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(DesignTokens.Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.m))
                    Column {
                        Text(
                            text = if (tracking.isPaused) "Trip paused" else "Trip in progress",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "${formatKm(tracking.distanceKm)} · ${tracking.speedKmh.toInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium.terminalStyle(),
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            Button(
                onClick = onStartTracking,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null)
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                Text(if (tracking.isTracking) "View trip" else "Start tracking")
            }
            OutlinedButton(
                onClick = onAddExpense,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(),
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                Text("Add expense")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            Text(
                text = "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelMedium.terminalStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            expenses.take(4).forEach { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedSm,
                    elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${expense.category} · ${expense.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatInr(expense.amountInr),
                            style = MaterialTheme.typography.bodyMedium.terminalStyle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(Modifier.padding(DesignTokens.Spacing.l)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.terminalStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.terminalStyle(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
