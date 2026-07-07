package com.mileway.feature.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.media_delete_confirm
import com.mileway.core.ui.resources.media_delete_description
import com.mileway.core.ui.resources.media_library_subtitle
import com.mileway.core.ui.resources.media_library_title
import com.mileway.core.ui.resources.media_plural_delete_items
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.media.viewmodel.CloudLibraryAction
import com.mileway.feature.media.viewmodel.CloudLibraryViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLibraryScreen(
    onNavigateBack: () -> Unit,
    onSelectUri: ((String) -> Unit)? = null,
    viewModel: CloudLibraryViewModel = koinViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val selectedIds = remember { mutableStateSetOf<String>() }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.media_library_title),
                subtitle = stringResource(Res.string.media_library_subtitle),
                titleIcon = Icons.Default.PhotoLibrary,
                depth = DesignTokens.NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.core_cd_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectionMode && selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    text = { Text("Delete (${selectedIds.size})") },
                    shape = DesignTokens.Shape.button,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No media saved yet.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Attachments you capture will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding =
                    PaddingValues(
                        start = 4.dp,
                        end = 4.dp,
                        top = innerPadding.calculateTopPadding() + 4.dp,
                        bottom = innerPadding.calculateBottomPadding() + 4.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.id }) { entry ->
                    LibraryThumbnail(
                        entry = entry,
                        isSelected = entry.id in selectedIds,
                        onTap = {
                            if (selectionMode) {
                                if (entry.id in selectedIds) {
                                    selectedIds.remove(entry.id)
                                } else {
                                    selectedIds.add(entry.id)
                                }
                                if (selectedIds.isEmpty()) selectionMode = false
                            } else if (onSelectUri != null) {
                                onSelectUri(entry.uri)
                            }
                        },
                        onLongPress = {
                            selectionMode = true
                            selectedIds.add(entry.id)
                        },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ActionConfirmationBottomSheet(
            title = pluralStringResource(Res.plurals.media_plural_delete_items, selectedIds.size, selectedIds.size),
            description = stringResource(Res.string.media_delete_description),
            confirmLabel = stringResource(Res.string.media_delete_confirm),
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                val toDelete = entries.filter { it.id in selectedIds }
                toDelete.forEach { viewModel.onAction(CloudLibraryAction.Delete(it)) }
                selectedIds.clear()
                selectionMode = false
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun LibraryThumbnail(
    entry: MediaLibraryEntry,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val shape = DesignTokens.Shape.button
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    },
                ),
    ) {
        AsyncImage(
            model = entry.uri,
            contentDescription = entry.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF6367FA).copy(alpha = 0.35f)),
            )
        }
    }
}
