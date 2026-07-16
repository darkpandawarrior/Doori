package com.mileway.feature.whatsnew.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_two_pane_placeholder_subtitle
import com.mileway.core.ui.resources.whatsnew_two_pane_placeholder_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

// ponytail: hand-rolled two-pane; migrate to org.jetbrains material3-adaptive when it's in the
// catalog — no new dependency for one breakpoint check (spec §6.2).
private val TwoPaneBreakpoint = 840.dp
private const val LIST_PANE_WEIGHT = 0.4f
private const val DETAIL_PANE_WEIGHT = 0.6f

/**
 * PLAN_V36 P6 (spec §6.2) — the whatsnew LIST destination's actual content. `maxWidth >= 840.dp`
 * (tablets/foldables/desktopApp) renders list + detail side by side with local selection state;
 * narrower renders the plain list and hands detail taps to [onOpenEntryFullscreen] (real NavHost
 * navigation, unchanged from P3/P4).
 *
 * The wide-mode detail pane deliberately does NOT receive [sharedTransitionScope]/
 * [animatedContentScope] — list and detail are mounted at the same time there, which would violate
 * `sharedBounds`'s one-source/one-target-per-key contract; only the narrow-mode list (which really
 * does transition into a separate NavHost destination) gets real scopes.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WhatsNewAdaptiveScaffold(
    onBack: () -> Unit,
    onOpenEntryFullscreen: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= TwoPaneBreakpoint) {
            var selectedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(LIST_PANE_WEIGHT)) {
                    WhatsNewListScreen(onBack = onBack, onOpenEntry = { id -> selectedEntryId = id })
                }
                Box(modifier = Modifier.weight(DETAIL_PANE_WEIGHT)) {
                    val id = selectedEntryId
                    if (id == null) {
                        WhatsNewTwoPanePlaceholder(modifier = Modifier.fillMaxSize())
                    } else {
                        WhatsNewDetailScreen(entryId = id, onBack = { selectedEntryId = null })
                    }
                }
            }
        } else {
            WhatsNewListScreen(
                onBack = onBack,
                onOpenEntry = onOpenEntryFullscreen,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
            )
        }
    }
}

/** Empty-selection placeholder for the wide-mode detail pane — spec §6.2. */
@Composable
private fun WhatsNewTwoPanePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(DesignTokens.Spacing.xxl),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.whatsnew_two_pane_placeholder_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = DesignTokens.Spacing.m),
            )
            Text(
                text = stringResource(Res.string.whatsnew_two_pane_placeholder_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.Spacing.s),
            )
        }
    }
}
