package com.miletracker.core.ui.components.sheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Whether a [FilterSection] lets the user pick one option or several. */
enum class FilterSelectionMode { SINGLE, MULTI }

/** One selectable value inside a [FilterSection]. */
data class FilterOption(
    val key: String,
    val label: String,
)

/** A drill-down group in a [FilterBottomSheet] — e.g. "Status", "Category". */
data class FilterSection(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val mode: FilterSelectionMode,
    val options: List<FilterOption>,
)

/**
 * Reusable drill-down filter sheet. The Main screen lists each [FilterSection]
 * with its current selection count; tapping one slides ([AnimatedContent]) into that section's option list
 * (single = radio, multi = checkbox). Selections are *staged* locally and only handed to [onApply] when the
 * user presses Apply (apply-on-confirm), so dismissing discards. Generic over string keys, so any screen can
 * drive its own filter model through it.
 *
 * @param initialSelected sectionKey → currently-applied option keys.
 * @param onApply receives the full staged map (sectionKey → selected option keys).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    sections: List<FilterSection>,
    initialSelected: Map<String, Set<String>>,
    onApply: (Map<String, Set<String>>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Filter",
) {
    val staged: SnapshotStateMap<String, Set<String>> =
        remember {
            mutableStateMapOf<String, Set<String>>().apply {
                sections.forEach { put(it.key, initialSelected[it.key] ?: emptySet()) }
            }
        }
    var activeSection by remember { mutableStateOf<FilterSection?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
        ) {
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "filterDrillDown",
            ) { section ->
                if (section == null) {
                    FilterMain(
                        title = title,
                        sections = sections,
                        staged = staged,
                        onOpenSection = { activeSection = it },
                        onClear = { sections.forEach { s -> staged[s.key] = emptySet() } },
                        onApply = { onApply(staged.toMap()) },
                    )
                } else {
                    FilterSectionDetail(
                        section = section,
                        selected = staged[section.key].orEmpty(),
                        onToggle = { optionKey -> staged[section.key] = toggle(section, staged[section.key].orEmpty(), optionKey) },
                        onBack = { activeSection = null },
                    )
                }
            }
        }
    }
}

private fun toggle(
    section: FilterSection,
    current: Set<String>,
    optionKey: String,
): Set<String> =
    when (section.mode) {
        FilterSelectionMode.SINGLE -> setOf(optionKey)
        FilterSelectionMode.MULTI -> if (optionKey in current) current - optionKey else current + optionKey
    }

@Composable
private fun FilterMain(
    title: String,
    sections: List<FilterSection>,
    staged: Map<String, Set<String>>,
    onOpenSection: (FilterSection) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            sections.forEach { section ->
                val count = staged[section.key].orEmpty().size
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(false, role = Role.Button, onClick = { onOpenSection(section) }).padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Text(section.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (count > 0) {
                        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(8.dp)) {
            Text("Apply", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterSectionDetail(
    section: FilterSection,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            section.options.forEach { option ->
                val isSelected = option.key in selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle(option.key) })
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (section.mode == FilterSelectionMode.SINGLE) {
                        RadioButton(selected = isSelected, onClick = { onToggle(option.key) })
                    } else {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggle(option.key) })
                    }
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
