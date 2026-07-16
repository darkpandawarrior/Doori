package com.mileway.feature.whatsnew.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * PLAN_V36 P6 (spec §6.1) — the What's New shared-element transition.
 *
 * Path taken: the primary path, not the plan's fallback. JB navigation-compose 2.10.0-alpha02's
 * `composable(...)` content lambda already carries an `AnimatedContentScope` receiver (same shape
 * as upstream AndroidX navigation-compose, which `AnimatedContentScope` also implements
 * `AnimatedVisibilityScope` for expressly so it can be handed straight to
 * `Modifier.sharedBounds`'s `animatedVisibilityScope` parameter — the documented
 * shared-elements-with-navigation recipe). `MilewayAppRoot` wraps its `NavHost` in a single
 * `SharedTransitionLayout`; `whatsNewGraph` threads that `SharedTransitionScope`, plus each
 * destination's own `AnimatedContentScope`, down as nullable parameters (not a `CompositionLocal`
 * — there are exactly two consumers, [com.mileway.feature.whatsnew.ui.components.WhatsNewEntryCard]
 * and [com.mileway.feature.whatsnew.ui.WhatsNewDetailScreen], so an ambient local would be more
 * machinery than the two call sites need, and — more importantly — a `CompositionLocal` would leak
 * into `WhatsNewAdaptiveScaffold`'s two-pane inline detail pane, where the list and detail are
 * mounted *simultaneously* rather than via a NavHost transition; `sharedBounds` requires exactly
 * one source and one target per key, so the two-pane path must never receive real scopes. Explicit
 * nullable parameters make that "no scopes here" decision visible at each call site instead of
 * requiring a manual local override.
 *
 * `null` scopes (previews, commonTest fakes, and the two-pane inline path) make
 * [whatsNewSharedBounds] a no-op — plain content, no shared bounds, exactly like every other
 * Mileway destination today.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.whatsNewSharedBounds(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
): Modifier {
    if (sharedTransitionScope == null || animatedContentScope == null) return this
    return with(sharedTransitionScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedContentScope,
        )
    }
}

internal fun whatsNewHeroSharedKey(entryId: String): String = "whatsnew_hero_$entryId"

internal fun whatsNewTitleSharedKey(entryId: String): String = "whatsnew_title_$entryId"
