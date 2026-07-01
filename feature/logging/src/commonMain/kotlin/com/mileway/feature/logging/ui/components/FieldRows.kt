@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.logging.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * A labelled "tap to open" field used for the Journey Date, Journey Completion
 * Time and Vehicle Type rows on Step 1. The label sits above a bordered value box
 * with an optional leading icon and a trailing affordance ([trailingIcon]).
 *
 * @param label        section label shown above the box
 * @param value        the current value text (or placeholder when empty)
 * @param onClick      invoked when the value box is tapped
 * @param leadingIcon  optional icon inside the value box
 * @param trailingIcon trailing affordance icon (defaults to a chevron)
 * @param isPlaceholder dims the value text when it is a placeholder
 */
@Composable
fun TapFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    isPlaceholder: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(DesignTokens.Spacing.s))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.Shape.roundedMd,
            color = MaterialTheme.colorScheme.surface,
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            onClick = onClick,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    leadingIcon?.let {
                        Icon(
                            it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DesignTokens.IconSize.navigation),
                        )
                        Spacer(Modifier.size(DesignTokens.Spacing.m))
                    }
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (isPlaceholder) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
        }
    }
}

/**
 * A header card describing the current step ("Step 1 of 2" + a subtitle), styled
 * as a tinted rounded surface to match the reference.
 */
@Composable
fun StepHeaderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
