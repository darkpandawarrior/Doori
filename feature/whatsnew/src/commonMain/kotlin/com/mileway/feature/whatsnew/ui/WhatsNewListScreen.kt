package com.mileway.feature.whatsnew.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.platform.LocalReducedMotion
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.whatsnew_empty_subtitle
import com.mileway.core.ui.resources.whatsnew_empty_title
import com.mileway.core.ui.resources.whatsnew_list_subtitle_count
import com.mileway.core.ui.resources.whatsnew_list_subtitle_empty
import com.mileway.core.ui.resources.whatsnew_list_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.ui.components.WhatsNewEntryCard
import com.mileway.feature.whatsnew.viewmodel.WhatsNewListUiState
import com.mileway.feature.whatsnew.viewmodel.WhatsNewListViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val ENTRY_ANIM_DURATION_MS = 220
private val EntryAnimOffset = 16.dp

/**
 * PLAN_V36 P3 — the full What's New list (spec §5.1). Pinned gradient header (same fixed-contrast
 * pattern as `HomeProfileHeader`, `9361fa08`/`2c7dc274`) + a `LazyColumn` of [WhatsNewEntryCard];
 * no pull-to-refresh / shimmer (the catalog is bundled, not fetched — spec §2).
 *
 * [sharedTransitionScope]/[animatedContentScope] are `null` by default — only the narrow-width
 * NavHost path (`whatsNewGraph`'s LIST destination) supplies real ones for the P6 shared-element
 * transition into the detail screen; the two-pane scaffold's inline detail path must never receive
 * them (see `whatsNewSharedBounds`'s KDoc for why).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WhatsNewListScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    viewModel: WhatsNewListViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
    val state by viewModel.uiState.collectAsState()
    WhatsNewListContent(
        state = state,
        onBack = onBack,
        onOpenEntry = onOpenEntry,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )
}

// PLAN_V36 P8: internal (not private) so `ui/previews/WhatsNewPreviews.kt` can render it directly
// against inline fake state — the "no DI, no ViewModel, no Koin" preview convention (see
// TrackingPreviews.kt) needs a state-driven entry point, and this is already exactly that.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun WhatsNewListContent(
    state: WhatsNewListUiState,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
) {
    val reducedMotion = LocalReducedMotion.current
    Column(modifier = Modifier.fillMaxSize()) {
        WhatsNewListHeader(entryCount = state.entries.size, onBack = onBack)

        if (state.isEmpty) {
            WhatsNewEmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    WhatsNewListItem(reducedMotion = reducedMotion) {
                        WhatsNewEntryCard(
                            entry = entry,
                            isNew = entry.id in state.newEntryIds,
                            onClick = { onOpenEntry(entry.id) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                        )
                    }
                }
            }
        }
    }
}

/**
 * PLAN_V36 P6 (spec §6.3) — the list-entry fade+slide reveal deferred from P3. One-shot per card
 * composition (a `LazyColumn` item re-enters this on scroll back into view, matching the reference
 * app's own re-triggering reveal), skipped entirely under [LocalReducedMotion] — content renders
 * immediately at rest instead of animating in.
 */
@Composable
private fun WhatsNewListItem(
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    if (reducedMotion) {
        content()
        return
    }
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { EntryAnimOffset.toPx() }
    LaunchedEffect(Unit) { progress.animateTo(1f, animationSpec = tween(ENTRY_ANIM_DURATION_MS)) }
    Box(
        modifier =
            Modifier.graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * offsetPx
            },
    ) {
        content()
    }
}

/**
 * Pinned header, fixed-contrast against the primary gradient (never `Color.White` — see
 * `HomeProfileHeader`'s `9361fa08` fix note: primary-on-primary is unreadable on saturated themes).
 */
@Composable
private fun WhatsNewListHeader(
    entryCount: Int,
    onBack: () -> Unit,
) {
    val headerContent = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DesignTokens.topBarGradientBrush())
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.core_cd_back),
                    tint = headerContent,
                )
            }
            Icon(
                Icons.Filled.NewReleases,
                contentDescription = null,
                tint = headerContent,
                modifier = Modifier.size(DesignTokens.IconSize.header),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            Column {
                Text(
                    text = stringResource(Res.string.whatsnew_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = headerContent,
                )
                Text(
                    text =
                        if (entryCount > 0) {
                            stringResource(Res.string.whatsnew_list_subtitle_count, entryCount)
                        } else {
                            stringResource(Res.string.whatsnew_list_subtitle_empty)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = headerContent.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Centered icon-in-circle empty state (parity — spec §5.1). */
@Composable
private fun WhatsNewEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.whatsnew_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.whatsnew_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
