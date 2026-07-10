package com.mileway.feature.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.ui.components.ZoomImageViewer
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.media_cd_attachment_preview
import com.mileway.core.ui.resources.media_delete_confirm
import com.mileway.core.ui.resources.media_delete_description
import com.mileway.core.ui.resources.media_delete_selected_count
import com.mileway.core.ui.resources.media_library_cd_favorite
import com.mileway.core.ui.resources.media_library_cd_sort
import com.mileway.core.ui.resources.media_library_cd_unfavorite
import com.mileway.core.ui.resources.media_library_cd_zoom
import com.mileway.core.ui.resources.media_library_confirm_selection
import com.mileway.core.ui.resources.media_library_empty_subtitle
import com.mileway.core.ui.resources.media_library_empty_title
import com.mileway.core.ui.resources.media_library_filter_all
import com.mileway.core.ui.resources.media_library_filter_favorites
import com.mileway.core.ui.resources.media_library_filter_images
import com.mileway.core.ui.resources.media_library_filter_pdfs
import com.mileway.core.ui.resources.media_library_filter_with_ocr
import com.mileway.core.ui.resources.media_library_selected_count
import com.mileway.core.ui.resources.media_library_sort_newest
import com.mileway.core.ui.resources.media_library_sort_oldest
import com.mileway.core.ui.resources.media_library_sort_recently_accessed
import com.mileway.core.ui.resources.media_library_subtitle
import com.mileway.core.ui.resources.media_library_title
import com.mileway.core.ui.resources.media_plural_delete_items
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.media.model.MediaLibraryFilter
import com.mileway.feature.media.model.MediaLibrarySort
import com.mileway.feature.media.model.SelectionConfig
import com.mileway.feature.media.viewmodel.CloudLibraryAction
import com.mileway.feature.media.viewmodel.CloudLibraryViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.lazy.items as lazyRowItems

/**
 * The saved-attachment grid.
 *
 * Two independent modes, both always available:
 * - **Browse** (default): long-press enters a delete-selection mode; a single tap otherwise
 *   forwards the uri via [onSelectUri] (kept for the existing single-pick call sites).
 * - **Picker** (V26 P26.LIB.4, when [selectionConfig] is non-null): tap toggles selection up to
 *   [SelectionConfig.maxCount]; entries whose mime type isn't in [SelectionConfig.allowedMimeTypes]
 *   are dimmed and unselectable. A bottom bar shows "x of y selected" + Confirm, calling
 *   [onConfirmSelection] with the chosen entries.
 *
 * The FilterChip row + sort menu (V26 P26.LIB.1) and per-tile favorite/full-screen-zoom actions
 * (V26 P26.LIB.2/.3) apply in both modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLibraryScreen(
    onNavigateBack: () -> Unit,
    onSelectUri: ((String) -> Unit)? = null,
    selectionConfig: SelectionConfig? = null,
    onConfirmSelection: ((List<MediaLibraryEntry>) -> Unit)? = null,
    viewModel: CloudLibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val entries = state.entries
    val selectedIds = remember { mutableStateSetOf<String>() }
    var deleteSelectionMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var viewerEntry by remember { mutableStateOf<MediaLibraryEntry?>(null) }

    fun toggleSelection(entry: MediaLibraryEntry) {
        when {
            entry.id in selectedIds -> selectedIds.remove(entry.id)
            selectionConfig == null -> Unit
            selectionConfig.maxCount <= 1 -> {
                selectedIds.clear()
                selectedIds.add(entry.id)
            }
            selectedIds.size < selectionConfig.maxCount -> selectedIds.add(entry.id)
            // ponytail: silently ignores taps past maxCount; add a "max N selected" snackbar if
            // this becomes a real complaint.
        }
    }

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
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(Res.string.media_library_cd_sort))
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortOption(
                                label = stringResource(Res.string.media_library_sort_newest),
                                selected = state.sort == MediaLibrarySort.NewestFirst,
                                onClick = {
                                    viewModel.onAction(CloudLibraryAction.SetSort(MediaLibrarySort.NewestFirst))
                                    sortMenuExpanded = false
                                },
                            )
                            SortOption(
                                label = stringResource(Res.string.media_library_sort_oldest),
                                selected = state.sort == MediaLibrarySort.OldestFirst,
                                onClick = {
                                    viewModel.onAction(CloudLibraryAction.SetSort(MediaLibrarySort.OldestFirst))
                                    sortMenuExpanded = false
                                },
                            )
                            SortOption(
                                label = stringResource(Res.string.media_library_sort_recently_accessed),
                                selected = state.sort == MediaLibrarySort.RecentlyAccessed,
                                onClick = {
                                    viewModel.onAction(CloudLibraryAction.SetSort(MediaLibrarySort.RecentlyAccessed))
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectionConfig == null && deleteSelectionMode && selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    text = { Text(stringResource(Res.string.media_delete_selected_count, selectedIds.size)) },
                    shape = DesignTokens.Shape.button,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        bottomBar = {
            if (selectionConfig != null) {
                SelectionConfirmBar(
                    selectedCount = selectedIds.size,
                    maxCount = selectionConfig.maxCount,
                    onConfirm = {
                        onConfirmSelection?.invoke(entries.filter { it.id in selectedIds })
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
        ) {
            FilterChipRow(
                filter = state.filter,
                onSelect = { viewModel.onAction(CloudLibraryAction.SetFilter(it)) },
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            stringResource(Res.string.media_library_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.media_library_empty_subtitle),
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
                            top = 4.dp,
                            bottom = innerPadding.calculateBottomPadding() + 4.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val isSelectable = selectionConfig == null || selectionConfig.accepts(entry)
                        LibraryThumbnail(
                            entry = entry,
                            isSelected = entry.id in selectedIds,
                            isSelectable = isSelectable,
                            onTap = {
                                when {
                                    selectionConfig != null -> toggleSelection(entry)
                                    deleteSelectionMode -> {
                                        toggleSelection(entry)
                                        if (selectedIds.isEmpty()) deleteSelectionMode = false
                                    }
                                    onSelectUri != null -> onSelectUri(entry.uri)
                                }
                            },
                            onLongPress = {
                                if (selectionConfig == null) {
                                    deleteSelectionMode = true
                                    selectedIds.add(entry.id)
                                }
                            },
                            onToggleFavorite = { viewModel.onAction(CloudLibraryAction.ToggleFavorite(entry)) },
                            onOpenZoom = {
                                viewModel.onAction(CloudLibraryAction.Viewed(entry))
                                viewerEntry = entry
                            },
                        )
                    }
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
                deleteSelectionMode = false
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    // V26 P26.LIB.3: full-screen zoomable preview for a tapped tile's expand icon.
    viewerEntry?.let { entry ->
        ZoomImageViewer(
            painter = rememberAsyncImagePainter(model = entry.uri),
            contentDescription = stringResource(Res.string.media_cd_attachment_preview),
            onDismiss = { viewerEntry = null },
        )
    }
}

@Composable
private fun SortOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = if (selected) FontWeight.Bold else null) },
        onClick = onClick,
        leadingIcon =
            if (selected) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
                null
            },
    )
}

/** V26 P26.LIB.1: the All/Images/PDFs/WithOcr/Favorites filter row. */
@Composable
private fun FilterChipRow(
    filter: MediaLibraryFilter,
    onSelect: (MediaLibraryFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        lazyRowItems(MediaLibraryFilter.entries) { option ->
            FilterChip(
                selected = filter == option,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
}

private fun MediaLibraryFilter.labelRes() =
    when (this) {
        MediaLibraryFilter.All -> Res.string.media_library_filter_all
        MediaLibraryFilter.Images -> Res.string.media_library_filter_images
        MediaLibraryFilter.Pdfs -> Res.string.media_library_filter_pdfs
        MediaLibraryFilter.WithOcr -> Res.string.media_library_filter_with_ocr
        MediaLibraryFilter.Favorites -> Res.string.media_library_filter_favorites
    }

/** V26 P26.LIB.4: "x of y selected" + Confirm, shown when [CloudLibraryScreen] is in picker mode. */
@Composable
private fun SelectionConfirmBar(
    selectedCount: Int,
    maxCount: Int,
    onConfirm: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = DesignTokens.Elevation.card) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
        ) {
            Text(
                text = stringResource(Res.string.media_library_selected_count, selectedCount, maxCount),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.s),
            )
            Button(
                onClick = onConfirm,
                enabled = selectedCount > 0,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.media_library_confirm_selection))
            }
        }
    }
}

@Composable
private fun LibraryThumbnail(
    entry: MediaLibraryEntry,
    isSelected: Boolean,
    isSelectable: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenZoom: () -> Unit,
) {
    val shape = DesignTokens.Shape.button
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .alpha(if (isSelectable) 1f else 0.35f)
                .then(
                    if (isSelectable) {
                        Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)
                    } else {
                        Modifier
                    },
                )
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

        OverlayIconButton(
            imageVector = if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription =
                stringResource(if (entry.isFavorite) Res.string.media_library_cd_unfavorite else Res.string.media_library_cd_favorite),
            onClick = onToggleFavorite,
            alignment = Alignment.TopEnd,
        )
        OverlayIconButton(
            imageVector = Icons.Default.Fullscreen,
            contentDescription = stringResource(Res.string.media_library_cd_zoom),
            onClick = onOpenZoom,
            alignment = Alignment.BottomStart,
        )
    }
}

/** Small circular tap target overlaid on a thumbnail corner — matches the review-grid delete overlay style. */
@Composable
private fun OverlayIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    alignment: Alignment,
) {
    Box(modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xs)) {
        Surface(
            shape = DesignTokens.Shape.button,
            color = Color.Black.copy(alpha = 0.55f),
            modifier =
                Modifier
                    .align(alignment)
                    .size(24.dp)
                    .clip(DesignTokens.Shape.button)
                    .clickable(onClick = onClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
            }
        }
    }
}
