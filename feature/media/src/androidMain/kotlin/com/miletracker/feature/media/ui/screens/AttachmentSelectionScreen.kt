@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.media.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.media.model.AttachmentItem
import com.miletracker.feature.media.model.AttachmentSource
import com.miletracker.feature.media.viewmodel.MediaAction
import com.miletracker.feature.media.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

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

    fun showIllustrative(label: String) {
        scope.launch {
            snackbarHostState.showSnackbar("$label is illustrative in this demo")
        }
    }

    // The nine source tiles, in reading order across the 3-column grid.
    val sources =
        remember {
            listOf(
                SourceTileSpec(SourceKey.CAMERA, "Camera", Icons.Default.CameraAlt),
                SourceTileSpec(SourceKey.GALLERY, "Gallery", Icons.Default.PhotoLibrary),
                SourceTileSpec(SourceKey.ODOMETER, "Odometer", Icons.Default.Speed, accent = OdometerAccent),
                SourceTileSpec(SourceKey.DOC_SCANNER, "Document Scanner", Icons.Default.DocumentScanner),
                SourceTileSpec(SourceKey.PDF, "PDF", Icons.Default.PictureAsPdf),
                SourceTileSpec(SourceKey.QR, "QR Code", Icons.Default.QrCode),
                SourceTileSpec(SourceKey.BARCODE, "Barcode", Icons.Default.QrCodeScanner),
                SourceTileSpec(SourceKey.CLOUD, "Cloud Library", Icons.Default.CloudDownload),
                SourceTileSpec(SourceKey.FILES, "Files", Icons.Default.Folder),
            )
        }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = "Add Attachment",
                subtitle = "Upload Attachments",
                depth = NavigationDepth.ROOT,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Trailing config icons (reference parity: theme, biometric, scan).
                    // Not interactive in this demo — rendered disabled per M3 pattern.
                    IconButton(enabled = false, onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Theme color",
                            modifier = Modifier.alpha(0.38f),
                        )
                    }
                    IconButton(enabled = false, onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric lock",
                            modifier = Modifier.alpha(0.38f),
                        )
                    }
                    IconButton(enabled = false, onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            modifier = Modifier.alpha(0.38f),
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
                    text = "Select an attachment source",
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
                            text = "Captured (${state.attachments.size})",
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
                        .clip(CircleShape)
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
        AsyncImage(
            model = item.uri,
            contentDescription = "Attachment ${item.id}",
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(DesignTokens.Shape.roundedMd),
        )
    }
}
