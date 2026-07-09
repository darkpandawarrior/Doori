@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.media.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.core_cd_more_options
import com.mileway.core.ui.resources.media_add_attachment_subtitle
import com.mileway.core.ui.resources.media_add_attachment_title
import com.mileway.core.ui.resources.media_captured_count
import com.mileway.core.ui.resources.media_cd_attachment_index
import com.mileway.core.ui.resources.media_cd_biometric_lock
import com.mileway.core.ui.resources.media_cd_ocr_reading
import com.mileway.core.ui.resources.media_cd_ocr_verified
import com.mileway.core.ui.resources.media_cd_scan
import com.mileway.core.ui.resources.media_cd_theme_color
import com.mileway.core.ui.resources.media_illustrative_snackbar
import com.mileway.core.ui.resources.media_ocr_label
import com.mileway.core.ui.resources.media_ocr_verified_label
import com.mileway.core.ui.resources.media_select_source_title
import com.mileway.core.ui.resources.media_source_barcode
import com.mileway.core.ui.resources.media_source_camera
import com.mileway.core.ui.resources.media_source_cloud_library
import com.mileway.core.ui.resources.media_source_document_scanner
import com.mileway.core.ui.resources.media_source_files
import com.mileway.core.ui.resources.media_source_gallery
import com.mileway.core.ui.resources.media_source_odometer
import com.mileway.core.ui.resources.media_source_pdf
import com.mileway.core.ui.resources.media_source_qr
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.AttachmentSource
import com.mileway.feature.media.ui.scanner.rememberDocumentScanLauncher
import com.mileway.feature.media.viewmodel.MediaAction
import com.mileway.feature.media.viewmodel.MediaViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Bottom padding so scrolling content clears the floating bubble bottom bar. */
private val BottomBarClearance = 140.dp

/** Accent applied to the Odometer source tile's icon (blue speedo) per the reference. */
private val OdometerAccent = DesignTokens.StatusColors.info

/**
 * Entry screen of the media flow: a 3-column "Select an attachment source" grid of nine
 * source tiles plus a grid of already-captured attachment thumbnails.
 *
 * Camera, Gallery and Files are functional; the remaining tiles are illustrative in this
 * demo and surface an explanatory snackbar. This is a top-level tab destination, so its
 * scrolling content reserves bottom space for the floating bubble bar.
 *
 * @param onNavigateToCamera   navigate to the CameraX capture route (optionally odometer mode)
 * @param onNavigateToPreview  navigate to the preview route once a uri is selected
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSelectionScreen(
    viewModel: MediaViewModel,
    onNavigateToCamera: (odometer: Boolean) -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                viewModel.onAction(MediaAction.PickedFromGallery(uri.toString()))
                onNavigateToPreview()
            }
        }

    // D.4: ML Kit document scanner — each scanned page enters the batch like a picked image.
    val launchDocumentScan =
        rememberDocumentScanLauncher { pages ->
            pages.forEach { viewModel.onAction(MediaAction.PickedFromGallery(it)) }
            if (pages.isNotEmpty()) onNavigateToPreview()
        }

    val illustrativeSnackbarTemplate = stringResource(Res.string.media_illustrative_snackbar)

    fun showIllustrative(label: String) {
        scope.launch {
            snackbarHostState.showSnackbar(String.format(illustrativeSnackbarTemplate, label))
        }
    }

    // The nine source tiles, in reading order across the 3-column grid.
    val sources =
        listOf(
            SourceTileSpec(SourceKey.CAMERA, stringResource(Res.string.media_source_camera), Icons.Default.CameraAlt),
            SourceTileSpec(SourceKey.GALLERY, stringResource(Res.string.media_source_gallery), Icons.Default.PhotoLibrary),
            SourceTileSpec(
                SourceKey.ODOMETER,
                stringResource(Res.string.media_source_odometer),
                Icons.Default.Speed,
                accent = OdometerAccent,
            ),
            SourceTileSpec(SourceKey.DOC_SCANNER, stringResource(Res.string.media_source_document_scanner), Icons.Default.DocumentScanner),
            SourceTileSpec(SourceKey.PDF, stringResource(Res.string.media_source_pdf), Icons.Default.PictureAsPdf),
            SourceTileSpec(SourceKey.QR, stringResource(Res.string.media_source_qr), Icons.Default.QrCode),
            SourceTileSpec(SourceKey.BARCODE, stringResource(Res.string.media_source_barcode), Icons.Default.QrCodeScanner),
            SourceTileSpec(SourceKey.CLOUD, stringResource(Res.string.media_source_cloud_library), Icons.Default.CloudDownload),
            SourceTileSpec(SourceKey.FILES, stringResource(Res.string.media_source_files), Icons.Default.Folder),
        )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            var showOverflowMenu by remember { mutableStateOf(false) }
            DepthAwareTopBar(
                title = stringResource(Res.string.media_add_attachment_title),
                subtitle = stringResource(Res.string.media_add_attachment_subtitle),
                titleIcon = Icons.Default.AttachFile,
                depth = NavigationDepth.ROOT,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.core_cd_back),
                        )
                    }
                },
                actions = {
                    // Trailing config icons (reference parity: theme, biometric, scan).
                    // Not interactive in this demo, rendered disabled per M3 pattern.
                    // Only 2 shown inline; the rest overflow into the menu (R2: max 2 trailing actions).
                    IconButton(enabled = false, onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(Res.string.media_cd_biometric_lock),
                            modifier = Modifier.alpha(0.38f),
                        )
                    }
                    IconButton(enabled = false, onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(Res.string.media_cd_scan),
                            modifier = Modifier.alpha(0.38f),
                        )
                    }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.core_cd_more_options),
                        )
                    }
                    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text(stringResource(Res.string.media_cd_theme_color)) },
                            leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                            onClick = { showOverflowMenu = false },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = DesignTokens.Spacing.screenHorizontal,
                    end = DesignTokens.Spacing.screenHorizontal,
                    top = DesignTokens.Spacing.l,
                    bottom = BottomBarClearance,
                ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.media_select_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = DesignTokens.Spacing.s),
                )
            }

            items(sources, key = { it.key }) { spec ->
                SourceTile(
                    spec = spec,
                    onClick = {
                        when (spec.key) {
                            SourceKey.CAMERA -> {
                                viewModel.onAction(MediaAction.SelectSource(AttachmentSource.CAMERA))
                                onNavigateToCamera(false)
                            }
                            SourceKey.ODOMETER -> {
                                viewModel.onAction(MediaAction.SelectSource(AttachmentSource.CAMERA))
                                onNavigateToCamera(true)
                            }
                            SourceKey.GALLERY -> {
                                viewModel.onAction(MediaAction.SelectSource(AttachmentSource.GALLERY))
                                galleryLauncher.launch("image/*")
                            }
                            SourceKey.FILES -> {
                                viewModel.onAction(MediaAction.SelectSource(AttachmentSource.FILES))
                                galleryLauncher.launch("*/*")
                            }
                            SourceKey.DOC_SCANNER -> {
                                viewModel.onAction(MediaAction.SelectSource(AttachmentSource.FILES))
                                launchDocumentScan()
                            }
                            SourceKey.CLOUD -> onNavigateToLibrary()
                            else -> showIllustrative(spec.label)
                        }
                    },
                )
            }

            if (state.attachments.isNotEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(DesignTokens.Spacing.l))
                        Text(
                            text = stringResource(Res.string.media_captured_count, state.attachments.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = DesignTokens.Spacing.s),
                        )
                    }
                }

                items(state.attachments, key = { it.id }) { item ->
                    AttachmentThumbnail(item)
                }
            }
        }
    }
}

/** Stable identity for each source tile, used as a grid key and to route taps. */
private enum class SourceKey {
    CAMERA,
    GALLERY,
    ODOMETER,
    DOC_SCANNER,
    PDF,
    QR,
    BARCODE,
    CLOUD,
    FILES,
}

/** Declarative spec for a single source tile. */
private data class SourceTileSpec(
    val key: SourceKey,
    val label: String,
    val icon: ImageVector,
    val accent: Color? = null,
)

@Composable
private fun SourceTile(
    spec: SourceTileSpec,
    onClick: () -> Unit,
) {
    val iconTint = spec.accent ?: MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor =
        spec.accent?.copy(alpha = 0.15f)
            ?: MaterialTheme.colorScheme.primaryContainer

    Surface(
        shape = DesignTokens.Shape.actionTile,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(DesignTokens.Shape.actionTile)
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.s),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(DesignTokens.ActionTileSize.circularContainer)
                        .clip(DesignTokens.Shape.button)
                        .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.label,
                    tint = iconTint,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun AttachmentThumbnail(item: AttachmentItem) {
    Surface(
        shape = DesignTokens.Shape.roundedMd,
        tonalElevation = DesignTokens.Elevation.card,
        modifier = Modifier.aspectRatio(1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.uri,
                contentDescription = stringResource(Res.string.media_cd_attachment_index, item.id),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(DesignTokens.Shape.roundedMd),
            )

            // D.5: OCR badge — a verified tick when >=2 passes agreed, else a neutral "OCR" chip
            // whenever a reading was detected. Driven by the multi-pass OcrResult (D.2).
            val ocr = item.ocr
            if (ocr?.detectedOdometer != null) {
                val verified = ocr.isVerified
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(DesignTokens.Spacing.xs)
                            .clip(DesignTokens.Shape.button)
                            .background(
                                if (verified) {
                                    DesignTokens.StatusColors.success.copy(alpha = 0.9f)
                                } else {
                                    Color.Black.copy(alpha = 0.55f)
                                },
                            )
                            .padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (verified) Icons.Default.Verified else Icons.Default.DocumentScanner,
                        contentDescription =
                            if (verified) {
                                stringResource(Res.string.media_cd_ocr_verified)
                            } else {
                                stringResource(Res.string.media_cd_ocr_reading)
                            },
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = if (verified) stringResource(Res.string.media_ocr_verified_label) else stringResource(Res.string.media_ocr_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
