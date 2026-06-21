@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.miletracker.core.data.model.db.AttachmentType
import com.miletracker.core.data.model.db.TripAttachmentEntity
import com.miletracker.core.data.model.display.TrackDisplayData
import com.miletracker.core.data.util.DateUtils
import com.miletracker.core.ui.components.LoadingScreen
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.tracking.ui.components.ExportOptionsDialog
import com.miletracker.feature.tracking.viewmodel.ExportAction
import com.miletracker.feature.tracking.viewmodel.ExportViewModel
import com.miletracker.feature.tracking.viewmodel.TrackDetailAction
import com.miletracker.feature.tracking.viewmodel.TrackDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenHwEvents: () -> Unit,
    onOpenDataPreview: () -> Unit = {},
    viewModel: TrackDetailViewModel = koinViewModel(),
    exportViewModel: ExportViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val exportState by exportViewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) { viewModel.onAction(TrackDetailAction.Load(routeId)) }

    // Fire share intent once ready
    LaunchedEffect(exportState.shareIntent) {
        exportState.shareIntent?.let { intent ->
            context.startActivity(intent)
            exportViewModel.onAction(ExportAction.ClearShareIntent)
        }
    }

    // Show error in snackbar
    LaunchedEffect(exportState.error) {
        exportState.error?.let { msg ->
            snackbarHostState.showSnackbar("Export failed: $msg")
            exportViewModel.onAction(ExportAction.ClearError)
        }
    }

    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format, filter ->
                showExportDialog = false
                exportViewModel.export(context, routeId, format, filter)
            },
            trackName = uiState.track?.name,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = uiState.track?.name ?: "Journey Details",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export track")
                    }
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Default.Map, contentDescription = "View Map")
                    }
                    IconButton(onClick = onOpenInsights) {
                        Icon(Icons.Default.Insights, contentDescription = "View Insights")
                    }
                    IconButton(onClick = onOpenHwEvents) {
                        Icon(Icons.Default.History, contentDescription = "Hardware Events")
                    }
                    IconButton(onClick = onOpenDataPreview) {
                        Icon(Icons.Default.Analytics, contentDescription = "Data Preview")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen()
            return@Scaffold
        }

        val track = uiState.track ?: return@Scaffold
        val rawTrack = uiState.rawTrack
        val health = rawTrack?.let { computeHealthLevel(it) }
        val gpsPoints = if (uiState.locations.isNotEmpty()) uiState.locations.size else track.locationCount
        Column(
            modifier =
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            DetailSummaryCard(track = track, health = health)

            // Metrics grid (2 columns)
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                MetricTile(Icons.Default.Straighten, "Distance", track.getFormattedDistance(), Modifier.weight(1f))
                MetricTile(Icons.Default.Timer, "Duration", track.getFormattedDuration(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                MetricTile(Icons.Default.Speed, "Avg speed", "%.0f km/h".format(track.avgSpeedKmh), Modifier.weight(1f))
                MetricTile(Icons.Default.Place, "GPS points", gpsPoints.toString(), Modifier.weight(1f))
            }
            if (track.reimbursableAmount > 0 || track.selectedVehicleType.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    MetricTile(
                        Icons.Default.CheckCircle,
                        "Amount",
                        if (track.reimbursableAmount > 0) "₹%.0f".format(track.reimbursableAmount) else "—",
                        Modifier.weight(1f),
                    )
                    MetricTile(
                        Icons.Default.Map,
                        "Vehicle",
                        humanizeVehicle(track.selectedVehicleType),
                        Modifier.weight(1f),
                    )
                }
            }

            // Attachments section, shown only when there are persisted photos
            if (uiState.attachments.isNotEmpty()) {
                AttachmentsCard(attachments = uiState.attachments)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            FilledTonalButton(onClick = { showExportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(if (exportState.isExporting) "Exporting…" else "Export track data")
            }
            FilledTonalButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text("View route map")
            }
            FilledTonalButton(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Insights, null, Modifier.size(18.dp))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text("Trip insights")
            }
            FilledTonalButton(onClick = onOpenHwEvents, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.History, null, Modifier.size(18.dp))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text("Hardware events")
            }
            FilledTonalButton(onClick = onOpenDataPreview, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Analytics, null, Modifier.size(18.dp))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text("Data preview")
            }
        }
    }
}

@Composable
private fun DetailSummaryCard(
    track: TrackDisplayData,
    health: HealthLevel? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedLg,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.raised),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DesignTokens.topBarGradientBrush())
                    .padding(DesignTokens.Spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailStatusChip(isSubmitted = track.isSubmitted)
                if (health != null) {
                    val healthColor =
                        when (health) {
                            HealthLevel.EXCELLENT -> Color(0xFF2E7D32)
                            HealthLevel.GOOD -> Color(0xFF558B2F)
                            HealthLevel.FAIR -> Color(0xFFF9A825)
                            HealthLevel.POOR -> Color(0xFFE65100)
                            HealthLevel.CRITICAL -> Color(0xFFC62828)
                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(healthColor.copy(alpha = 0.85f))
                                .padding(horizontal = DesignTokens.Spacing.m, vertical = 4.dp),
                    ) {
                        Text(
                            health.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = track.getFormattedDistance(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            if (track.startTime > 0) {
                Text(
                    text = DateUtils.epochToDisplayDate(track.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSize.actionTile),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailStatusChip(isSubmitted: Boolean) {
    val label = if (isSubmitted) "Submitted" else "Saved"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = DesignTokens.Spacing.m, vertical = 4.dp),
    ) {
        if (isSubmitted) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(DesignTokens.IconSize.inline))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

/**
 * Card showing all locally-stored attachments for the trip: receipts as a scrollable
 * thumbnail strip, and odometer start/end proofs each with their OCR reading.
 */
@Composable
private fun AttachmentsCard(attachments: List<TripAttachmentEntity>) {
    val receipts = attachments.filter { it.type == AttachmentType.RECEIPT }
    val odoStart = attachments.lastOrNull { it.type == AttachmentType.ODOMETER_START }
    val odoEnd = attachments.lastOrNull { it.type == AttachmentType.ODOMETER_END }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                Text(
                    "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Odometer proofs
            if (odoStart != null || odoEnd != null) {
                Text(
                    "Odometer proofs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    odoStart?.let { a ->
                        OdometerProofTile(label = "Start", uri = a.uri, ocrText = a.ocrText)
                    }
                    odoEnd?.let { a ->
                        OdometerProofTile(label = "End", uri = a.uri, ocrText = a.ocrText)
                    }
                }
            }

            // Receipt thumbnails
            if (receipts.isNotEmpty()) {
                Text(
                    "Receipts (${receipts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    receipts.forEach { a ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = a.uri,
                                contentDescription = "Receipt photo",
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OdometerProofTile(
    label: String,
    uri: String,
    ocrText: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AsyncImage(
            model = uri,
            contentDescription = "$label odometer proof",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        if (!ocrText.isNullOrBlank()) {
            Text(
                ocrText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/** Turns a camelCase vehicle key (e.g. "fourWheelerPetrol") into "Four Wheeler Petrol". */
private fun humanizeVehicle(key: String): String {
    if (key.isBlank()) return "—"
    return key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercaseChar() }
}
