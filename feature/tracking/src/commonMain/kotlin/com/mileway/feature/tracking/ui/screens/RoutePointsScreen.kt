@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.tracking.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_retry
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_load_more_error
import com.mileway.core.ui.resources.tracking_route_points_count
import com.mileway.core.ui.resources.tracking_route_points_empty_subtitle
import com.mileway.core.ui.resources.tracking_route_points_empty_title
import com.mileway.core.ui.resources.tracking_route_points_load_error
import com.mileway.core.ui.resources.tracking_route_points_subtitle
import com.mileway.core.ui.resources.tracking_route_points_title
import com.mileway.core.ui.resources.tracking_unknown_error
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.tracking.viewmodel.RoutePointUi
import com.mileway.feature.tracking.viewmodel.RoutePointsAction
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * G1 (Paging 3): the raw GPS route-points log for one journey, paged out of Room via
 * [RoutePointsViewModel.points] and collected with [collectAsLazyPagingItems]. A long trip is
 * thousands of fixes, so this is the one genuinely list-heavy surface — points stream in
 * chronologically as the user scrolls, with proper refresh/append load-state handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePointsScreen(
    routeId: String,
    onBack: () -> Unit,
    viewModel: RoutePointsViewModel = koinViewModel(),
) {
    LaunchedEffect(routeId) { viewModel.onAction(RoutePointsAction.Load(routeId)) }

    val ui by viewModel.state.collectAsState()
    val points = viewModel.points.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title =
                    ui.totalPoints?.let { stringResource(Res.string.tracking_route_points_count, it) }
                        ?: stringResource(Res.string.tracking_route_points_title),
                subtitle = stringResource(Res.string.tracking_route_points_subtitle),
                depth = NavigationDepth.LEVEL_2,
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
    ) { padding ->
        // A.9: pull-to-refresh re-queries the page from Room via the PagingSource. Only show the pull
        // spinner when refreshing an already-loaded list; the initial load uses the centered spinner below.
        val isRefreshing = points.loadState.refresh is LoadState.Loading && points.itemCount > 0
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { points.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val refresh = points.loadState.refresh
            when {
                refresh is LoadState.Loading && points.itemCount == 0 -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                refresh is LoadState.Error && points.itemCount == 0 -> {
                    RoutePointsMessage(
                        icon = Icons.Filled.Warning,
                        title = stringResource(Res.string.tracking_route_points_load_error),
                        subtitle = refresh.error.message ?: stringResource(Res.string.tracking_unknown_error),
                        actionLabel = stringResource(Res.string.tracking_action_retry),
                        onAction = { points.retry() },
                    )
                }
                refresh is LoadState.NotLoading && points.itemCount == 0 -> {
                    RoutePointsMessage(
                        icon = Icons.Filled.LocationOff,
                        title = stringResource(Res.string.tracking_route_points_empty_title),
                        subtitle = stringResource(Res.string.tracking_route_points_empty_subtitle),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        items(
                            count = points.itemCount,
                            key = points.itemKey { it.id },
                        ) { index ->
                            points[index]?.let { point -> RoutePointRow(index = index, point = point) }
                        }

                        // Append load-state footer: spinner while the next page loads, retry on error.
                        when (val append = points.loadState.append) {
                            is LoadState.Loading ->
                                item {
                                    Box(Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m), Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    }
                                }
                            is LoadState.Error ->
                                item {
                                    Column(
                                        Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            append.error.message ?: stringResource(Res.string.tracking_load_more_error),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Button(onClick = { points.retry() }) {
                                            Text(stringResource(Res.string.tracking_action_retry))
                                        }
                                    }
                                }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

private fun formatPointTime(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}:${dt.second.toString().padStart(2, '0')}"
}

@Composable
private fun RoutePointRow(
    index: Int,
    point: RoutePointUi,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Spacing.s),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sequence index — 1-based for humans.
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${formatCoord(point.lat)}, ${formatCoord(point.lng)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text =
                        buildString {
                            append(formatPointTime(point.timeMillis))
                            append("  ·  ")
                            append("${point.speedKmh.format1()} km/h")
                            append("  ·  ±${point.accuracyM.toInt()}m")
                            if (point.provider.isNotBlank() && point.provider != "NONE") {
                                append("  ·  ${point.provider.lowercase()}")
                            }
                        },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (point.isCheckIn) PointFlag(Icons.Filled.PinDrop, MaterialTheme.colorScheme.primary)
            if (point.isPaused) PointFlag(Icons.Filled.Pause, MaterialTheme.colorScheme.tertiary)
            if (point.isAbnormal) PointFlag(Icons.Filled.Warning, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PointFlag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
) {
    Spacer(Modifier.width(DesignTokens.Spacing.xs))
    Box(
        Modifier.size(28.dp).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RoutePointsMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(DesignTokens.Spacing.m))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun formatCoord(value: Double): String {
    // 5 decimal places ≈ 1.1 m precision — enough to read a GPS trail.
    val scaled = kotlin.math.round(value * 100_000.0) / 100_000.0
    return scaled.toString()
}

private fun Double.format1(): String {
    val scaled = kotlin.math.round(this * 10.0) / 10.0
    return scaled.toString()
}
