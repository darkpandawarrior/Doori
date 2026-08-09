package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_selected
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Tier 2 of the three-tier sheet system (see [MilewayActionSheet], [MilewayAlertDialog]): choose
 * one of a list — vehicle, purpose, office, delegate. A single tap both selects AND dismisses; on
 * purpose there is no "Done" button here — a Done button on a single-select list is a second tap
 * that buys nothing. Built on [AppActionSheet] for the shared modal chrome (title + insets); no
 * dismiss-guard needed here since a picker never holds unsaved data of its own.
 *
 * Takes plain data + lambdas only (no ViewModel), so the gallery can render it directly. For a
 * long/searchable list, reach for [SearchablePickerSheet] instead — this one is for the short,
 * scan-at-a-glance case the taxonomy names (vehicle, purpose, office, delegate).
 */
@Composable
fun <T> MilewayPickerSheet(
    title: String,
    options: List<MilewayPickerOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppActionSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = DesignTokens.IconSize.minTouchTarget)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { selectAndDismiss(option, onSelect, onDismiss) },
                        )
                        .padding(vertical = DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                option.icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DesignTokens.IconSize.actionTile),
                    )
                }
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(Res.string.core_cd_selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                }
            }
        }
    }
}

/** One choice in a [MilewayPickerSheet]: the value plus its label and an optional leading icon. */
data class MilewayPickerOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * [MilewayPickerSheet]'s single-select contract: choosing [option] fires exactly one
 * `onSelect(option.value)` immediately followed by exactly one `onDismiss()` — never a separate
 * "Done" step. Pulled out as a pure function (rather than two inline lambda calls in the row's
 * onClick) so the sequencing itself is unit-testable without Compose.
 */
fun <T> selectAndDismiss(
    option: MilewayPickerOption<T>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    onSelect(option.value)
    onDismiss()
}
