package com.mileway.feature.profile.ui.screens

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_saved_places_add_cd
import com.mileway.core.ui.resources.profile_saved_places_add_title
import com.mileway.core.ui.resources.profile_saved_places_address
import com.mileway.core.ui.resources.profile_saved_places_back
import com.mileway.core.ui.resources.profile_saved_places_cancel
import com.mileway.core.ui.resources.profile_saved_places_delete
import com.mileway.core.ui.resources.profile_saved_places_edit_title
import com.mileway.core.ui.resources.profile_saved_places_empty
import com.mileway.core.ui.resources.profile_saved_places_label
import com.mileway.core.ui.resources.profile_saved_places_lat
import com.mileway.core.ui.resources.profile_saved_places_lng
import com.mileway.core.ui.resources.profile_saved_places_save
import com.mileway.core.ui.resources.profile_saved_places_subtitle
import com.mileway.core.ui.resources.profile_saved_places_title
import com.mileway.core.ui.resources.profile_saved_places_type
import com.mileway.core.ui.resources.profile_saved_places_type_home
import com.mileway.core.ui.resources.profile_saved_places_type_other
import com.mileway.core.ui.resources.profile_saved_places_type_work
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.model.SavedPlace
import com.mileway.feature.profile.model.SavedPlaceType
import com.mileway.feature.profile.viewmodel.SavedPlacesViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P3.4: home/work/other saved places. Room-backed list grouped by type, with an add/edit
 * bottom sheet (label + address + optional manual coordinates) and delete. Reachable from the
 * Account hub's plugin-gated Saved Places tile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedPlacesViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    var editing by remember { mutableStateOf<SavedPlace?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showSheet = false
            editing = null
            viewModel.clearSubmitError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB45309), Color(0xFF7C2D12))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_saved_places_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_saved_places_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_saved_places_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    IconButton(onClick = {
                        editing = null
                        showSheet = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.profile_saved_places_add_cd), tint = Color.White)
                    }
                }
            }

            if (uiState.places.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.profile_saved_places_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    items(
                        uiState.places,
                    ) { place ->
                        SavedPlaceCard(
                            place = place,
                            onEdit = {
                                editing = place
                                showSheet = true
                            },
                            onDelete = { viewModel.delete(place.id) },
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { closeSheet() }, sheetState = sheetState) {
            SavedPlaceEditor(
                initial = editing,
                submitError = uiState.submitError,
                onSave = { type, label, address, lat, lng ->
                    val accepted =
                        viewModel.save(
                            id = editing?.id ?: "",
                            type = type,
                            label = label,
                            address = address,
                            latText = lat,
                            lngText = lng,
                        )
                    if (accepted) closeSheet()
                },
                onCancel = { closeSheet() },
            )
        }
    }
}

@Composable
private fun SavedPlaceCard(
    place: SavedPlace,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${stringResource(place.type.labelRes())} · ${place.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (place.lat != null && place.lng != null) {
                    Text(
                        "${place.lat}, ${place.lng}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.profile_saved_places_edit_title))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.profile_saved_places_delete))
            }
        }
    }
}

@Composable
private fun SavedPlaceEditor(
    initial: SavedPlace?,
    submitError: String?,
    onSave: (SavedPlaceType, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember { mutableStateOf(initial?.type ?: SavedPlaceType.HOME) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var lat by remember { mutableStateOf(initial?.lat?.toString() ?: "") }
    var lng by remember { mutableStateOf(initial?.lng?.toString() ?: "") }

    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Text(
            stringResource(if (initial == null) Res.string.profile_saved_places_add_title else Res.string.profile_saved_places_edit_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(Res.string.profile_saved_places_type), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            SavedPlaceType.entries.forEach { option ->
                FilterChip(
                    selected = type == option,
                    onClick = { type = option },
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(Res.string.profile_saved_places_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(Res.string.profile_saved_places_address)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            OutlinedTextField(
                value = lat,
                onValueChange = { lat = it },
                label = { Text(stringResource(Res.string.profile_saved_places_lat)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lng,
                onValueChange = { lng = it },
                label = { Text(stringResource(Res.string.profile_saved_places_lng)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (submitError != null) {
            Text(submitError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_saved_places_cancel))
            }
            Button(onClick = { onSave(type, label, address, lat, lng) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_saved_places_save))
            }
        }
        Spacer(Modifier.height(DesignTokens.Spacing.m))
    }
}

private fun SavedPlaceType.labelRes(): StringResource =
    when (this) {
        SavedPlaceType.HOME -> Res.string.profile_saved_places_type_home
        SavedPlaceType.WORK -> Res.string.profile_saved_places_type_work
        SavedPlaceType.OTHER -> Res.string.profile_saved_places_type_other
    }
