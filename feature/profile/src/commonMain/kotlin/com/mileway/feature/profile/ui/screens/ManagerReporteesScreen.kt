package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.model.ReporteeTrip
import com.mileway.feature.profile.model.ReporteeTripSummary
import com.mileway.feature.profile.model.SeededReportees
import com.mileway.feature.profile.viewmodel.ManagerReporteesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val headerGradient = listOf(Color(0xFF0F4C75), Color(0xFF1B6CA8))

/**
 * PLAN_V24 P10.6: manager-only reportee tracking view — a list of the manager's reportees, each
 * with a seeded per-reportee summary (trip count, total km, pending approvals, last trip). Tapping
 * a reportee drills into [ManagerReporteeDetailScreen]. Reached only when `trackMileageManagerView`
 * is on (the entry tile is plugin-gated); [ManagerReporteesUiState.enabled] is a defensive gate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerReporteesScreen(
    onBack: () -> Unit,
    onOpenReportee: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManagerReporteesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ManagerHeader(
                title = mrv("manager_view_title", "My reportees"),
                subtitle = mrv("manager_view_subtitle", "Team tracking overview"),
                onBack = onBack,
            )
            if (!state.enabled) {
                EmptyManagerState(mrv("manager_view_disabled", "Manager view is not enabled"))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    items(state.summaries, key = { it.reportee.code }) { summary ->
                        ReporteeCard(summary, onClick = { onOpenReportee(summary.reportee.code) })
                    }
                    item { Spacer(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}

/**
 * Drill-in: one reportee's seeded trip list. Reads the reportee identity from [SeededReportees] and
 * the trips from the shared [ManagerReporteesViewModel] seed passthrough.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerReporteeDetailScreen(
    code: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManagerReporteesViewModel = koinViewModel(),
) {
    val reportee = SeededReportees.all.firstOrNull { it.code == code }
    val trips = viewModel.tripsFor(code)

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ManagerHeader(
                title = reportee?.name ?: mrv("manager_view_reportee", "Reportee"),
                subtitle = reportee?.code ?: code,
                onBack = onBack,
            )
            if (trips.isEmpty()) {
                EmptyManagerState(mrv("manager_view_no_trips", "No trips recorded yet"))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    items(trips, key = { it.id }) { trip -> TripRow(trip) }
                    item { Spacer(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}

@Composable
private fun ManagerHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(headerGradient))
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = mrv("manager_view_back", "Back"), tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun ReporteeCard(
    summary: ReporteeTripSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(Color(0xFF1B6CA8).copy(alpha = 0.12f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1B6CA8))
                }
                Spacer(Modifier.width(DesignTokens.Spacing.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.reportee.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(summary.reportee.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.padding(top = DesignTokens.Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
                StatChip(Icons.Default.Route, mrv("manager_view_trips", "{n} trips").replace("{n}", summary.tripCount.toString()))
                StatChip(Icons.AutoMirrored.Filled.TrendingUp, "${summary.totalKm.roundToInt()} km")
                if (summary.pendingApprovals > 0) {
                    StatChip(
                        Icons.Default.PendingActions,
                        mrv("manager_view_pending", "{n} pending").replace("{n}", summary.pendingApprovals.toString()),
                        tint = Color(0xFFEA580C),
                    )
                }
            }
            if (summary.lastTripLabel.isNotEmpty()) {
                Spacer(Modifier.padding(top = DesignTokens.Spacing.xs))
                Text(
                    mrv("manager_view_last_trip", "Last: {label}").replace("{label}", summary.lastTripLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(color = tint.copy(alpha = 0.10f), shape = DesignTokens.Shape.roundedSm) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(DesignTokens.Spacing.xs))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TripRow(trip: ReporteeTrip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.fromTo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(trip.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${trip.distanceKm.roundToInt()} km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(trip.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyManagerState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xl), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Screen-internal labels via the dynamic resolver to avoid generated-symbol churn. */
@Composable
private fun mrv(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
