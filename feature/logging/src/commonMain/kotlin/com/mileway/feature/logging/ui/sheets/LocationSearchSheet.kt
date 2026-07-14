package com.mileway.feature.logging.ui.sheets

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.siddharth.kmp.common.formatDecimal
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_clear_all
import com.mileway.core.ui.resources.logging_clear_cd
import com.mileway.core.ui.resources.logging_favorite_cd
import com.mileway.core.ui.resources.logging_favorites
import com.mileway.core.ui.resources.logging_locating
import com.mileway.core.ui.resources.logging_more_actions_cd
import com.mileway.core.ui.resources.logging_no_matches_body
import com.mileway.core.ui.resources.logging_no_matches_title
import com.mileway.core.ui.resources.logging_recent
import com.mileway.core.ui.resources.logging_remove_from_recent
import com.mileway.core.ui.resources.logging_remove_named
import com.mileway.core.ui.resources.logging_save_as_home
import com.mileway.core.ui.resources.logging_save_as_work
import com.mileway.core.ui.resources.logging_saved_places
import com.mileway.core.ui.resources.logging_search_location_placeholder
import com.mileway.core.ui.resources.logging_search_location_title
import com.mileway.core.ui.resources.logging_search_locations_body
import com.mileway.core.ui.resources.logging_search_locations_title
import com.mileway.core.ui.resources.logging_unfavorite_cd
import com.mileway.core.ui.resources.logging_use_current_location
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.ui.model.CityCatalog
import com.mileway.feature.logging.ui.model.LocationEntry
import com.mileway.feature.logging.ui.model.PoiCategory
import com.mileway.feature.logging.ui.model.SavedPlaceUi
import com.mileway.feature.logging.ui.model.haversineKm
import org.jetbrains.compose.resources.stringResource

/** Callbacks the [LocationSearchSheet] routes to the ViewModel — grouped to keep the arg list flat. */
data class LocationSearchActions(
    val onQueryChange: (String) -> Unit,
    val onPick: (LocationEntry) -> Unit,
    val onToggleFavorite: (LocationEntry) -> Unit,
    val onSaveAs: (LocationEntry, String) -> Unit,
    val onRemoveRecent: (LocationEntry) -> Unit,
    val onRemoveSaved: (String) -> Unit,
    val onUseCurrent: () -> Unit,
    val onClearRecent: () -> Unit,
)

/**
 * Modal location-search + switching sheet, backed entirely by offline data.
 *
 * When the query is blank it surfaces the switching shortcuts — a resolved "current location" row,
 * Home/Work/custom saved-place chips, a favorites section, and recents — and once the user types it
 * shows debounced [CityCatalog] results. Every row can be starred (favorite), saved as a named place,
 * or (for recents) removed, via a star toggle plus an overflow menu. Selecting any entry confirms it
 * through [LocationSearchActions.onPick] and dismisses the sheet.
 *
 * State is hoisted: the host `SearchLocationViewModel` owns [query]/[results]/[recent]/[favorites]/
 * [saved]/[currentLocation]; this composable is otherwise stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    query: String,
    results: List<LocationEntry>,
    recent: List<LocationEntry>,
    favorites: List<LocationEntry>,
    saved: List<SavedPlaceUi>,
    favoriteNames: Set<String>,
    currentLocation: LocationEntry?,
    isLoadingCurrent: Boolean,
    actions: LocationSearchActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBrowse = query.isBlank()

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
                text = stringResource(Res.string.logging_search_location_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            OutlinedTextField(
                value = query,
                onValueChange = actions.onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.logging_search_location_placeholder)) },
                singleLine = true,
                shape = DesignTokens.Shape.roundedMd,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { actions.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.logging_clear_cd))
                        }
                    }
                },
            )

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            CurrentLocationRow(
                currentLocation = currentLocation,
                isLoading = isLoadingCurrent,
                onUseCurrent = actions.onUseCurrent,
            )

            if (showBrowse && saved.isNotEmpty()) {
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                SavedChipsRow(saved = saved, actions = actions)
            }

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            when {
                !showBrowse && results.isEmpty() ->
                    EmptyHint(
                        title = stringResource(Res.string.logging_no_matches_title),
                        body = stringResource(Res.string.logging_no_matches_body),
                    )

                !showBrowse ->
                    LocationList(
                        entries = results,
                        favoriteNames = favoriteNames,
                        actions = actions,
                        maxHeight = 360.dp,
                    )

                favorites.isEmpty() && recent.isEmpty() ->
                    EmptyHint(
                        title = stringResource(Res.string.logging_search_locations_title),
                        body = stringResource(Res.string.logging_search_locations_body),
                    )

                else ->
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                    ) {
                        if (favorites.isNotEmpty()) {
                            item { SectionHeader(icon = Icons.Filled.Star, title = stringResource(Res.string.logging_favorites)) }
                            items(favorites, key = { "fav-${it.name}" }) { entry ->
                                LocationRow(
                                    entry = entry,
                                    isFavorite = true,
                                    actions = actions,
                                    onRemove = null,
                                )
                            }
                        }
                        if (recent.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    icon = Icons.Filled.History,
                                    title = stringResource(Res.string.logging_recent),
                                    trailing = {
                                        TextButton(
                                            onClick = actions.onClearRecent,
                                            shape = DesignTokens.Shape.button,
                                        ) { Text(stringResource(Res.string.logging_clear_all)) }
                                    },
                                )
                            }
                            items(recent, key = { "recent-${it.name}" }) { entry ->
                                LocationRow(
                                    entry = entry,
                                    isFavorite = entry.name in favoriteNames,
                                    actions = actions,
                                    onRemove = { actions.onRemoveRecent(entry) },
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun CurrentLocationRow(
    currentLocation: LocationEntry?,
    isLoading: Boolean,
    onUseCurrent: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        onClick = onUseCurrent,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.logging_use_current_location),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    currentLocation?.subtitle ?: stringResource(Res.string.logging_locating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SavedChipsRow(
    saved: List<SavedPlaceUi>,
    actions: LocationSearchActions,
) {
    Column {
        SectionHeader(icon = Icons.Filled.Home, title = stringResource(Res.string.logging_saved_places))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            items(saved, key = { it.label }) { item ->
                AssistChip(
                    onClick = { actions.onPick(item.entry) },
                    label = { Text(item.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = savedLabelIcon(item.label),
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { actions.onRemoveSaved(item.label) }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(Res.string.logging_remove_named, item.label),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LocationList(
    entries: List<LocationEntry>,
    favoriteNames: Set<String>,
    actions: LocationSearchActions,
    maxHeight: androidx.compose.ui.unit.Dp,
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = maxHeight),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        items(entries, key = { it.name }) { entry ->
            LocationRow(
                entry = entry,
                isFavorite = entry.name in favoriteNames,
                actions = actions,
                onRemove = null,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSize.inline),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun LocationRow(
    entry: LocationEntry,
    isFavorite: Boolean,
    actions: LocationSearchActions,
    onRemove: (() -> Unit)?,
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
        onClick = { actions.onPick(entry) },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Surface(shape = DesignTokens.Shape.button, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
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
            IconButton(onClick = { actions.onToggleFavorite(entry) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isFavorite) stringResource(Res.string.logging_unfavorite_cd) else stringResource(Res.string.logging_favorite_cd),
                    tint =
                        if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(20.dp),
                )
            }
            RowOverflowMenu(entry = entry, actions = actions, onRemove = onRemove)
        }
    }
}

@Composable
private fun RowOverflowMenu(
    entry: LocationEntry,
    actions: LocationSearchActions,
    onRemove: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = stringResource(Res.string.logging_more_actions_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.logging_save_as_home)) },
            leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
            onClick = {
                actions.onSaveAs(entry, "Home")
                expanded = false
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.logging_save_as_work)) },
            leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null) },
            onClick = {
                actions.onSaveAs(entry, "Work")
                expanded = false
            },
        )
        if (onRemove != null) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.logging_remove_from_recent)) },
                leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null) },
                onClick = {
                    onRemove()
                    expanded = false
                },
            )
        }
    }
}

private fun savedLabelIcon(label: String) =
    when (label.lowercase()) {
        "home" -> Icons.Filled.Home
        "work", "office" -> Icons.Filled.Work
        else -> Icons.Filled.LocationOn
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
