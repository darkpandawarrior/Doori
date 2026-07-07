package com.mileway.feature.tracking.ui.screens

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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_cd_open_map
import com.mileway.core.ui.resources.tracking_live_subtitle
import com.mileway.core.ui.resources.tracking_live_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.tracking.ui.components.LiveHealthMonitorCard
import com.mileway.feature.tracking.ui.components.LiveSyncStatusCard
import com.mileway.feature.tracking.ui.components.LiveTrackingOverviewCard
import com.mileway.feature.tracking.ui.components.RecentEventsCard
import com.mileway.feature.tracking.viewmodel.LiveTrackAction
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackingUiState
import org.jetbrains.compose.resources.stringResource
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
            DepthAwareTopBar(
                title = stringResource(Res.string.tracking_live_title),
                subtitle = stringResource(Res.string.tracking_live_subtitle),
                titleIcon = Icons.Default.MyLocation,
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tracking_cd_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                shape = DesignTokens.Shape.button,
                onClick = onOpenMap,
            ) {
                Icon(Icons.Default.Map, contentDescription = stringResource(Res.string.tracking_cd_open_map))
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
