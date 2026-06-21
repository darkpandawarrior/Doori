package com.miletracker.feature.logging.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.feature.logging.ui.model.CityCatalog
import com.miletracker.feature.logging.ui.model.LocationEntry
import com.miletracker.feature.logging.ui.model.PoiCategory
import com.miletracker.feature.logging.ui.model.haversineKm

/**
 * Modal location-search sheet backed by the offline [CityCatalog].
 *
 * Mirrors the reference "Search Location" sheet: a search field, a "Current"
 * shortcut, a Recent section (with clear-all and per-item remove), and a result
 * list once the query is at least two characters. Selecting any entry confirms
 * it back to the caller via [onPick] and dismisses the sheet.
 *
 * @param recent           recent picks surfaced when the query is empty
 * @param onPick           called with the chosen place
 * @param onUseCurrent     called when the user taps the "Current" shortcut
 * @param onClearRecent    called when the user clears the recent list
 * @param onDismiss        called when the sheet is dismissed without a pick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    recent: List<LocationEntry>,
    onPick: (LocationEntry) -> Unit,
    onUseCurrent: () -> Unit,
    onClearRecent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val results = remember(query) { CityCatalog.search(query) }
    val showRecent = query.isBlank() && recent.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = DesignTokens.Shape.sheet,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.l),
        ) {
            Text(
                text = "Search Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a location") },
                singleLine = true,
                shape = DesignTokens.Shape.roundedMd,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            // "Use Current Location" row, picks a deterministic demo coordinate.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedLg,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                onClick = {
                    onPick(CityCatalog.currentLocation)
                    onUseCurrent()
                },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your current location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            CityCatalog.currentLocation.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            when {
                showRecent ->
                    RecentSection(
                        recent = recent,
                        onPick = onPick,
                        onClearAll = onClearRecent,
                    )

                query.isNotBlank() && results.isEmpty() ->
                    EmptyHint(
                        title = "Search for locations",
                        body = "Type at least 2 characters to find places near you",
                    )

                query.isBlank() ->
                    EmptyHint(
                        title = "Search for locations",
                        body = "Type at least 2 characters to find places near you",
                    )

                else ->
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                    ) {
                        items(results, key = { it.name }) { entry ->
                            LocationRow(entry = entry, onClick = { onPick(entry) })
                        }
                    }
            }
        }
    }
}

@Composable
private fun RecentSection(
    recent: List<LocationEntry>,
    onPick: (LocationEntry) -> Unit,
    onClearAll: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onClearAll) { Text("Clear all") }
        }
        LazyColumn(
            modifier = Modifier.heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            items(recent, key = { it.name }) { entry ->
                LocationRow(entry = entry, onClick = { onPick(entry) })
            }
        }
    }
}

@Composable
private fun LocationRow(
    entry: LocationEntry,
    onClick: () -> Unit,
) {
    val distKm =
        haversineKm(
            CityCatalog.currentLocation.lat,
            CityCatalog.currentLocation.lng,
            entry.lat,
            entry.lng,
        )
    val distLabel =
        when {
            distKm < 1.0 -> "${(distKm * 1000).toInt()} m"
            else -> "${distKm.formatDecimal(1)} km"
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Icon(
                    imageVector = entry.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    entry.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = distLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun PoiCategory.icon() =
    when (this) {
        PoiCategory.OFFICE -> Icons.Filled.Business
        PoiCategory.CLIENT -> Icons.Filled.DirectionsCar
        PoiCategory.RESTAURANT -> Icons.Filled.Restaurant
        PoiCategory.HOME -> Icons.Filled.Home
        PoiCategory.TRANSIT -> Icons.Filled.Train
        PoiCategory.LANDMARK, PoiCategory.OTHER -> Icons.Filled.LocationOn
    }

@Composable
private fun EmptyHint(
    title: String,
    body: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
