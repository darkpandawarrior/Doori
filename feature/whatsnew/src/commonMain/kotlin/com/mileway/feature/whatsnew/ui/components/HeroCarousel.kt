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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.ui.platform.LocalReducedMotion
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_cd_next_media
import com.mileway.core.ui.resources.whatsnew_cd_previous_media
import com.mileway.core.ui.resources.whatsnew_cd_thumbnail
import com.mileway.core.ui.resources.whatsnew_detail_step
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import com.mileway.feature.whatsnew.resources.Res as WhatsNewRes

private val HeroHeight = 280.dp
private val ThumbnailSize = 64.dp
private const val AUTO_ADVANCE_MS = 4_500L

/**
 * PLAN_V36 P6 (spec §6.3) — pure gate for [HeroCarousel]'s auto-advance loop, pulled out of the
 * `LaunchedEffect` so it's directly unit-testable without a Compose test harness.
 */
internal fun shouldAutoAdvance(
    reducedMotion: Boolean,
    isZoomed: Boolean,
    isScrollInProgress: Boolean,
): Boolean = !reducedMotion && !isZoomed && !isScrollInProgress

/**
 * PLAN_V36 P4 — spec §5.2's hero carousel: a [HorizontalPager] of [ZoomableMedia] pages. When
 * there's more than one [WhatsNewMedia] item it also renders chevron overlays, a dot indicator, a
 * 64dp thumbnail strip (selected = primary border) and a "Step X of N" chip, and auto-advances
 * every 4.5s — paused while the current page is zoomed, the pager itself is being dragged, or
 * (PLAN_V36 P6) [LocalReducedMotion] is on, in which case the auto-advance loop never starts at
 * all. The caller (`WhatsNewDetailScreen`) already skips this composable entirely when media is
 * empty.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun HeroCarousel(
    media: List<WhatsNewMedia>,
    title: String,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { media.size })
    val scope = rememberCoroutineScope()
    var isZoomed by remember { mutableStateOf(false) }
    val multiMedia = media.size > 1
    val reducedMotion = LocalReducedMotion.current
    val previousLabel = stringResource(Res.string.whatsnew_cd_previous_media)
    val nextLabel = stringResource(Res.string.whatsnew_cd_next_media)

    LaunchedEffect(pagerState.currentPage) {
        // V36 review fix: re-sync the carousel-level zoom flag on every page change, not just via
        // the per-page onZoomChanged below. Navigating off a zoomed page via chevron/thumbnail
        // (rather than dragging out of zoom) used to leave isZoomed stuck true forever, permanently
        // killing auto-advance. ZoomableMedia resets its own zoom on page change via resetKey — this
        // just keeps the carousel's flag in sync with that.
        isZoomed = false
        onPageChanged(pagerState.currentPage)
    }

    if (multiMedia && !reducedMotion) {
        LaunchedEffect(pagerState) {
            while (true) {
                delay(AUTO_ADVANCE_MS)
                if (shouldAutoAdvance(reducedMotion, isZoomed, pagerState.isScrollInProgress)) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % media.size)
                }
            }
        }
    }

    Column(
        modifier =
            modifier.let {
                // PLAN_V36 P6 (spec §6.4): TalkBack next/previous-step actions — the pager's own
                // swipe gesture is otherwise the only way to change pages. Horizontal-scroll
                // semantics for the pager itself come for free from HorizontalPager.
                if (!multiMedia) {
                    it
                } else {
                    it.semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(previousLabel) {
                                    scope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage - 1 + media.size) % media.size)
                                    }
                                    true
                                },
                                CustomAccessibilityAction(nextLabel) {
                                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1) % media.size) }
                                    true
                                },
                            )
                    }
                }
            },
    ) {
        Box {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(HeroHeight)) { page ->
                val item = media[page]
                ZoomableMedia(
                    model = WhatsNewRes.getUri(item.path),
                    contentDescription = whatsNewMediaContentDescription(item.caption, title, page, media.size),
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
                    contentDescription = previousLabel,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1 + media.size) % media.size)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(DesignTokens.Spacing.s),
                )
                ChevronOverlayButton(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = nextLabel,
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

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ThumbnailButton(
    item: WhatsNewMedia,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    AsyncImage(
        model = WhatsNewRes.getUri(item.path),
        contentDescription = item.caption ?: stringResource(Res.string.whatsnew_cd_thumbnail, index + 1),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(ThumbnailSize)
                .clip(DesignTokens.Shape.roundedSm)
                // PLAN_V36 P6 (spec §6.4): TalkBack announces the currently-selected thumbnail.
                .semantics { this.selected = selected }
                .border(2.dp, borderColor, DesignTokens.Shape.roundedSm)
                .clickable(onClick = onClick),
    )
}
