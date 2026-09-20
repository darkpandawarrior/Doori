package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.favourite.FavouriteRoute
import com.mileway.core.data.favourite.PinnableTrack
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.FavouriteRoutesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * PLAN_V24 P12.8: favourite routes — the pinned list (rename / unpin) plus a "Pin a route" section
 * over completed trips. Each favourite carries its quick-start classification default (purpose) and
 * a distance cache. Reached from a plugin-gated profile-hub tile. Blocked zones are skipped (see the
 * repository doc + PROGRESS) — routes only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteRoutesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavouriteRoutesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pinTarget by remember { mutableStateOf<PinnableTrack?>(null) }
    var renameTarget by remember { mutableStateOf<FavouriteRoute?>(null) }

    Scaffoldish(onBack = onBack, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            item {
                Text(
                    frv("favourites_pinned", "Pinned routes"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (state.favourites.isEmpty()) {
                item {
                    Text(
                        frv("favourites_none", "No favourite routes yet. Pin a completed trip below."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.favourites, key = { it.id }) { fav ->
                FavouriteCard(
                    favourite = fav,
                    onRename = { renameTarget = fav },
                    onRemove = { viewModel.remove(fav.id) },
                )
            }
            item {
                Text(
                    frv("favourites_pin_a_route", "Pin a route"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.m),
                )
            }
            if (state.pinnable.isEmpty()) {
                item {
                    Text(
                        frv("favourites_no_trips", "Complete a trip to pin it as a favourite route."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.pinnable, key = { it.trackId }) { track ->
                PinnableCard(track = track, onPin = { pinTarget = track })
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    pinTarget?.let { track ->
        NameSheet(
            title = frv("favourites_pin_title", "Name this route"),
            initial = track.name,
            onConfirm = { name ->
                viewModel.pin(track, name)
                pinTarget = null
            },
            onDismiss = { pinTarget = null },
        )
    }
    renameTarget?.let { fav ->
        NameSheet(
            title = frv("favourites_rename_title", "Rename route"),
            initial = fav.name,
            onConfirm = { name ->
                viewModel.rename(fav.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Scaffoldish(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = frv("favourites_back", "Back"))
                }
                Text(
                    frv("favourites_title", "Favourite routes"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            content()
        }
    }
}

@Composable
private fun FavouriteCard(
    favourite: FavouriteRoute,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = DesignTokens.Spacing.s)) {
                Text(favourite.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${favourite.distanceKm.roundToInt()} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (favourite.purpose.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(favourite.purpose) })
                }
            }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, contentDescription = frv("favourites_rename", "Rename")) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = frv("favourites_unpin", "Unpin")) }
        }
    }
}

@Composable
private fun PinnableCard(
    track: PinnableTrack,
    onPin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.name.ifBlank { frv("favourites_untitled_trip", "Trip") }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${track.distanceKm.roundToInt()} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(frv("favourites_pin", "Pin"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameSheet(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l).padding(bottom = DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(frv("favourites_name_label", "Route name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(frv("favourites_cancel", "Cancel")) }
                OutlinedButton(onClick = { onConfirm(name) }, modifier = Modifier.weight(1f)) { Text(frv("favourites_save", "Save")) }
            }
        }
    }
}

/** Screen-internal labels via the dynamic resolver with an English fallback (no generated symbols). */
@Composable
private fun frv(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
