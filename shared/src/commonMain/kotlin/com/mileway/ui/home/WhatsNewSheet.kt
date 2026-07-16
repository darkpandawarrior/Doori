package com.mileway.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whats_new_got_it
import com.mileway.core.ui.resources.whats_new_see_all
import com.mileway.core.ui.resources.whats_new_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V24 P2.2 / PLAN_V36 P2 — the one-shot "What's new in Mileway" digest sheet. Shows the
 * top entries (a short digest, not the full catalog — see [WhatsNewViewModel]) from the bundled
 * [com.mileway.feature.whatsnew.data.WhatsNewRepository]; dismissing (button or scrim)
 * acknowledges the version so it never replays.
 *
 * Rows navigate to that entry's detail screen via [onOpenEntry]; the footer opens the full list
 * via [onSeeAll] — both wired by the host in a later phase, no-op hoisted callbacks until then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    entries: List<WhatsNewEntry>,
    onDismiss: () -> Unit,
    onOpenEntry: (String) -> Unit = {},
    onSeeAll: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                Icon(Icons.Filled.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(Res.string.whats_new_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            entries.forEach { entry ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEntry(entry.id) },
                ) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            TextButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.whats_new_see_all))
            }

            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedMd,
            ) { Text(stringResource(Res.string.whats_new_got_it), fontWeight = FontWeight.SemiBold) }
        }
    }
}
