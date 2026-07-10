package com.mileway.feature.travel.ui.screens

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.common.asString
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_active_trip_label
import com.mileway.core.ui.resources.travel_active_trips
import com.mileway.core.ui.resources.travel_boarding_suffix
import com.mileway.core.ui.resources.travel_book_flight
import com.mileway.core.ui.resources.travel_book_train
import com.mileway.core.ui.resources.travel_gate_suffix
import com.mileway.core.ui.resources.travel_header_subtitle
import com.mileway.core.ui.resources.travel_header_title
import com.mileway.core.ui.resources.travel_itin_day1_item1_subtitle
import com.mileway.core.ui.resources.travel_itin_day1_item1_title
import com.mileway.core.ui.resources.travel_itin_day1_item2_subtitle
import com.mileway.core.ui.resources.travel_itin_day1_item2_title
import com.mileway.core.ui.resources.travel_itin_day1_item3_subtitle
import com.mileway.core.ui.resources.travel_itin_day1_item3_title
import com.mileway.core.ui.resources.travel_itin_day1_label
import com.mileway.core.ui.resources.travel_itin_day2_item1_subtitle
import com.mileway.core.ui.resources.travel_itin_day2_item1_title
import com.mileway.core.ui.resources.travel_itin_day2_item2_subtitle
import com.mileway.core.ui.resources.travel_itin_day2_item2_title
import com.mileway.core.ui.resources.travel_itin_day2_label
import com.mileway.core.ui.resources.travel_itin_day3_item1_subtitle
import com.mileway.core.ui.resources.travel_itin_day3_item1_title
import com.mileway.core.ui.resources.travel_itin_day3_item2_subtitle
import com.mileway.core.ui.resources.travel_itin_day3_item2_title
import com.mileway.core.ui.resources.travel_itin_day3_label
import com.mileway.core.ui.resources.travel_on_time_chip
import com.mileway.core.ui.resources.travel_policy_notice
import com.mileway.core.ui.resources.travel_tab_bookings
import com.mileway.core.ui.resources.travel_tab_itinerary
import com.mileway.core.ui.resources.travel_total_spend
import com.mileway.core.ui.resources.travel_upcoming
import com.mileway.core.ui.resources.travel_upcoming_bookings_title
import com.mileway.core.ui.resources.travel_view_boarding_pass
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.travel.model.BookingRecord
import com.mileway.feature.travel.model.TransportMode
import com.mileway.feature.travel.viewmodel.TravelAction
import com.mileway.feature.travel.viewmodel.TravelEffect
import com.mileway.feature.travel.viewmodel.TravelViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val TealGradient =
    Brush.horizontalGradient(
        colors = listOf(Color(0xFF00695C), Color(0xFF00BCD4)),
    )

// ==========================================
// Itinerary mock data (Phase P)
// ==========================================

private enum class ItineraryType { FLIGHT, HOTEL, MEETING, DINING }

private data class ItineraryItem(
    val icon: ImageVector,
    val title: StringResource,
    val subtitle: StringResource,
    val type: ItineraryType,
)

private data class ItineraryDay(val label: StringResource, val items: List<ItineraryItem>)

/** Sample itinerary shown on the Itinerary tab (Phase P). */
private val ITINERARY =
    listOf(
        ItineraryDay(
            label = Res.string.travel_itin_day1_label,
            items =
                listOf(
                    ItineraryItem(
                        Icons.Filled.AirplanemodeActive,
                        Res.string.travel_itin_day1_item1_title,
                        Res.string.travel_itin_day1_item1_subtitle,
                        ItineraryType.FLIGHT,
                    ),
                    ItineraryItem(
                        Icons.Filled.MeetingRoom,
                        Res.string.travel_itin_day1_item2_title,
                        Res.string.travel_itin_day1_item2_subtitle,
                        ItineraryType.HOTEL,
                    ),
                    ItineraryItem(
                        Icons.Filled.MeetingRoom,
                        Res.string.travel_itin_day1_item3_title,
                        Res.string.travel_itin_day1_item3_subtitle,
                        ItineraryType.MEETING,
                    ),
                ),
        ),
        ItineraryDay(
            label = Res.string.travel_itin_day2_label,
            items =
                listOf(
                    ItineraryItem(
                        Icons.Filled.MeetingRoom,
                        Res.string.travel_itin_day2_item1_title,
                        Res.string.travel_itin_day2_item1_subtitle,
                        ItineraryType.MEETING,
                    ),
                    ItineraryItem(
                        Icons.Filled.Restaurant,
                        Res.string.travel_itin_day2_item2_title,
                        Res.string.travel_itin_day2_item2_subtitle,
                        ItineraryType.DINING,
                    ),
                ),
        ),
        ItineraryDay(
            label = Res.string.travel_itin_day3_label,
            items =
                listOf(
                    ItineraryItem(
                        Icons.Filled.MeetingRoom,
                        Res.string.travel_itin_day3_item1_title,
                        Res.string.travel_itin_day3_item1_subtitle,
                        ItineraryType.MEETING,
                    ),
                    ItineraryItem(
                        Icons.Filled.AirplanemodeActive,
                        Res.string.travel_itin_day3_item2_title,
                        Res.string.travel_itin_day3_item2_subtitle,
                        ItineraryType.FLIGHT,
                    ),
                ),
        ),
    )

private fun itineraryTypeColor(type: ItineraryType): Color =
    when (type) {
        ItineraryType.FLIGHT -> Color(0xFF1565C0)
        ItineraryType.HOTEL -> Color(0xFF6A1B9A)
        ItineraryType.MEETING -> Color(0xFF00695C)
        ItineraryType.DINING -> Color(0xFFE65100)
    }

// ==========================================
// Screen
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelHomeScreen(viewModel: TravelViewModel = koinViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TravelEffect.ShowMessage ->
                    scope.launch { snackbarHostState.showSnackbar(effect.message.asString()) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ScreenStateContent(
            state = uiState.content,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            onRetry = { viewModel.onAction(TravelAction.Refresh) },
        ) { data ->
            Column(modifier = Modifier.fillMaxSize()) {
                TravelHeader()
                SummaryStrip(
                    activeTripCount = if (data.activeBooking != null) 1 else 0,
                    upcomingCount = data.upcoming.size,
                    totalSpend = data.totalSpend,
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    listOf(
                        stringResource(Res.string.travel_tab_bookings) to Icons.Filled.ConfirmationNumber,
                        stringResource(Res.string.travel_tab_itinerary) to Icons.Filled.MeetingRoom,
                    ).forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
                when (selectedTab) {
                    0 ->
                        BookingsTab(
                            activeBooking = data.activeBooking,
                            upcoming = data.upcoming,
                            onAction = viewModel::onAction,
                        )
                    1 -> ItineraryTab()
                }
            }
        }
    }
}

@Composable
private fun BookingsTab(
    activeBooking: BookingRecord?,
    upcoming: List<BookingRecord>,
    onAction: (TravelAction) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (activeBooking != null) {
            item {
                Spacer(Modifier.height(DesignTokens.Spacing.m))
                ActiveTripCard(
                    booking = activeBooking,
                    onViewBoardingPass = { onAction(TravelAction.ViewBoardingPass) },
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
                )
            }
        }
        item {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text(
                text = stringResource(Res.string.travel_upcoming_bookings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
        }
        items(upcoming.size) { index ->
            UpcomingBookingCard(
                booking = upcoming[index],
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
            )
            if (index < upcoming.size - 1) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
            }
        }
        item {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            QuickActionsRow(
                onBookFlight = { onAction(TravelAction.BookFlight) },
                onBookTrain = { onAction(TravelAction.BookTrain) },
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
            )
        }
        item {
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            TravelPolicyCard(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l))
            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}

@Composable
private fun ItineraryTab() {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = DesignTokens.Spacing.l,
                vertical = DesignTokens.Spacing.m,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        ITINERARY.forEach { day ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                ) {
                    Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                        Text(
                            text = stringResource(day.label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(DesignTokens.Spacing.m))
                        day.items.forEachIndexed { idx, itItem ->
                            ItineraryItemRow(item = itItem)
                            if (idx < day.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = DesignTokens.Spacing.s, bottom = DesignTokens.Spacing.s, start = 36.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(DesignTokens.Spacing.xl)) }
    }
}

@Composable
private fun ItineraryItemRow(item: ItineraryItem) {
    val typeColor = itineraryTypeColor(item.type)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 3.dp, height = 44.dp)
                    .background(typeColor, DesignTokens.Shape.button),
        )
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(typeColor.copy(alpha = 0.12f), DesignTokens.Shape.button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(item.title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(stringResource(item.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// Shared sub-composables
// ==========================================

@Composable
private fun TravelHeader() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(TealGradient)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.xl),
    ) {
        Column {
            Text(
                text = stringResource(Res.string.travel_header_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(Res.string.travel_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SummaryStrip(
    activeTripCount: Int,
    upcomingCount: Int,
    totalSpend: Double,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricItem(label = stringResource(Res.string.travel_active_trips), value = activeTripCount.toString())
            VerticalDivider()
            MetricItem(label = stringResource(Res.string.travel_upcoming), value = upcomingCount.toString())
            VerticalDivider()
            MetricItem(label = stringResource(Res.string.travel_total_spend), value = "₹${totalSpend.toLong()}")
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerticalDivider() {
    HorizontalDivider(
        modifier = Modifier.size(width = 1.dp, height = 36.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

@Composable
private fun ActiveTripCard(
    booking: BookingRecord,
    onViewBoardingPass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.travel_active_trip_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(Res.string.travel_on_time_chip),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MilewayColors.success,
                            labelColor = Color.White,
                        ),
                    border = null,
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = transportIcon(booking.mode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(
                    text = "${booking.origin} → ${booking.destination}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text =
                    "${booking.carrier} ${booking.flightOrTrainNumber}" +
                        (if (booking.boardingTime != null) stringResource(Res.string.travel_boarding_suffix, booking.boardingTime) else "") +
                        (if (booking.gate != null) stringResource(Res.string.travel_gate_suffix, booking.gate) else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))
            OutlinedButton(
                onClick = onViewBoardingPass,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(
                    imageVector = Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    modifier = Modifier.size(androidx.compose.material3.ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.travel_view_boarding_pass))
            }
        }
    }
}

@Composable
private fun UpcomingBookingCard(
    booking: BookingRecord,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            DesignTokens.Shape.button,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = transportIcon(booking.mode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${booking.origin} → ${booking.destination}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${booking.carrier} · ${DateUtils.epochToDisplayDate(booking.departureMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = DesignTokens.Shape.button,
            ) {
                Text(
                    text = "₹${booking.amountRupees.toLong()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onBookFlight: () -> Unit,
    onBookTrain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        OutlinedButton(onClick = onBookFlight, modifier = Modifier.weight(1f), shape = DesignTokens.Shape.button) {
            Icon(Icons.Filled.AirplanemodeActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(Res.string.travel_book_flight))
        }
        OutlinedButton(onClick = onBookTrain, modifier = Modifier.weight(1f), shape = DesignTokens.Shape.button) {
            Icon(Icons.Filled.DirectionsRailway, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(Res.string.travel_book_train))
        }
    }
}

@Composable
private fun TravelPolicyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.travel_policy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun transportIcon(mode: TransportMode): ImageVector =
    when (mode) {
        TransportMode.FLIGHT -> Icons.Filled.AirplanemodeActive
        TransportMode.TRAIN -> Icons.Filled.DirectionsRailway
        TransportMode.BUS -> Icons.Filled.DirectionsBus
        TransportMode.CAB -> Icons.Filled.DirectionsBus
    }
