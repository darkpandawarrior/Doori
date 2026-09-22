package com.mileway.core.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.theme.DesignTokens

// ---------------------------------------------------------------------------
// Previews for the two most widely reused core:ui primitives.
//
// Chosen by import count, not by feel. Measured on this branch:
//   SectionCard  imported by 21 modules
//   StatusChip   imported by 17 modules
//   (next one down is ExpandableText at 4)
//
// Neither had a preview before. core:ui's existing 43 previews are all tracking
// hero-card / top-bar / theme-picker surfaces, so every screen in the app renders
// these two primitives while no preview showed either of them.
//
// Both are enum- or slot-driven: a screenshot of one screen shows exactly one of
// six tones and one of four SectionCard shapes. Rendering the variant set side by
// side is the thing a full-screen capture structurally cannot do, which is why
// these earn a preview and a one-off leaf row does not.
// ---------------------------------------------------------------------------

/**
 * Every [StatusTone] at once. The tint fill comes from `MilewayRoles.tint(role)`, so this is
 * also the contrast check: a label that disappears into its own chip shows up here first.
 */
@PreviewLightDark
@Composable
fun PreviewStatusChipTones() {
    PreviewSurface {
        FlowRow(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusTone.entries.forEach { tone ->
                StatusChip(label = tone.name, tone = tone)
            }
        }
    }
}

/**
 * The four header shapes [SectionCard] resolves between: bare content, title only,
 * title + subtitle + leading icon, and a trailing action. Callers pick these by
 * omitting parameters, so the branches are easy to get wrong and invisible one at a time.
 */
@PreviewLightDark
@Composable
fun PreviewSectionCardHeaderVariants() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            SectionCard {
                Text("No header at all", style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = "Title only") {
                Text("Header row with just a title", style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(
                title = "This month",
                subtitle = "1,284 km reimbursable",
                leadingIcon = Icons.Filled.Insights,
            ) {
                Text("Title, subtitle and leading icon", style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(
                title = "Recent claims",
                leadingIcon = Icons.Filled.Receipt,
                trailingAction = { TextButton(onClick = {}) { Text("See all") } },
            ) {
                Text("Trailing action in the header row", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
