package com.mileway.feature.profile.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.dialog.ColorWheelDialog
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.components.theme.MilewayThemePicker
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_settings_about
import com.mileway.core.ui.resources.profile_settings_account
import com.mileway.core.ui.resources.profile_settings_aggressive_gps
import com.mileway.core.ui.resources.profile_settings_aggressive_gps_desc
import com.mileway.core.ui.resources.profile_settings_app_version
import com.mileway.core.ui.resources.profile_settings_back
import com.mileway.core.ui.resources.profile_settings_battery_aware
import com.mileway.core.ui.resources.profile_settings_battery_aware_desc
import com.mileway.core.ui.resources.profile_settings_custom_color
import com.mileway.core.ui.resources.profile_settings_custom_color_desc
import com.mileway.core.ui.resources.profile_settings_custom_theme_color
import com.mileway.core.ui.resources.profile_settings_customization
import com.mileway.core.ui.resources.profile_settings_dark_theme
import com.mileway.core.ui.resources.profile_settings_dark_theme_desc
import com.mileway.core.ui.resources.profile_settings_delete_account
import com.mileway.core.ui.resources.profile_settings_delete_account_desc
import com.mileway.core.ui.resources.profile_settings_demo_build
import com.mileway.core.ui.resources.profile_settings_denied
import com.mileway.core.ui.resources.profile_settings_developer
import com.mileway.core.ui.resources.profile_settings_developer_options
import com.mileway.core.ui.resources.profile_settings_developer_options_desc
import com.mileway.core.ui.resources.profile_settings_distance_units
import com.mileway.core.ui.resources.profile_settings_employee_code
import com.mileway.core.ui.resources.profile_settings_experimental
import com.mileway.core.ui.resources.profile_settings_granted
import com.mileway.core.ui.resources.profile_settings_low_end
import com.mileway.core.ui.resources.profile_settings_low_end_desc
import com.mileway.core.ui.resources.profile_settings_map_provider
import com.mileway.core.ui.resources.profile_settings_map_provider_desc
import com.mileway.core.ui.resources.profile_settings_material_you_desc
import com.mileway.core.ui.resources.profile_settings_notifications
import com.mileway.core.ui.resources.profile_settings_off
import com.mileway.core.ui.resources.profile_settings_organization
import com.mileway.core.ui.resources.profile_settings_palette_style
import com.mileway.core.ui.resources.profile_settings_perm_denied
import com.mileway.core.ui.resources.profile_settings_permission_health
import com.mileway.core.ui.resources.profile_settings_plugins
import com.mileway.core.ui.resources.profile_settings_plugins_desc
import com.mileway.core.ui.resources.profile_settings_preferences
import com.mileway.core.ui.resources.profile_settings_recommended
import com.mileway.core.ui.resources.profile_settings_reset
import com.mileway.core.ui.resources.profile_settings_reset_confirm
import com.mileway.core.ui.resources.profile_settings_reset_desc
import com.mileway.core.ui.resources.profile_settings_reset_sheet_desc
import com.mileway.core.ui.resources.profile_settings_subtitle
import com.mileway.core.ui.resources.profile_settings_theme
import com.mileway.core.ui.resources.profile_settings_theme_color
import com.mileway.core.ui.resources.profile_settings_theme_color_custom
import com.mileway.core.ui.resources.profile_settings_title
import com.mileway.core.ui.resources.profile_settings_trip_reminders_on
import com.mileway.core.ui.resources.profile_settings_unit_km
import com.mileway.core.ui.resources.profile_settings_unit_miles
import com.mileway.core.ui.resources.profile_settings_use_system_colors
import com.mileway.core.ui.resources.settings_language
import com.mileway.core.ui.theme.AccentPalette
import com.mileway.core.ui.theme.AppLanguage
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.LocaleController
import com.mileway.core.ui.theme.PaletteStyleNames
import com.mileway.core.ui.theme.parseHexColor
import com.mileway.feature.profile.model.SettingsUiState
import com.mileway.feature.profile.model.computePermissionHealth
import com.mileway.feature.profile.ui.components.SyncDiagnosticsCard
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import android.provider.Settings as SystemSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDebugMenu: () -> Unit = {},
    onOpenPlugins: () -> Unit = {},
    onOpenAccountDeletion: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val darkOverride by viewModel.darkThemeOverride.collectAsStateWithLifecycle()
    val useMiles by viewModel.useMiles.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val profile by viewModel.uiState.collectAsStateWithLifecycle()
    val milewayTheme by viewModel.milewayTheme.collectAsStateWithLifecycle()
    val accentPalette by viewModel.accentPalette.collectAsStateWithLifecycle()
    val customSeedHex by viewModel.customSeedHex.collectAsStateWithLifecycle()
    val useSystemColors by viewModel.useSystemColors.collectAsStateWithLifecycle()
    val paletteStyle by viewModel.paletteStyle.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val experimentalFlags by viewModel.experimentalFlags.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val about = SettingsUiState(darkThemeOverride = darkOverride, useMiles = useMiles)

    var showPalettePicker by remember { mutableStateOf(false) }
    var showColorWheel by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val permSnackbarState = remember { SnackbarHostState() }
    val permScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(permSnackbarState) },
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_settings_title),
                subtitle = stringResource(Res.string.profile_settings_subtitle),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.Filled.Settings,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.profile_settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            val permissionRows = rememberPermissionHealthRows()
            val permDeniedMessage = stringResource(Res.string.profile_settings_perm_denied)
            val requestPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (!granted) {
                        permScope.launch {
                            permSnackbarState.showSnackbar(permDeniedMessage)
                        }
                    }
                }
            PermissionHealthSection(
                rows = permissionRows,
                onPermissionToggle = { row ->
                    val manifestPermission = manifestPermissionFor(row.name)
                    if (row.isGranted || manifestPermission == null) {
                        // Already granted, or nothing to request on this API level/permission (e.g.
                        // scoped-storage Storage) — only a real Settings toggle can change it.
                        context.startActivity(
                            Intent(SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    } else {
                        requestPermissionLauncher.launch(manifestPermission)
                    }
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            SyncDiagnosticsCard()
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))
            SettingsSectionLabel(stringResource(Res.string.profile_settings_account))
            val header = profile.header
            ListItem(
                headlineContent = { Text(header.name) },
                supportingContent = { Text(header.email) },
            )
            if (header.code.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.profile_settings_employee_code)) },
                    supportingContent = { Text(header.code) },
                )
            }
            if (header.tenant.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.profile_settings_organization)) },
                    supportingContent = { Text(header.tenant) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel(stringResource(Res.string.profile_settings_preferences))
            // The curated theme dictates light/dark (e.g. Daybreak is light, Matrix is dark), so the
            // manual override is superseded while one is active — surface that instead of a no-op switch.
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_dark_theme)) },
                supportingContent = {
                    Text(stringResource(Res.string.profile_settings_dark_theme_desc, milewayTheme.label))
                },
                trailingContent = {
                    Switch(
                        checked = !milewayTheme.isLight,
                        enabled = false,
                        onCheckedChange = {},
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_distance_units)) },
                supportingContent = {
                    Text(
                        if (useMiles) {
                            stringResource(Res.string.profile_settings_unit_miles)
                        } else {
                            stringResource(Res.string.profile_settings_unit_km)
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = useMiles,
                        onCheckedChange = { viewModel.toggleUnits() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_notifications)) },
                supportingContent = {
                    Text(
                        if (notificationsEnabled) {
                            stringResource(Res.string.profile_settings_trip_reminders_on)
                        } else {
                            stringResource(Res.string.profile_settings_off)
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            // ----------------------------------------------------------------
            // Customization section
            // ----------------------------------------------------------------
            SettingsSectionLabel(stringResource(Res.string.profile_settings_customization))

            // Design Language v2 — curated theme gallery (Matrix / Amoled / Ion / Daybreak).
            // Self-previewing swatches; picking one applies the hand-tuned, AA-verified scheme.
            Text(
                text = stringResource(Res.string.profile_settings_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        start = DesignTokens.Spacing.l,
                        end = DesignTokens.Spacing.l,
                        top = DesignTokens.Spacing.s,
                        bottom = DesignTokens.Spacing.xs,
                    ),
            )
            MilewayThemePicker(
                selected = milewayTheme,
                onSelect = { viewModel.setMilewayTheme(it) },
                modifier =
                    Modifier.padding(
                        horizontal = DesignTokens.Spacing.l,
                        vertical = DesignTokens.Spacing.xs,
                    ),
            )

            // Theme colour — preset seed the whole scheme is generated from
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_theme_color)) },
                supportingContent = {
                    Text(
                        if (customSeedHex.isBlank()) {
                            accentPalette.label
                        } else {
                            stringResource(Res.string.profile_settings_theme_color_custom, customSeedHex)
                        },
                    )
                },
                trailingContent = {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(
                                    parseHexColor(customSeedHex)
                                        ?: parseHexColor(accentPalette.seedHex)
                                        ?: MaterialTheme.colorScheme.primary,
                                ),
                    )
                },
                modifier = Modifier.clickable { showPalettePicker = true },
            )

            // Fully custom seed via colour wheel
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_custom_color)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_custom_color_desc)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { showColorWheel = true },
            )

            // Palette generation style
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_palette_style)) },
                supportingContent = { Text(paletteStyle) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { showStylePicker = true },
            )

            // Android 12+ wallpaper-derived colours
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.profile_settings_use_system_colors)) },
                    supportingContent = { Text(stringResource(Res.string.profile_settings_material_you_desc)) },
                    trailingContent = {
                        Switch(
                            checked = useSystemColors,
                            onCheckedChange = { viewModel.setUseSystemColors(it) },
                        )
                    },
                )
            }

            // Language / locale
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_language)) },
                supportingContent = { Text(language.displayName) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { showLanguagePicker = true },
            )

            // Map provider (OSM only, multi-provider toggle not meaningful)
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_map_provider)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_map_provider_desc)) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            // Experimental toggles
            SettingsSectionLabel(stringResource(Res.string.profile_settings_experimental))

            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_battery_aware)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_battery_aware_desc)) },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.batteryAwareTracking,
                        onCheckedChange = { viewModel.toggleBatteryAwareTracking() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_low_end)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_low_end_desc)) },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.lowEndDeviceTuning,
                        onCheckedChange = { viewModel.toggleLowEndDeviceTuning() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_aggressive_gps)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_aggressive_gps_desc)) },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.aggressiveGpsFilter,
                        onCheckedChange = { viewModel.toggleAggressiveGpsFilter() },
                    )
                },
            )

            // Reset customization
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_reset)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_reset_desc)) },
                modifier = Modifier.clickable { showResetConfirm = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel(stringResource(Res.string.profile_settings_about))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_settings_app_version)) },
                supportingContent = { Text(about.appVersion) },
            )
            Text(
                text = stringResource(Res.string.profile_settings_demo_build),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = DesignTokens.Spacing.l,
                        vertical = DesignTokens.Spacing.s,
                    ),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel(stringResource(Res.string.profile_settings_developer))
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text(stringResource(Res.string.profile_settings_plugins)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_plugins_desc)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenPlugins),
            )
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text(stringResource(Res.string.profile_settings_developer_options)) },
                supportingContent = { Text(stringResource(Res.string.profile_settings_developer_options_desc)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenDebugMenu),
            )

            // ----------------------------------------------------------------
            // PLAN_V24 P7.1: Danger zone — account deletion lifecycle
            // ----------------------------------------------------------------
            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                headlineContent = {
                    Text(
                        stringResource(Res.string.profile_settings_delete_account),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                supportingContent = { Text(stringResource(Res.string.profile_settings_delete_account_desc)) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenAccountDeletion),
            )
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    if (showPalettePicker) {
        SimpleSelectionDialog(
            title = stringResource(Res.string.profile_settings_theme_color),
            options = AccentPalette.entries.map { it.label },
            selected = if (customSeedHex.isBlank()) accentPalette.label else "",
            onDismiss = { showPalettePicker = false },
            onSelect = { label ->
                showPalettePicker = false
                AccentPalette.entries.firstOrNull { it.label == label }?.let { viewModel.setPalette(it) }
            },
        )
    }

    if (showColorWheel) {
        ColorWheelDialog(
            selectedColor =
                parseHexColor(customSeedHex)
                    ?: parseHexColor(accentPalette.seedHex)
                    ?: MaterialTheme.colorScheme.primary,
            showHexcode = true,
            title = stringResource(Res.string.profile_settings_custom_theme_color),
            onDismiss = { showColorWheel = false },
            onColorSelected = { _, hex ->
                showColorWheel = false
                viewModel.setCustomSeed(hex)
            },
        )
    }

    if (showStylePicker) {
        SimpleSelectionDialog(
            title = stringResource(Res.string.profile_settings_palette_style),
            options = PaletteStyleNames,
            selected = paletteStyle,
            onDismiss = { showStylePicker = false },
            onSelect = { style ->
                showStylePicker = false
                viewModel.setPaletteStyle(style)
            },
        )
    }

    if (showLanguagePicker) {
        SimpleSelectionDialog(
            title = stringResource(Res.string.settings_language),
            options = AppLanguage.entries.map { it.displayName },
            selected = language.displayName,
            onDismiss = { showLanguagePicker = false },
            onSelect = { displayName ->
                showLanguagePicker = false
                val picked = AppLanguage.entries.firstOrNull { it.displayName == displayName }
                if (picked != null) {
                    viewModel.setLanguage(picked)
                    // UX.6: update the shared app-wide locale state (features observe LocaleController.currentTag).
                    localeController.setLanguage(picked)
                    // Wire per-app locale via AppCompatDelegate, persisted by the platform.
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(picked.tag),
                    )
                }
            },
        )
    }

    if (showResetConfirm) {
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.profile_settings_reset),
            description = stringResource(Res.string.profile_settings_reset_sheet_desc),
            confirmLabel = stringResource(Res.string.profile_settings_reset_confirm),
            tone = ActionConfirmationToneType.Warning,
            onConfirm = {
                showResetConfirm = false
                viewModel.resetCustomization()
                // Also reset locale to English
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(AppLanguage.ENGLISH.tag),
                )
            },
            onDismiss = { showResetConfirm = false },
        )
    }
}

@Composable
private fun SimpleSelectionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AppActionSheet(
        onDismiss = onDismiss,
        title = title,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            TextButton(
                shape = DesignTokens.Shape.button,
                onClick = { onSelect(option) },
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs),
            ) {
                Text(
                    text = option,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    style =
                        if (isSelected) {
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(
                horizontal = DesignTokens.Spacing.l,
                vertical = DesignTokens.Spacing.s,
            ),
    )
}

@Composable
private fun PermissionHealthSection(
    rows: List<PermissionHealthRow>,
    onPermissionToggle: (PermissionHealthRow) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val summary = computePermissionHealth(rows.map { it.toHealthEntry() })

    SettingsSectionLabel(stringResource(Res.string.profile_settings_permission_health))

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val sweep = 360f * (summary.healthScorePercent / 100f)
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    "${summary.healthScorePercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                val allRequiredGranted = summary.requiredGranted == summary.requiredTotal
                Text(
                    "${summary.healthScorePercent}%: " +
                        if (allRequiredGranted) {
                            "All required permissions granted"
                        } else {
                            "${summary.requiredTotal - summary.requiredGranted} required permission(s) missing"
                        },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    val requiredMark = if (allRequiredGranted) " ✓" else ""
                    PermChip(
                        label = "Required ${summary.requiredGranted}/${summary.requiredTotal}$requiredMark",
                        color = if (allRequiredGranted) DesignTokens.StatusColors.success else DesignTokens.StatusColors.error,
                    )
                    PermChip(
                        label = "Recommended ${summary.recommendedGranted}/${summary.recommendedTotal}",
                        color = DesignTokens.StatusColors.warning,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(DesignTokens.Spacing.m))

    rows.forEach { row ->
        PermissionCard(entry = row, onToggle = { onPermissionToggle(row) })
    }
}

private fun PermissionHealthRow.toHealthEntry() =
    com.mileway.feature.profile.model.PermissionHealthEntry(
        name = name,
        isRequired = isRequired,
        isGranted = isGranted,
    )

@Composable
private fun PermChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = DesignTokens.Shape.chip,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PermissionCard(
    entry: PermissionHealthRow,
    onToggle: () -> Unit,
) {
    val grantedColor = if (entry.isGranted) DesignTokens.StatusColors.success else DesignTokens.StatusColors.error
    val grantedLabel =
        if (entry.isGranted) {
            stringResource(Res.string.profile_settings_granted)
        } else {
            stringResource(Res.string.profile_settings_denied)
        }
    ListItem(
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(DesignTokens.Shape.button)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        headlineContent = { Text(entry.name) },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Surface(
                    color = grantedColor.copy(alpha = 0.12f),
                    shape = DesignTokens.Shape.chip,
                ) {
                    Text(
                        text = grantedLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = grantedColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (!entry.isRequired) {
                    Text(
                        stringResource(Res.string.profile_settings_recommended),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        },
        trailingContent = {
            Switch(
                checked = entry.isGranted,
                onCheckedChange = { onToggle() },
            )
        },
    )
}
