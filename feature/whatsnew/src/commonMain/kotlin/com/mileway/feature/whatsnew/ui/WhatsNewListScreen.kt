package com.mileway.feature.whatsnew.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/**
 * PLAN_V36 P3 — the full What's New list (spec §5.1). Pinned gradient header (same fixed-contrast
 * pattern as `HomeProfileHeader`, `9361fa08`/`2c7dc274`) + a `LazyColumn` of [WhatsNewEntryCard];
 * no pull-to-refresh / shimmer (the catalog is bundled, not fetched — spec §2).
 */
@Composable
fun WhatsNewListScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    viewModel: WhatsNewListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    WhatsNewListContent(state = state, onBack = onBack, onOpenEntry = onOpenEntry)
}

@Composable
private fun WhatsNewListContent(
    state: WhatsNewListUiState,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
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
                    WhatsNewEntryCard(
                        entry = entry,
                        isNew = entry.id in state.newEntryIds,
                        onClick = { onOpenEntry(entry.id) },
                    )
                }
            }
        }
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
