@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.mileway.core.ui.components.ZoomImageViewer
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.ui.sheets.OcrResultBottomSheet
import com.mileway.feature.media.viewmodel.MediaAction
import com.mileway.feature.media.viewmodel.MediaViewModel

/**
 * Preview of the pending capture(s) before they are committed.
 *
 * - **Single capture**: a full-bleed preview with a Retake / Run OCR / Use photo action row.
 * - **Multi capture** (more than one item in the batch): a 3-column thumbnail grid with a
 *   delete overlay per tile and a dashed "Add More" tile, plus a "Use N photos" confirm bar.
 *   Tapping a thumbnail opens it fullscreen via the shared [ZoomImageViewer].
 *
 * The OCR result sheet is state-driven off [com.mileway.feature.media.viewmodel.MediaUiState.ocrSheetVisible].
 *
 * @param onRetake   discard the pending batch and pop back to the camera/selection
 * @param onAddMore  return to the camera to capture another photo into the same batch
 * @param onUsePhoto invoked after the pending attachment(s) are confirmed
 */
@Composable
fun AttachmentPreviewScreen(
    viewModel: MediaViewModel,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    onAddMore: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val batch = state.pendingBatch
    val pending = state.pendingItem
    val isMulti = batch.size > 1

    // Fullscreen viewer target (null = closed).
    var viewerUri by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = modifier) { innerPadding ->
        if (isMulti) {
            MultiCaptureContent(
                items = batch,
                isProcessing = state.isProcessing,
                contentPadding = innerPadding,
                onOpen = { viewerUri = it.uri },
                onDelete = { viewModel.onAction(MediaAction.RemoveFromBatch(it.id)) },
                onAddMore = onAddMore,
                onRetake = {
                    viewModel.onAction(MediaAction.Retake)
                    onRetake()
                },
                onConfirm = {
                    viewModel.onAction(MediaAction.ConfirmPending)
                    onUsePhoto()
                },
            )
        } else {
            SingleCaptureContent(
                pending = pending,
                isProcessing = state.isProcessing,
                contentPadding = innerPadding,
                onPreviewTap = { uri -> viewerUri = uri },
                onRetake = {
                    viewModel.onAction(MediaAction.Retake)
                    onRetake()
                },
                onRunOcr = { viewModel.onAction(MediaAction.RunOcr) },
                onUsePhoto = {
                    viewModel.onAction(MediaAction.ConfirmPending)
                    onUsePhoto()
                },
            )
        }
    }

    // Fullscreen zoomable viewer for a tapped thumbnail / preview.
    viewerUri?.let { uri ->
        ZoomImageViewer(
            painter = rememberAsyncImagePainter(model = uri),
            contentDescription = "Attachment preview",
            onDismiss = { viewerUri = null },
        )
    }

    val ocr = pending?.ocr
    if (state.ocrSheetVisible && ocr != null) {
        OcrResultBottomSheet(
            result = ocr,
            onConfirm = {
                viewModel.onAction(MediaAction.ConfirmPending)
                onUsePhoto()
            },
            onEdit = { viewModel.onAction(MediaAction.DismissSheet) },
            onDismiss = { viewModel.onAction(MediaAction.DismissSheet) },
        )
    }
}

/** Single-photo layout: full-bleed preview + Retake / Run OCR / Use photo. */
@Composable
private fun SingleCaptureContent(
    pending: AttachmentItem?,
    isProcessing: Boolean,
    contentPadding: PaddingValues,
    onPreviewTap: (String) -> Unit,
    onRetake: () -> Unit,
    onRunOcr: () -> Unit,
    onUsePhoto: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (pending != null) {
                AsyncImage(
                    model = pending.uri,
                    contentDescription = "Captured attachment preview",
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable { onPreviewTap(pending.uri) },
                )
            } else {
                Text(
                    text = "No photo to preview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isProcessing) {
                CircularProgressIndicator()
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(
                onClick = onRetake,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f),
            ) {
                Text("Retake")
            }
            TextButton(
                onClick = onRunOcr,
                enabled = pending != null && !isProcessing,
                modifier = Modifier.weight(1f),
            ) {
                Text("Run OCR")
            }
            Button(
                onClick = onUsePhoto,
                enabled = pending != null && !isProcessing,
                modifier = Modifier.weight(1f),
            ) {
                Text("Use photo")
            }
        }
    }
}

/** Multi-photo layout: thumbnail grid with delete overlays + "Add More", and a confirm bar. */
@Composable
private fun MultiCaptureContent(
    items: List<AttachmentItem>,
    isProcessing: Boolean,
    contentPadding: PaddingValues,
    onOpen: (AttachmentItem) -> Unit,
    onDelete: (AttachmentItem) -> Unit,
    onAddMore: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
    ) {
        Text(
            text = "Review captures",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    start = DesignTokens.Spacing.l,
                    end = DesignTokens.Spacing.l,
                    top = DesignTokens.Spacing.l,
                    bottom = DesignTokens.Spacing.s,
                ),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                CaptureThumbnail(
                    item = item,
                    onOpen = { onOpen(item) },
                    onDelete = { onDelete(item) },
                )
            }
            item(key = "add_more") {
                AddMoreTile(onClick = onAddMore)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
        ) {
            Button(
                onClick = onConfirm,
                enabled = items.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Use ${items.size} photos")
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedButton(
                onClick = onRetake,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Discard all")
            }
        }
    }
}

/** One capture tile in the review grid, with a circular delete overlay in the corner. */
@Composable
private fun CaptureThumbnail(
    item: AttachmentItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        Surface(
            shape = DesignTokens.Shape.roundedMd,
            tonalElevation = DesignTokens.Elevation.card,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(DesignTokens.Shape.roundedMd)
                    .clickable(onClick = onOpen),
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = "Capture ${item.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(DesignTokens.Spacing.xs)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove capture",
                    tint = Color.White,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
            }
        }
    }
}

/** Dashed "Add More" tile that returns to the camera to capture another photo. */
@Composable
private fun AddMoreTile(onClick: () -> Unit) {
    Surface(
        shape = DesignTokens.Shape.roundedMd,
        color = Color.Transparent,
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(DesignTokens.Shape.roundedMd)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = DesignTokens.Shape.roundedMd,
                )
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add more",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.header),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = "Add More",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
