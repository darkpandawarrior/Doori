package com.mileway.feature.whatsnew.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_cd_new_badge
import com.mileway.core.ui.resources.whatsnew_new_badge
import com.mileway.core.ui.resources.whatsnew_released_on
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import com.mileway.feature.whatsnew.resources.Res as WhatsNewRes

private const val PRESSED_SCALE = 0.96f
private val HeroHeightMin = 180.dp
private val HeroHeightMax = 280.dp

/** Also reused by `WhatsNewDetailScreen`'s header subtitle (release-date fallback, spec §5.2). */
internal val ReleasedDateFormat =
    LocalDate.Format {
        dayOfMonth()
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        year()
    }

/**
 * PLAN_V36 P3 — one row in [com.mileway.feature.whatsnew.ui.WhatsNewListScreen]'s `LazyColumn`.
 * Spec §5.1: hero image (skipped cleanly when [WhatsNewEntry.media] is empty — true for every
 * catalog entry until V36.P5 ships real media), NEW badge, title+chevron, description, a
 * released-date tag and one tag per [WhatsNewEntry.modules] entry.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WhatsNewEntryCard(
    entry: WhatsNewEntry,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "whatsnew_card_scale",
    )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(interactionSource = interactionSource, indication = LocalIndication.current) {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                },
        shape = DesignTokens.Shape.carouselCard,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                modifier = Modifier.padding(DesignTokens.Spacing.l),
            ) {
                if (entry.media.isNotEmpty()) {
                    WhatsNewHeroImage(
                        entryId = entry.id,
                        title = entry.title,
                        media = entry.media.first(),
                        totalMedia = entry.media.size,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f)
                                .whatsNewSharedBounds(
                                    key = whatsNewTitleSharedKey(entry.id),
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                ),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (entry.modules.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        item {
                            ReleasedDateTag(entry.releasedOn)
                        }
                        items(entry.modules) { module ->
                            WhatsNewTag(module)
                        }
                    }
                } else {
                    ReleasedDateTag(entry.releasedOn)
                }
            }

            if (isNew) {
                NewBadge(modifier = Modifier.align(Alignment.TopEnd).padding(DesignTokens.Spacing.m))
            }
        }
    }
}

/** Hero image (first media item only — a carousel is the detail screen's job, V36.P4). */
@OptIn(ExperimentalResourceApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun WhatsNewHeroImage(
    entryId: String,
    title: String,
    media: WhatsNewMedia,
    totalMedia: Int,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
) {
    Box(
        modifier =
            Modifier.whatsNewSharedBounds(
                key = whatsNewHeroSharedKey(entryId),
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
            ),
    ) {
        AsyncImage(
            model = WhatsNewRes.getUri(media.path),
            contentDescription = whatsNewMediaContentDescription(media.caption, title, index = 0, total = totalMedia),
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HeroHeightMin, max = HeroHeightMax)
                    .clip(DesignTokens.Shape.roundedMd)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        // Subtle bottom scrim so a badge/caption over a bright screenshot stays legible.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HeroHeightMin, max = HeroHeightMax)
                    .clip(DesignTokens.Shape.roundedMd)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f)),
                            startY = 0.6f,
                        ),
                    ),
        )
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.whatsnew_new_badge)
    val description = stringResource(Res.string.whatsnew_cd_new_badge)
    Surface(
        // PLAN_V36 P6 (spec §6.4): clearAndSetSemantics so TalkBack announces the full
        // "unread update" meaning instead of just the visible "NEW" glyph.
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.error,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
        )
    }
}

/** Non-interactive display tag — same `SuggestionChip(onClick = {})` idiom as `TravelHomeScreen`. */
@Composable
private fun WhatsNewTag(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** Reused directly by `WhatsNewDetailScreen`'s "RELEASED <date>" chip (spec §5.2) — same visual language. */
@Composable
internal fun ReleasedDateTag(releasedOn: LocalDate) {
    val text = stringResource(Res.string.whatsnew_released_on, releasedOn.format(ReleasedDateFormat))
    SuggestionChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    )
}
