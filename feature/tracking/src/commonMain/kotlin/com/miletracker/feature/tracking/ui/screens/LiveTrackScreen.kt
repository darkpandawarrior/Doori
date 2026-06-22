package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.feature.tracking.ui.components.LiveHealthMonitorCard
import com.miletracker.feature.tracking.ui.components.LiveSyncStatusCard
import com.miletracker.feature.tracking.ui.components.LiveTrackingOverviewCard
import com.miletracker.feature.tracking.ui.components.RecentEventsCard
import com.miletracker.feature.tracking.viewmodel.LiveTrackAction
import com.miletracker.feature.tracking.viewmodel.LiveTrackViewModel
import com.miletracker.feature.tracking.viewmodel.LiveTrackingUiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackScreen(
    routeId: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: LiveTrackViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onAction(LiveTrackAction.Refresh) }

    val ui by viewModel.state.collectAsState()
    val liveState = ui.liveTrackingState
    val hardwareEvents = ui.hardwareEventsState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Tracking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenMap) {
                Icon(Icons.Default.Map, contentDescription = "Open Map")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = liveState) {
                is LiveTrackingUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is LiveTrackingUiState.Error ->
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )

                is LiveTrackingUiState.Success -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LiveTrackingOverviewCard(state.trackData, modifier = Modifier.fillMaxWidth())

                        LiveHealthMonitorCard(
                            locationCount = state.locationPoints.size,
                            unsyncedCount = state.trackData.unsyncedLocationPoints,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        LiveSyncStatusCard(
                            total = state.trackData.totalLocationPoints,
                            unsynced = state.trackData.unsyncedLocationPoints,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (hardwareEvents.isNotEmpty()) {
                            RecentEventsCard(hardwareEvents, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}
