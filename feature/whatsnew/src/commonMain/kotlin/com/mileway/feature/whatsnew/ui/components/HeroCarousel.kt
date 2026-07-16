package com.mileway.feature.whatsnew.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_cd_next_media
import com.mileway.core.ui.resources.whatsnew_cd_previous_media
import com.mileway.core.ui.resources.whatsnew_cd_thumbnail
import com.mileway.core.ui.resources.whatsnew_detail_step
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val HeroHeight = 280.dp
private val ThumbnailSize = 64.dp
private const val AUTO_ADVANCE_MS = 4_500L

/**
 * PLAN_V36 P4 — spec §5.2's hero carousel: a [HorizontalPager] of [ZoomableMedia] pages. When
 * there's more than one [WhatsNewMedia] item it also renders chevron overlays, a dot indicator, a
 * 64dp thumbnail strip (selected = primary border) and a "Step X of N" chip, and auto-advances
 * every 4.5s — paused while the current page is zoomed or the pager itself is being dragged.
 *
 * Reduced-motion (killing the auto-advance) is V36.P6 (`LocalReducedMotion` doesn't exist yet).
 * The caller (`WhatsNewDetailScreen`) already skips this composable entirely when media is empty.
 */
@Composable
fun HeroCarousel(
    media: List<WhatsNewMedia>,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { media.size })
    val scope = rememberCoroutineScope()
    var isZoomed by remember { mutableStateOf(false) }
    val multiMedia = media.size > 1

    LaunchedEffect(pagerState.currentPage) { onPageChanged(pagerState.currentPage) }

    if (multiMedia) {
        LaunchedEffect(pagerState) {
            while (true) {
                delay(AUTO_ADVANCE_MS)
                if (!isZoomed && !pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % media.size)
                }
            }
        }
    }

    Column(modifier = modifier) {
        Box {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(HeroHeight)) { page ->
                val item = media[page]
                ZoomableMedia(
                    model = item.path,
                    contentDescription = item.caption,
                    resetKey = page,
                    onZoomChanged = { zoomed -> if (page == pagerState.currentPage) isZoomed = zoomed },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(HeroHeight)
                            .clip(DesignTokens.Shape.roundedMd)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            if (multiMedia) {
                ChevronOverlayButton(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(Res.string.whatsnew_cd_previous_media),
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1 + media.size) % media.size)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(DesignTokens.Spacing.s),
                )
                ChevronOverlayButton(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(Res.string.whatsnew_cd_next_media),
                    onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1) % media.size) } },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(DesignTokens.Spacing.s),
                )
                StepChip(
                    current = pagerState.currentPage + 1,
                    total = media.size,
                    modifier = Modifier.align(Alignment.TopEnd).padding(DesignTokens.Spacing.m),
                )
            }
        }

        if (multiMedia) {
            PageDots(
                count = media.size,
                selected = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.m),
            ) {
                itemsIndexed(media) { index, item ->
                    ThumbnailButton(
                        item = item,
                        index = index,
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }
        }
    }
}

/** Same translucent-circle idiom as `core/ui`'s `ZoomImageViewer` close button. */
@Composable
private fun ChevronOverlayButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = DesignTokens.Shape.button,
        color = Color.Black.copy(alpha = 0.4f),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}

@Composable
private fun StepChip(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = stringResource(Res.string.whatsnew_detail_step, current, total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
        )
    }
}

/** Dot indicator, same animated-width idiom as `core/ui`'s `OnboardingCarousel`. */
@Composable
private fun PageDots(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(if (isSelected) 20.dp else 8.dp, label = "whatsnew_hero_dot_width")
            val color by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "whatsnew_hero_dot_color",
            )
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 8.dp)
                        .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun ThumbnailButton(
    item: WhatsNewMedia,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    AsyncImage(
        model = item.path,
        contentDescription = item.caption ?: stringResource(Res.string.whatsnew_cd_thumbnail, index + 1),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(ThumbnailSize)
                .clip(DesignTokens.Shape.roundedSm)
                .border(2.dp, borderColor, DesignTokens.Shape.roundedSm)
                .clickable(onClick = onClick),
    )
}
