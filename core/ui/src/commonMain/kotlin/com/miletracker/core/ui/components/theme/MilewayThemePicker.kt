package com.miletracker.core.ui.components.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.MilewayTheme

/**
 * Design Language v2 theme picker — the curated [MilewayTheme] gallery (Matrix / Amoled / Ion /
 * Daybreak). Each option is a self-previewing swatch card painted in **its own** scheme colours
 * (canvas, card, accent, text) so the user sees what they're choosing before applying it, and the
 * selected card carries the accent glow ring.
 *
 * Part of the shared design system (`core:ui`) rather than forked per-feature, so Settings/Profile
 * and any future surface render the same picker. Accessible: each swatch is a [Role.RadioButton]
 * with a self-describing [contentDescription] and a ≥48dp touch target via the card height.
 */
@Composable
fun MilewayThemePicker(
    selected: MilewayTheme,
    onSelect: (MilewayTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        // Two columns: pack the four curated themes into a tidy 2x2 gallery.
        MilewayTheme.entries.chunked(2).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                rowThemes.forEach { theme ->
                    ThemeSwatchCard(
                        theme = theme,
                        isSelected = theme == selected,
                        onSelect = { onSelect(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** A single self-previewing theme card, painted in its own scheme. */
@Composable
private fun ThemeSwatchCard(
    theme: MilewayTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = theme.spec
    val ringWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "themeSwatchRing",
    )
    val ringColor = if (isSelected) spec.accent else spec.border

    Column(
        modifier =
            modifier
                .clip(DesignTokens.Shape.roundedMd)
                .background(spec.canvas)
                .border(ringWidth, ringColor, DesignTokens.Shape.roundedMd)
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                )
                .clearAndSetSemantics {
                    this.selected = isSelected
                    contentDescription =
                        "${theme.label} theme. ${theme.description}" +
                            if (isSelected) " Selected." else ""
                }
                .padding(DesignTokens.Spacing.m),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        // Mini preview "card" stacked on the canvas, with an accent dot and faux text rows.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.Shape.roundedSm)
                    .background(spec.surfaceCard)
                    .border(1.dp, spec.border, DesignTokens.Shape.roundedSm)
                    .padding(DesignTokens.Spacing.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(spec.accent),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SwatchLine(color = spec.text, widthFraction = 1f)
                    SwatchLine(color = spec.textMuted, widthFraction = 0.6f)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = theme.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = spec.text,
            )
            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(spec.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = spec.onAccent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/** A faux text row in the swatch preview — a rounded bar in the given colour. */
@Composable
private fun SwatchLine(
    color: Color,
    widthFraction: Float,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .size(width = 1.dp, height = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.85f)),
    )
}
