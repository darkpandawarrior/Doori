package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.plugin.PersonaSummary
import com.mileway.core.data.plugin.PluginCategory
import com.mileway.core.data.plugin.PluginKind
import com.mileway.core.data.plugin.PluginSource
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.plugin.PluginValueSpec
import com.mileway.core.data.plugin.ResolvedPlugin
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.viewmodel.PluginManagerAction
import com.mileway.feature.profile.viewmodel.PluginManagerViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * PLAN_V24 P0.3 — the Master Plugin page. Searchable, sectioned by [PluginCategory], one row per
 * plugin with a live source chip (Default/Preset/Custom/Forced), a persona-preset switcher, a
 * "reset to persona defaults" action, and an experimental section behind a 7-tap version-row
 * unlock. Every string is resolved dynamically by key ([uiText]) so plugins registered by later
 * phases surface here with zero screen changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    onBack: () -> Unit,
    viewModel: PluginManagerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPersonaSheet by remember { mutableStateOf(false) }
    var pendingPersona by remember { mutableStateOf<PersonaSummary?>(null) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = uiText("plugins_title", "Plugins"),
                subtitle = uiText("plugins_subtitle", "Every feature, toggled live"),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.Filled.Extension,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = uiText("plugins_back", "Back"),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = DesignTokens.Spacing.xl),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onAction(PluginManagerAction.Search(it)) },
                    label = { Text(uiText("plugins_search_hint", "Search plugins")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = DesignTokens.Shape.roundedSm,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                )
            }

            if (state.restartPendingCount > 0) {
                item {
                    RestartBanner(count = state.restartPendingCount)
                }
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    TextButton(
                        onClick = { showPersonaSheet = true },
                        shape = DesignTokens.Shape.button,
                        enabled = state.personas.isNotEmpty(),
                    ) { Text(uiText("plugins_apply_persona", "Apply persona")) }
                    TextButton(
                        onClick = { viewModel.onAction(PluginManagerAction.ResetToPreset) },
                        shape = DesignTokens.Shape.button,
                    ) { Text(uiText("plugins_reset_all", "Reset to persona defaults")) }
                }
            }

            val visible = state.visiblePlugins
            PluginCategory.entries.forEach { category ->
                val rows = visible.filter { it.descriptor.category == category }
                if (rows.isNotEmpty()) {
                    item(key = "cat_${category.name}") {
                        SectionLabel(categoryLabel(category))
                    }
                    items(rows, key = { it.descriptor.id }) { resolved ->
                        PluginRow(
                            resolved = resolved,
                            onToggle = { on -> viewModel.onAction(PluginManagerAction.SetToggle(resolved.descriptor.id, on)) },
                            onSetValue = { value -> viewModel.onAction(PluginManagerAction.SetValue(resolved.descriptor.id, value)) },
                            onClear = { viewModel.onAction(PluginManagerAction.ClearOverride(resolved.descriptor.id)) },
                        )
                    }
                }
            }

            item {
                VersionRow(
                    unlocked = state.experimentalUnlocked,
                    onTap = { viewModel.onAction(PluginManagerAction.VersionRowTap) },
                )
            }
        }
    }

    if (showPersonaSheet) {
        AppActionSheet(
            onDismiss = { showPersonaSheet = false },
            title = uiText("plugins_apply_persona", "Apply persona"),
        ) {
            state.personas.forEach { persona ->
                ListItem(
                    headlineContent = { Text(uiText(persona.nameKey, persona.id)) },
                    supportingContent = { Text(uiText(persona.descriptionKey, "")) },
                    modifier =
                        Modifier.clickable {
                            showPersonaSheet = false
                            pendingPersona = persona
                        },
                )
            }
        }
    }

    val persona = pendingPersona
    if (persona != null) {
        ActionConfirmationBottomSheet(
            title = uiText("plugins_persona_dialog_title", "Apply ${uiText(persona.nameKey, persona.id)}?", uiText(persona.nameKey, persona.id)),
            description = uiText("plugins_persona_dialog_desc", "Applies this persona's plugin mix to the current account."),
            confirmLabel = uiText("plugins_persona_clear", "Clear my changes first"),
            dismissLabel = uiText("plugins_persona_keep", "Keep my changes"),
            tone = ActionConfirmationToneType.Warning,
            onConfirm = { _ ->
                viewModel.onAction(PluginManagerAction.ApplyPersona(persona, clearFirst = true))
                pendingPersona = null
            },
            onDismiss = {
                // "Keep my changes": still apply the persona, layered over existing overrides.
                viewModel.onAction(PluginManagerAction.ApplyPersona(persona, clearFirst = false))
                pendingPersona = null
            },
        )
    }
}

@Composable
private fun PluginRow(
    resolved: ResolvedPlugin,
    onToggle: (Boolean) -> Unit,
    onSetValue: (PluginValue) -> Unit,
    onClear: () -> Unit,
) {
    val descriptor = resolved.descriptor
    val title = uiText("${descriptor.titleKey}", prettify(descriptor.id))
    val description = uiText(descriptor.descriptionKey, "")

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs)) {
                if (description.isNotBlank()) Text(description)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    SourceChip(resolved.source)
                    if (descriptor.requiresRestart) {
                        Chip(uiText("plugins_restart_chip", "Restart"), DesignTokens.StatusColors.warning)
                    }
                    if (resolved.source == PluginSource.USER) {
                        Text(
                            text = uiText("plugins_clear_override", "Reset"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onClear() },
                        )
                    }
                }
                // VALUE plugins: an inline editor under the row (none registered yet in P0.1).
                if (descriptor.kind == PluginKind.VALUE) {
                    ValueEditor(resolved, onSetValue)
                }
            }
        },
        trailingContent = {
            if (descriptor.kind != PluginKind.VALUE) {
                Switch(
                    checked = (resolved.value as? PluginValue.Bool)?.value ?: descriptor.defaultOn,
                    onCheckedChange = onToggle,
                )
            }
        },
    )
}

@Composable
private fun ValueEditor(
    resolved: ResolvedPlugin,
    onSetValue: (PluginValue) -> Unit,
) {
    when (val spec = resolved.descriptor.valueSpec) {
        is PluginValueSpec.IntSpec -> {
            val current = (resolved.value as? PluginValue.IntVal)?.value ?: spec.defaultValue
            Column {
                Text("$current${spec.unit?.let { " $it" } ?: ""}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = current.toFloat(),
                    onValueChange = { onSetValue(PluginValue.IntVal(it.roundToInt())) },
                    valueRange = spec.min.toFloat()..spec.max.toFloat(),
                    steps = ((spec.max - spec.min) / spec.step - 1).coerceAtLeast(0),
                )
            }
        }
        is PluginValueSpec.DoubleSpec -> {
            val current = (resolved.value as? PluginValue.DoubleVal)?.value ?: spec.defaultValue
            Column {
                Text("$current${spec.unit?.let { " $it" } ?: ""}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = current.toFloat(),
                    onValueChange = { onSetValue(PluginValue.DoubleVal(it.toDouble())) },
                    valueRange = spec.min.toFloat()..spec.max.toFloat(),
                )
            }
        }
        is PluginValueSpec.EnumSpec -> {
            val current = (resolved.value as? PluginValue.Str)?.value ?: spec.defaultValue
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                spec.options.forEach { option ->
                    TextButton(onClick = { onSetValue(PluginValue.Str(option)) }, shape = DesignTokens.Shape.button) {
                        Text(
                            option,
                            fontWeight = if (option == current) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        null -> Unit
    }
}

@Composable
private fun SourceChip(source: PluginSource) {
    val (label, color) =
        when (source) {
            PluginSource.DEFAULT -> uiText("plugins_source_default", "Default") to MaterialTheme.colorScheme.onSurfaceVariant
            PluginSource.PRESET -> uiText("plugins_source_preset", "Preset") to MaterialTheme.colorScheme.primary
            PluginSource.USER -> uiText("plugins_source_custom", "Custom") to DesignTokens.StatusColors.success
            PluginSource.FORCED -> uiText("plugins_source_forced", "Forced") to DesignTokens.StatusColors.warning
        }
    Chip(label, color)
}

@Composable
private fun Chip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(color = color.copy(alpha = 0.12f), shape = DesignTokens.Shape.chip) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun RestartBanner(count: Int) {
    Surface(
        color = DesignTokens.StatusColors.warning.copy(alpha = 0.12f),
        shape = DesignTokens.Shape.roundedMd,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.xs),
    ) {
        Text(
            text = uiText("plugins_restart_banner", "$count change(s) need a restart to apply", count),
            style = MaterialTheme.typography.bodySmall,
            color = DesignTokens.StatusColors.warning,
            modifier = Modifier.padding(DesignTokens.Spacing.m),
        )
    }
}

@Composable
private fun VersionRow(
    unlocked: Boolean,
    onTap: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(uiText("plugins_version_row", "Registry v1", "1")) },
        supportingContent = {
            Text(
                if (unlocked) {
                    uiText("plugins_experimental_section", "Experimental section unlocked")
                } else {
                    uiText("plugins_experimental_hint", "Tap 7 times to reveal experimental plugins")
                },
            )
        },
        modifier = Modifier.clickable(onClick = onTap),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
    )
}

@Composable
private fun categoryLabel(category: PluginCategory): String =
    when (category) {
        PluginCategory.AUTH -> uiText("plugins_cat_auth", "Authentication")
        PluginCategory.ONBOARDING -> uiText("plugins_cat_onboarding", "Onboarding")
        PluginCategory.PROFILE -> uiText("plugins_cat_profile", "Profile")
        PluginCategory.VERIFICATION -> uiText("plugins_cat_verification", "Verification")
        PluginCategory.GROWTH -> uiText("plugins_cat_growth", "Growth & rewards")
        PluginCategory.MEMBERSHIP -> uiText("plugins_cat_membership", "Membership")
        PluginCategory.LIFECYCLE -> uiText("plugins_cat_lifecycle", "Account lifecycle")
        PluginCategory.PAYMENTS -> uiText("plugins_cat_payments", "Payments")
        PluginCategory.TRACKING -> uiText("plugins_cat_tracking", "Tracking")
        PluginCategory.TRACKING_TUNING -> uiText("plugins_cat_tracking_tuning", "Tracking tuning")
        PluginCategory.VEHICLES -> uiText("plugins_cat_vehicles", "Vehicles")
        PluginCategory.ENGAGEMENT -> uiText("plugins_cat_engagement", "Engagement")
        PluginCategory.BANNERS -> uiText("plugins_cat_banners", "Banners")
        PluginCategory.CORE_MODULES -> uiText("plugins_cat_core_modules", "Core modules")
    }

/** Resolve a string by resource-name key, falling back if the key isn't defined yet. */
@Composable
private fun uiText(
    key: String,
    fallback: String,
    vararg args: Any,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return if (args.isEmpty()) stringResource(resource) else stringResource(resource, *args)
}

private fun prettify(id: String): String = id.replaceFirstChar { it.uppercase() }
