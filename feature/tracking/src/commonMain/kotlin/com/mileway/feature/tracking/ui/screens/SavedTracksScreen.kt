package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siddharth.kmp.common.formatDecimal
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.util.DateUtils
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.core.ui.components.ConfettiBurst
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.components.LoadingScreen
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_collapse
import com.mileway.core.ui.resources.tracking_cd_exit_selection
import com.mileway.core.ui.resources.tracking_cd_expand
import com.mileway.core.ui.resources.tracking_detail_amount
import com.mileway.core.ui.resources.tracking_insights_distance
import com.mileway.core.ui.resources.tracking_insights_duration
import com.mileway.core.ui.resources.tracking_route_line
import com.mileway.core.ui.resources.tracking_saved_empty_journeys_hint
import com.mileway.core.ui.resources.tracking_saved_empty_journeys_none
import com.mileway.core.ui.resources.tracking_saved_empty_journeys_query
import com.mileway.core.ui.resources.tracking_saved_empty_search_hint
import com.mileway.core.ui.resources.tracking_saved_empty_submissions_hint
import com.mileway.core.ui.resources.tracking_saved_empty_submissions_none
import com.mileway.core.ui.resources.tracking_saved_empty_submissions_query
import com.mileway.core.ui.resources.tracking_saved_filter_all
import com.mileway.core.ui.resources.tracking_saved_filter_all_count
import com.mileway.core.ui.resources.tracking_saved_filter_filed_count
import com.mileway.core.ui.resources.tracking_saved_filter_kept
import com.mileway.core.ui.resources.tracking_saved_filter_new_tracker
import com.mileway.core.ui.resources.tracking_saved_filter_other
import com.mileway.core.ui.resources.tracking_saved_filter_this_week
import com.mileway.core.ui.resources.tracking_saved_filter_unclaimed_count
import com.mileway.core.ui.resources.tracking_saved_insight_avg
import com.mileway.core.ui.resources.tracking_saved_insight_count
import com.mileway.core.ui.resources.tracking_saved_journey_default
import com.mileway.core.ui.resources.tracking_saved_search_journeys
import com.mileway.core.ui.resources.tracking_saved_search_submissions
import com.mileway.core.ui.resources.tracking_saved_select_hint
import com.mileway.core.ui.resources.tracking_saved_select_submissions
import com.mileway.core.ui.resources.tracking_saved_start_journey
import com.mileway.core.ui.resources.tracking_saved_stat_reimbursed
import com.mileway.core.ui.resources.tracking_saved_stat_trips
import com.mileway.core.ui.resources.tracking_saved_subtitle
import com.mileway.core.ui.resources.tracking_saved_title
import com.mileway.core.ui.resources.tracking_saved_week_summary
import com.mileway.core.ui.resources.tracking_status_saved
import com.mileway.core.ui.resources.tracking_status_submitted
import com.mileway.core.ui.resources.tracking_unknown_place
import com.mileway.core.ui.resources.tracking_voucher_created_snackbar
import com.mileway.core.ui.resources.tracking_voucher_summary_total
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.model.toLinkedContext
import com.mileway.feature.tracking.ui.components.CreateVoucherButton
import com.mileway.feature.tracking.ui.components.LinkedContextCard
import com.mileway.feature.tracking.ui.components.NoJourneysThisWeekState
import com.mileway.feature.tracking.ui.components.NoSubmissionsState
import com.mileway.feature.tracking.ui.components.SavedTracksChipRow
import com.mileway.feature.tracking.ui.components.SavedTracksFilterChip
import com.mileway.feature.tracking.ui.components.SavedTracksSearchField
import com.mileway.feature.tracking.ui.components.SavedTracksSegment
import com.mileway.feature.tracking.ui.components.SavedTracksSegmentedToggle
import com.mileway.feature.tracking.ui.components.StaticPolylineThumbnail
import com.mileway.feature.tracking.ui.components.StatusBadge
import com.mileway.feature.tracking.ui.components.SubmissionCard
import com.mileway.feature.tracking.ui.components.SubmissionCardData
import com.mileway.feature.tracking.ui.components.SubmissionDateHeader
import com.mileway.feature.tracking.ui.components.SubmissionSelectionRow
import com.mileway.feature.tracking.viewmodel.JourneyFilter
import com.mileway.feature.tracking.viewmodel.SavedTracksAction
import com.mileway.feature.tracking.viewmodel.SavedTracksTab
import com.mileway.feature.tracking.viewmodel.SavedTracksUiState
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SubmissionFilter
import com.mileway.feature.tracking.viewmodel.SubmissionItem
import com.mileway.feature.tracking.viewmodel.SubmissionSource
import com.mileway.feature.tracking.viewmodel.SyncStatusViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * Top-level "Saved Tracks" tab, a Journeys/Submissions surface sitting above the bubble bottom bar.
 *
 * The public signature is intentionally stable: the integrator still passes [onTrackClick] and
 * [onStartNew], and the screen still owns its own [SavedTracksViewModel] via Koin.
 *
 * Layout:
 *  - Gradient ROOT header with aggregate stats (the "deeper = calmer" anchor).
 *  - A Journeys/Submissions segmented toggle, a rounded search field, and chip filters.
 *  - Journeys tab: the filtered journey card list (with a "No journeys this week" empty state).
 *  - Submissions tab: date-grouped submission cards with a long-press selection mode and a
 *    full-width "Create Voucher" CTA. Creating a voucher is a pure-demo acknowledgement (confetti).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTracksScreen(
    onTrackClick: (String) -> Unit,
    onStartNew: () -> Unit,
    viewModel: SavedTracksViewModel = koinViewModel(),
    syncStatusViewModel: SyncStatusViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val syncChipText by syncStatusViewModel.chipText.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pure-demo voucher acknowledgement: snackbar + confetti, then consume the one-shot flag.
    LaunchedEffect(uiState.voucherCreatedAck) {
        if (uiState.voucherCreatedAck) {
            snackbarHostState.showSnackbar(getString(Res.string.tracking_voucher_created_snackbar))
            viewModel.onAction(SavedTracksAction.VoucherAckConsumed)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    shape = DesignTokens.Shape.button,
                    onClick = onStartNew,
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text(stringResource(Res.string.tracking_saved_start_journey)) },
                    // Lift above the floating bubble bar.
                    modifier = Modifier.padding(bottom = 88.dp),
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize()) {
                TrackMilesHeader(
                    tracks = uiState.tracks,
                    selectionMode = uiState.selectionMode,
                    onClearSelection = { viewModel.onAction(SavedTracksAction.ClearSelection) },
                )
                // Wave-4 §2.3: sync-status chip — null while idle (no session has ever synced).
                syncChipText?.let { text ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                    ) {
                        StatusBadge(text = text, color = MilewayColors.neutral)
                    }
                }
                SavedTracksBody(
                    uiState = uiState,
                    bottomPadding = padding.calculateBottomPadding(),
                    onTrackClick = onTrackClick,
                    viewModel = viewModel,
                )
            }
        }

        // Celebration overlay floats above everything while the flag is set.
        if (uiState.voucherCreatedAck) {
            ConfettiBurst(modifier = Modifier.fillMaxSize())
        }
    }
}

/** Gradient ROOT header with title + summary stats, the screen's anchor ("deeper = calmer"). */
@Composable
private fun TrackMilesHeader(
    tracks: List<TrackDisplayData>,
    selectionMode: Boolean = false,
    onClearSelection: () -> Unit = {},
) {
    val totalTrips = tracks.size
    val totalKm = tracks.sumOf { it.distanceKm }
    val totalReimbursable = tracks.filter { it.isSubmitted }.sumOf { it.reimbursableAmount }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DesignTokens.topBarGradientBrush())
                .statusBarsPadding()
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            if (selectionMode) {
                                stringResource(
                                    Res.string.tracking_saved_select_submissions,
                                )
                            } else {
                                stringResource(Res.string.tracking_saved_title)
                            },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = if (selectionMode) stringResource(Res.string.tracking_saved_select_hint) else stringResource(Res.string.tracking_saved_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                // VI.3: "X" button exits selection mode
                if (selectionMode) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.tracking_cd_exit_selection), tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                HeaderStat(value = totalTrips.toString(), label = stringResource(Res.string.tracking_saved_stat_trips), modifier = Modifier.weight(1f))
                HeaderStat(
                    value = "${totalKm.formatDecimal(0)} km",
                    label = stringResource(Res.string.tracking_insights_distance),
                    modifier = Modifier.weight(1f),
                )
                HeaderStat(
                    value = "₹${totalReimbursable.formatDecimal(0)}",
                    label = stringResource(Res.string.tracking_saved_stat_reimbursed),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeaderStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(DesignTokens.Shape.roundedSm)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(vertical = DesignTokens.Spacing.m, horizontal = DesignTokens.Spacing.s),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.dataStyle(), fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/**
 * Below-header content: the toggle/search/chips controls and the per-tab list. Everything below
 * the gradient header lives in a single [LazyColumn] so the controls scroll with the content.
 */
@Composable
private fun SavedTracksBody(
    uiState: SavedTracksUiState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTrackClick: (String) -> Unit,
    viewModel: SavedTracksViewModel,
) {
    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding =
            PaddingValues(
                top = DesignTokens.Spacing.l,
                bottom = bottomPadding + 140.dp,
                start = DesignTokens.Spacing.l,
                end = DesignTokens.Spacing.l,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        // Controls shared by both tabs.
        item {
            SavedTracksSegmentedToggle(
                selected = if (uiState.tab == SavedTracksTab.JOURNEYS) SavedTracksSegment.JOURNEYS else SavedTracksSegment.SUBMISSIONS,
                journeyCount = uiState.journeyCount,
                submissionCount = uiState.submissionCount,
                onSelect = { segment ->
                    viewModel.onAction(
                        SavedTracksAction.TabSelected(
                            if (segment == SavedTracksSegment.JOURNEYS) SavedTracksTab.JOURNEYS else SavedTracksTab.SUBMISSIONS,
                        ),
                    )
                },
            )
        }

        when (uiState.tab) {
            SavedTracksTab.JOURNEYS -> journeysSection(uiState, onTrackClick, viewModel)
            SavedTracksTab.SUBMISSIONS -> submissionsSection(uiState, viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Journeys tab
// ─────────────────────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.journeysSection(
    uiState: SavedTracksUiState,
    onTrackClick: (String) -> Unit,
    viewModel: SavedTracksViewModel,
) {
    item {
        SavedTracksSearchField(
            query = uiState.journeySearch,
            placeholder = stringResource(Res.string.tracking_saved_search_journeys),
            onQueryChange = { viewModel.onAction(SavedTracksAction.JourneySearchChanged(it)) },
            onFilterClick = null,
        )
    }
    item {
        SavedTracksChipRow {
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_this_week),
                selected = uiState.journeyFilter == JourneyFilter.THIS_WEEK,
                onClick = { viewModel.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.THIS_WEEK)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_kept),
                selected = uiState.journeyFilter == JourneyFilter.KEPT,
                onClick = { viewModel.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.KEPT)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_all),
                selected = uiState.journeyFilter == JourneyFilter.ALL,
                onClick = { viewModel.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.ALL)) },
            )
        }
    }

    val journeys = filterJourneys(uiState)
    when {
        journeys.isEmpty() && uiState.journeyFilter == JourneyFilter.THIS_WEEK && uiState.journeySearch.isBlank() -> {
            item {
                NoJourneysThisWeekState(onViewAll = { viewModel.onAction(SavedTracksAction.JourneyFilterSelected(JourneyFilter.ALL)) })
            }
        }
        journeys.isEmpty() -> {
            item {
                EmptyState(
                    title =
                        if (uiState.journeySearch.isNotBlank()) {
                            stringResource(
                                Res.string.tracking_saved_empty_journeys_query,
                            )
                        } else {
                            stringResource(Res.string.tracking_saved_empty_journeys_none)
                        },
                    subtitle =
                        if (uiState.journeySearch.isNotBlank()) {
                            stringResource(
                                Res.string.tracking_saved_empty_search_hint,
                            )
                        } else {
                            stringResource(Res.string.tracking_saved_empty_journeys_hint)
                        },
                )
            }
        }
        else ->
            items(journeys, key = { it.token }) { track ->
                JourneyCard(track = track, onClick = { onTrackClick(track.token) })
            }
    }
}

/**
 * Applies the active Journeys-tab search and chip filter to the track list. "Kept" reuses the
 * submitted flag as the demo's "retained" signal; "This Week" keeps tracks from the last 7 days.
 */
private fun filterJourneys(uiState: SavedTracksUiState): List<TrackDisplayData> {
    val now = Clock.System.now().toEpochMilliseconds()
    val weekAgo = now - 7L * 24 * 60 * 60 * 1000
    return uiState.tracks
        .filter { track ->
            when (uiState.journeyFilter) {
                JourneyFilter.THIS_WEEK -> track.startTime >= weekAgo
                JourneyFilter.KEPT -> track.isSubmitted
                JourneyFilter.ALL -> true
            }
        }
        .filter { track ->
            uiState.journeySearch.isBlank() ||
                (track.name?.contains(uiState.journeySearch, ignoreCase = true) == true) ||
                track.token.contains(uiState.journeySearch, ignoreCase = true)
        }
}

@Composable
private fun JourneyCard(
    track: TrackDisplayData,
    onClick: () -> Unit,
) {
    val score = track.healthScore()
    val scoreColor =
        when {
            score >= 80 -> MilewayColors.success
            score >= 60 -> MilewayColors.warning
            else -> MilewayColors.danger
        }
    val routePoints =
        listOf(
            track.startLatitude to track.startLongitude,
            track.endLatitude to track.endLongitude,
        ).filter { (lat, lng) -> lat != 0.0 || lng != 0.0 }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column {
            // VI.1: 64dp route thumbnail with health score badge overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                StaticPolylineThumbnail(
                    latLngs = if (routePoints.size >= 2) routePoints else emptyList(),
                    thumbHeight = 64.dp,
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(DesignTokens.Spacing.s)
                            .size(32.dp)
                            .clip(DesignTokens.Shape.button)
                            .background(scoreColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.name ?: stringResource(Res.string.tracking_saved_journey_default),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        if (track.startTime > 0) {
                            Text(
                                text = DateUtils.epochToDisplayDate(track.startTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    StatusChip(isSubmitted = track.isSubmitted)
                }

                // Muted from → to place names (offline-resolved), the journey-card anatomy from the
                // reference. Shown only when at least one endpoint resolves to a named waypoint.
                JourneyRouteLine(
                    fromLat = track.startLatitude,
                    fromLng = track.startLongitude,
                    toLat = track.endLatitude,
                    toLng = track.endLongitude,
                )

                val linkedContext = track.toLinkedContext()
                if (linkedContext != null) {
                    Spacer(Modifier.height(DesignTokens.Spacing.m))
                    LinkedContextCard(context = linkedContext, onClick = onClick)
                }

                Spacer(Modifier.height(DesignTokens.Spacing.m))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                ) {
                    Metric(label = stringResource(Res.string.tracking_insights_distance), value = track.getFormattedDistance(), modifier = Modifier.weight(1f))
                    Metric(label = stringResource(Res.string.tracking_insights_duration), value = track.getFormattedDuration(), modifier = Modifier.weight(1f))
                    Metric(
                        label = stringResource(Res.string.tracking_detail_amount),
                        value = if (track.reimbursableAmount > 0) "₹${track.reimbursableAmount.formatDecimal(0)}" else "—",
                        valueColor = if (track.reimbursableAmount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                // VI.1: 4dp quality bar, width proportional to health score
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(DesignTokens.Shape.button)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(score / 100f)
                                .height(4.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(scoreColor),
                    )
                }
            }
        }
    }
}

/**
 * A muted "From → To" line resolving each saved-journey endpoint to an offline place name. Falls
 * back gracefully: if neither endpoint resolves to a named waypoint, the row is omitted entirely.
 */
@Composable
private fun JourneyRouteLine(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
) {
    val resolver = remember { OfflineLocationNameResolver() }
    val from = remember(fromLat, fromLng) { resolver.nameFor(fromLat, fromLng) }
    val to = remember(toLat, toLng) { resolver.nameFor(toLat, toLng) }
    if (from == null && to == null) return

    Spacer(Modifier.height(DesignTokens.Spacing.xs))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MilewayColors.neutral,
            modifier = Modifier.size(DesignTokens.IconSize.inline),
        )
        Text(
            text = stringResource(Res.string.tracking_route_line, shortPlace(from), shortPlace(to)),
            style = MaterialTheme.typography.bodySmall,
            color = MilewayColors.neutral,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** Trim a resolved "<locality>, Pune" label to just the locality for the compact route line. */
@Composable
private fun shortPlace(name: String?): String = name?.substringBefore(",")?.trim() ?: stringResource(Res.string.tracking_unknown_place)

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge.dataStyle(), fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun StatusChip(isSubmitted: Boolean) {
    val (label, color) =
        if (isSubmitted) {
            stringResource(Res.string.tracking_status_submitted) to DesignTokens.StatusColors.success
        } else {
            stringResource(Res.string.tracking_status_saved) to DesignTokens.StatusColors.info
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
    ) {
        if (isSubmitted) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(DesignTokens.IconSize.inline))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Submissions tab
// ─────────────────────────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.submissionsSection(
    uiState: SavedTracksUiState,
    viewModel: SavedTracksViewModel,
) {
    item {
        SavedTracksSearchField(
            query = uiState.submissionSearch,
            placeholder = stringResource(Res.string.tracking_saved_search_submissions),
            onQueryChange = { viewModel.onAction(SavedTracksAction.SubmissionSearchChanged(it)) },
            onFilterClick = null,
        )
    }
    // Primary submission filter chips.
    item {
        SavedTracksChipRow {
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_all_count, uiState.submissionCount),
                selected = uiState.submissionFilter == SubmissionFilter.ALL,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionFilterSelected(SubmissionFilter.ALL)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_unclaimed_count, uiState.unclaimedCount),
                selected = uiState.submissionFilter == SubmissionFilter.UNCLAIMED,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionFilterSelected(SubmissionFilter.UNCLAIMED)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_filed_count, uiState.filedCount),
                selected = uiState.submissionFilter == SubmissionFilter.FILED,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionFilterSelected(SubmissionFilter.FILED)) },
            )
        }
    }
    // Secondary source chips.
    item {
        SavedTracksChipRow {
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_all),
                selected = uiState.submissionSource == SubmissionSource.ALL,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionSourceSelected(SubmissionSource.ALL)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_new_tracker),
                selected = uiState.submissionSource == SubmissionSource.NEW_TRACKER,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionSourceSelected(SubmissionSource.NEW_TRACKER)) },
            )
            SavedTracksFilterChip(
                label = stringResource(Res.string.tracking_saved_filter_other),
                selected = uiState.submissionSource == SubmissionSource.OTHER,
                onClick = { viewModel.onAction(SavedTracksAction.SubmissionSourceSelected(SubmissionSource.OTHER)) },
            )
        }
    }

    // VI.4: Collapsible insights card above the submissions list
    item {
        SubmittedInsightsCard(submissions = uiState.allSubmissions)
    }

    // Selection-mode header + Create Voucher CTA.
    if (uiState.selectionMode && uiState.selectedSubmissionIds.isNotEmpty()) {
        item {
            SubmissionSelectionRow(
                selectedCount = uiState.selectedSubmissionIds.size,
                onClearSelection = { viewModel.onAction(SavedTracksAction.ClearSelection) },
            )
        }
        val voucherCount = uiState.selectedUnclaimedIds.size
        if (voucherCount > 0) {
            item {
                // VI.3: Crossfade count animation on the Create Voucher CTA
                AnimatedContent(
                    targetState = voucherCount,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    label = "voucherCount",
                ) { count ->
                    CreateVoucherButton(count = count, onClick = { viewModel.onAction(SavedTracksAction.CreateVoucher) })
                }
            }
        }
    }

    val submissions = uiState.filteredSubmissions
    if (submissions.isEmpty()) {
        item {
            NoSubmissionsState(
                title =
                    if (uiState.submissionSearch.isNotBlank()) {
                        stringResource(
                            Res.string.tracking_saved_empty_submissions_query,
                        )
                    } else {
                        stringResource(Res.string.tracking_saved_empty_submissions_none)
                    },
                subtitle =
                    if (uiState.submissionSearch.isNotBlank()) {
                        stringResource(
                            Res.string.tracking_saved_empty_search_hint,
                        )
                    } else {
                        stringResource(Res.string.tracking_saved_empty_submissions_hint)
                    },
            )
        }
    } else {
        // Date-grouped: a header per day, then the day's submission cards.
        val grouped = submissions.groupBy { DateUtils.epochToDisplayDate(it.expenseDateMillis) }
        grouped.forEach { (date, daySubmissions) ->
            item(key = "header_$date") {
                SubmissionDateHeader(label = date)
            }
            items(daySubmissions, key = { it.id }) { submission ->
                val haptic = LocalHapticFeedback.current
                SubmissionCard(
                    data = submission.toCardData(),
                    isSelected = submission.id in uiState.selectedSubmissionIds,
                    selectionMode = uiState.selectionMode,
                    onClick = {
                        if (uiState.selectionMode) {
                            viewModel.onAction(SavedTracksAction.SubmissionTapped(submission.id))
                        } else {
                            viewModel.onAction(SavedTracksAction.SubmissionSelectionToggled(submission.id))
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onAction(SavedTracksAction.SubmissionLongPressed(submission.id))
                    },
                )
            }
        }
    }
}

/** Maps the ViewModel's [SubmissionItem] onto the stateless card's render data. */
private fun SubmissionItem.toCardData() =
    SubmissionCardData(
        id = id,
        transId = transId,
        amount = amount,
        expenseDateMillis = expenseDateMillis,
        attachmentCount = attachmentCount,
        violationCount = violationCount,
        acknowledged = acknowledged,
        isNewTracker = isNewTracker,
        voucherCreated = voucherCreated,
        approvalStatus = approvalStatus,
        voucherNumber = voucherNumber,
    )

/** VI.1: Pseudo health score (0–100) for a journey card, derived from available fields. */
private fun TrackDisplayData.healthScore(): Int {
    var score = 70
    val pointDensity = if (distanceKm > 0) locationCount / distanceKm else 0.0
    score +=
        when {
            pointDensity >= 20 -> 20
            pointDensity >= 10 -> 12
            pointDensity >= 5 -> 5
            pointDensity >= 2 -> 0
            else -> -15
        }
    if (locationCount < 5) score -= 20
    if (locationCount >= 50) score += 10
    if (distanceKm > 5) score += 5
    return score.coerceIn(0, 100)
}

/** VI.4: Collapsible weekly-insights banner above the submissions list. */
@Composable
private fun SubmittedInsightsCard(
    submissions: List<SubmissionItem>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val weekMs = 7L * 24 * 3_600_000
    val now = submissions.maxOfOrNull { it.expenseDateMillis } ?: 0L
    val thisWeek = submissions.filter { it.expenseDateMillis >= now - weekMs }
    val weekTotal = thisWeek.sumOf { it.amount }
    val avgPerTrip = if (thisWeek.isNotEmpty()) weekTotal / thisWeek.size else 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        tonalElevation = DesignTokens.Elevation.card,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tracking_saved_week_summary, thisWeek.size, weekTotal.formatDecimal(0)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) stringResource(Res.string.tracking_cd_collapse) else stringResource(Res.string.tracking_cd_expand),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = DesignTokens.Spacing.s),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InsightMetric(label = stringResource(Res.string.tracking_saved_insight_count), value = "${thisWeek.size}", modifier = Modifier.weight(1f))
                    InsightMetric(
                        label = stringResource(Res.string.tracking_voucher_summary_total),
                        value = "₹${weekTotal.formatDecimal(0)}",
                        modifier = Modifier.weight(1f),
                    )
                    InsightMetric(
                        label = stringResource(Res.string.tracking_saved_insight_avg),
                        value = "₹${avgPerTrip.formatDecimal(0)}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.dataStyle(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
    }
}
