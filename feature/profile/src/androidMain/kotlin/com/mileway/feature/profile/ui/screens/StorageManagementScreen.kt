package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mileway.core.data.settings.StorageArea
import com.mileway.core.data.settings.StorageTier
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.StorageAreaUi
import com.mileway.feature.profile.viewmodel.StorageManagementViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * P31.MISC.2: the full storage-management screen — every clearable on-device area, tiered
 * Safe/Caution/Danger (see [StorageArea]). Safe clears immediately; Caution/Danger route through
 * [ActionConfirmationBottomSheet] first, matching the app's confirmation-over-dialog convention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagementScreen(
    onBack: () -> Unit,
    viewModel: StorageManagementViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pendingArea by remember { mutableStateOf<StorageArea?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            items(state.areas, key = { it.area.id }) { areaUi ->
                StorageAreaRow(
                    areaUi = areaUi,
                    isClearing = state.clearingAreaId == areaUi.area.id,
                    onClear = {
                        if (areaUi.area.tier == StorageTier.SAFE) {
                            viewModel.clearArea(areaUi.area.id)
                        } else {
                            pendingArea = areaUi.area
                        }
                    },
                )
            }
        }
    }

    pendingArea?.let { area ->
        ActionConfirmationBottomSheet(
            title = "Clear ${area.label.lowercase()}?",
            description = area.description,
            confirmLabel = "Clear",
            icon = Icons.Filled.Warning,
            tone = if (area.tier == StorageTier.DANGER) ActionConfirmationToneType.Danger else ActionConfirmationToneType.Warning,
            onConfirm = {
                viewModel.clearArea(area.id)
                pendingArea = null
            },
            onDismiss = { pendingArea = null },
        )
    }
}

@Composable
private fun StorageAreaRow(
    areaUi: StorageAreaUi,
    isClearing: Boolean,
    onClear: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(text = areaUi.area.label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = areaUi.area.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = areaUi.sizeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = tierColor(areaUi.area.tier),
            )
            Button(onClick = onClear, enabled = !isClearing) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Text(if (isClearing) "Clearing…" else "Clear")
            }
        }
    }
}

@Composable
private fun tierColor(tier: StorageTier) =
    when (tier) {
        StorageTier.SAFE -> DesignTokens.StatusColors.success
        StorageTier.CAUTION -> Color(0xFFF59E0B)
        StorageTier.DANGER -> MaterialTheme.colorScheme.error
    }
