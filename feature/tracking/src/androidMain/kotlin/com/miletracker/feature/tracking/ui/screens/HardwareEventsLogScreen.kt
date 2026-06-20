package com.miletracker.feature.tracking.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.data.model.db.EventAudience
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.util.DateUtils
import com.miletracker.core.ui.components.sheet.AppActionSheet
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.tracking.export.HardwareEventExporter
import com.miletracker.feature.tracking.viewmodel.HardwareEventsAction
import com.miletracker.feature.tracking.viewmodel.HardwareEventsViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareEventsLogScreen(
    routeId: String,
    onBack: () -> Unit,
    viewModel: HardwareEventsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) { viewModel.onAction(HardwareEventsAction.LoadByToken(routeId)) }

    val ui by viewModel.state.collectAsState()
    val events = ui.filteredEvents
    val isLoading = ui.isLoading
    val searchQuery = ui.searchQuery
    val selectedAudiences = ui.selectedAudiences
    val stats = ui.eventStats

    if (showExportDialog) {
        AppActionSheet(
            onDismiss = { showExportDialog = false },
            title = "Export events",
        ) {
            Text(
                "Choose a format for the ${events.size} visible events:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    showExportDialog = false
                    val intent = HardwareEventExporter.shareEvents(context, routeId, events, useCsv = true)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("CSV") }
            OutlinedButton(
                onClick = {
                    showExportDialog = false
                    val intent = HardwareEventExporter.shareEvents(context, routeId, events, useCsv = false)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("JSON") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = "Hardware Events (${stats.totalCount})",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }, enabled = events.isNotEmpty()) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export events")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onAction(HardwareEventsAction.SetSearchQuery(it)) },
                placeholder = { Text("Search events…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onAction(HardwareEventsAction.SetSearchQuery("")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Audience filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(EventAudience.entries.filter { it != EventAudience.UNKNOWN }) { audience ->
                    FilterChip(
                        selected = selectedAudiences.contains(audience),
                        onClick = { viewModel.onAction(HardwareEventsAction.ToggleAudienceFilter(audience)) },
                        label = { Text(audience.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No events found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // Group by date
                val grouped =
                    events.groupBy { event ->
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(event.time))
                    }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    grouped.forEach { (date, dayEvents) ->
                        item {
                            Text(
                                date,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(dayEvents, key = { it.id }) { event ->
                            HardwareEventItem(event)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HardwareEventItem(event: HardwareEvent) {
    val (icon, color) = eventIconAndColor(event.eventType)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.event, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        DateUtils.epochToTime(event.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    event.activity?.let { activity ->
                        Text(
                            activity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            AudienceBadge(event.audience)
        }
    }
}

@Composable
private fun AudienceBadge(audience: EventAudience) {
    val color =
        when (audience) {
            EventAudience.USER -> Color(0xFF2196F3)
            EventAudience.SUPPORT -> Color(0xFFFF5722)
            EventAudience.DEBUG -> Color(0xFF9C27B0)
            EventAudience.SUMMARY -> Color(0xFF4CAF50)
            else -> Color(0xFF9E9E9E)
        }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            audience.name.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

private fun eventIconAndColor(type: EventType): Pair<ImageVector, Color> =
    when (type) {
        EventType.TRACKING_STARTED -> Icons.Default.PlayArrow to Color(0xFF4CAF50)
        EventType.TRACKING_STOPPED -> Icons.Default.Stop to Color(0xFFF44336)
        EventType.TRACKING_PAUSED -> Icons.Default.PowerOff to Color(0xFFFF9800)
        EventType.TRACKING_RESUMED -> Icons.Default.PlayArrow to Color(0xFF8BC34A)
        EventType.GPS_LOST -> Icons.Default.GpsOff to Color(0xFFF44336)
        EventType.GPS_REGAINED -> Icons.Default.GpsFixed to Color(0xFF4CAF50)
        EventType.BATTERY_OPTIMIZATION_ON, EventType.BATTERY_OPTIMIZATION_OFF -> Icons.Default.BatteryAlert to Color(0xFFFF9800)
        EventType.APP_KILLED -> Icons.Default.PhoneAndroid to Color(0xFFF44336)
        EventType.PHONE_RESTART -> Icons.Default.PhoneAndroid to Color(0xFF9C27B0)
        else -> Icons.Default.Info to Color(0xFF9E9E9E)
    }
