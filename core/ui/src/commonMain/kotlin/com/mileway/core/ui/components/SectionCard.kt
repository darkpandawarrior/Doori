package com.mileway.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * Reusable section card with consistent styling.
 *
 * Header row: optional leading icon in a tinted rounded container, title + optional
 * subtitle, and an optional trailing action slot. The card body is rendered below
 * the header via [content].
 */
@Composable
fun SectionCard(
    title: String = "",
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = DesignTokens.Shape.roundedSm,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.Spacing.l),
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = MaterialTheme.colorScheme.primary,
    leadingIconContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            // Header row, omitted when no title, subtitle, icon, or trailing action
            if (title.isNotEmpty() || subtitle != null || leadingIcon != null || trailingAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.Spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    ) {
                        if (leadingIcon != null) {
                            Surface(
                                color = leadingIconContainerColor,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = null,
                                    tint = leadingIconTint,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = titleColor,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    if (trailingAction != null) {
                        Box(modifier = Modifier.padding(start = DesignTokens.Spacing.s)) {
                            trailingAction()
                        }
                    }
                }
            }

            // Content area
            content()
        }
    }
}

/**
 * Collapsible variant of [SectionCard].
 *
 * The trailing action is a compact Show/Hide toggle with a rotating chevron; the
 * body expands and collapses with the card bounds animating via animateContentSize.
 */
@Composable
fun CollapsibleSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // Animate the rotation of the arrow icon
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow_rotation",
    )

    SectionCard(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        modifier = modifier.animateContentSize(),
        trailingAction = {
            // Rectangular toggle with 8dp corners
            Button(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(8.dp),
                contentPadding =
                    PaddingValues(
                        horizontal = DesignTokens.Spacing.m,
                        vertical = DesignTokens.Spacing.xs,
                    ),
                modifier = Modifier.height(32.dp),
            ) {
                Text(
                    text = if (expanded) "Hide" else "Show",
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier =
                        Modifier
                            .padding(start = DesignTokens.Spacing.xs)
                            .size(DesignTokens.IconSize.inline)
                            .rotate(rotation),
                )
            }
        },
    ) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.s),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                content()
            }
        }
    }
}
