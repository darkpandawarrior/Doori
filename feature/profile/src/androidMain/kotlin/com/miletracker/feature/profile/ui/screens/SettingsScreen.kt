package com.miletracker.feature.profile.ui.screens

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PinDrop
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.ui.components.dialog.ColorWheelDialog
import com.miletracker.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.miletracker.core.ui.components.sheet.ActionConfirmationToneType
import com.miletracker.core.ui.components.sheet.AppActionSheet
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.AccentPalette
import com.miletracker.core.ui.theme.AppLanguage
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.core.ui.theme.LocaleController
import com.miletracker.core.ui.theme.PaletteStyleNames
import com.miletracker.core.ui.theme.parseHexColor
import com.miletracker.feature.profile.model.SettingsUiState
import com.miletracker.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDebugMenu: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    localeController: LocaleController = koinInject(),
) {
    val darkOverride by viewModel.darkThemeOverride.collectAsStateWithLifecycle()
    val useMiles by viewModel.useMiles.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val profile by viewModel.uiState.collectAsStateWithLifecycle()
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
                title = "Settings",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            PermissionHealthSection(
                onPermissionToggle = {
                    permScope.launch {
                        permSnackbarState.showSnackbar("Permission changes require system settings.")
                    }
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))
            SettingsSectionLabel("Account")
            val header = profile.header
            ListItem(
                headlineContent = { Text(header.name) },
                supportingContent = { Text(header.email) },
            )
            if (header.code.isNotBlank()) {
                ListItem(
                    headlineContent = { Text("Employee code") },
                    supportingContent = { Text(header.code) },
                )
            }
            if (header.tenant.isNotBlank()) {
                ListItem(
                    headlineContent = { Text("Organization") },
                    supportingContent = { Text(header.tenant) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel("Preferences")
            ListItem(
                headlineContent = { Text("Dark theme") },
                supportingContent = { Text("Override the system theme") },
                trailingContent = {
                    Switch(
                        checked = darkOverride ?: systemDark,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Distance units") },
                supportingContent = { Text(if (useMiles) "Miles" else "Kilometers") },
                trailingContent = {
                    Switch(
                        checked = useMiles,
                        onCheckedChange = { viewModel.toggleUnits() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text(if (notificationsEnabled) "Trip reminders on" else "Off") },
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
            SettingsSectionLabel("Customization")

            // Theme colour, preset seed the whole scheme is generated from
            ListItem(
                headlineContent = { Text("Theme color") },
                supportingContent = {
                    Text(if (customSeedHex.isBlank()) accentPalette.label else "Custom ($customSeedHex)")
                },
                trailingContent = {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
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
                headlineContent = { Text("Custom color") },
                supportingContent = { Text("Pick any seed color with the color wheel") },
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
                headlineContent = { Text("Palette style") },
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
                    headlineContent = { Text("Use system colors") },
                    supportingContent = { Text("Material You — colors from your wallpaper") },
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
                headlineContent = { Text("Language") },
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
                headlineContent = { Text("Map provider") },
                supportingContent = { Text("OpenStreetMap (fixed — single provider build)") },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            // Experimental toggles
            SettingsSectionLabel("Experimental")

            ListItem(
                headlineContent = { Text("Battery-aware tracking") },
                supportingContent = { Text("Reduce GPS polling below 15% battery (real effect)") },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.batteryAwareTracking,
                        onCheckedChange = { viewModel.toggleBatteryAwareTracking() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Low-end device tuning") },
                supportingContent = { Text("Fewer UI animations (cosmetic in demo)") },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.lowEndDeviceTuning,
                        onCheckedChange = { viewModel.toggleLowEndDeviceTuning() },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Aggressive GPS filter") },
                supportingContent = { Text("Tighter spike rejection radius — 40 m vs 80 m (real effect)") },
                trailingContent = {
                    Switch(
                        checked = experimentalFlags.aggressiveGpsFilter,
                        onCheckedChange = { viewModel.toggleAggressiveGpsFilter() },
                    )
                },
            )

            // Reset customization
            ListItem(
                headlineContent = { Text("Reset customization") },
                supportingContent = { Text("Restore palette, language, and experimental flags to defaults") },
                modifier = Modifier.clickable { showResetConfirm = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel("About")
            ListItem(
                headlineContent = { Text("App version") },
                supportingContent = { Text(about.appVersion) },
            )
            Text(
                text = "Demo build — mock data only, no network calls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = DesignTokens.Spacing.l,
                        vertical = DesignTokens.Spacing.s,
                    ),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.s))

            SettingsSectionLabel("Developer")
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text("Developer options") },
                supportingContent = { Text("Debug flags, profiles, export tools") },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenDebugMenu),
            )
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    if (showPalettePicker) {
        SimpleSelectionDialog(
            title = "Theme color",
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
            title = "Custom theme color",
            onDismiss = { showColorWheel = false },
            onColorSelected = { _, hex ->
                showColorWheel = false
                viewModel.setCustomSeed(hex)
            },
        )
    }

    if (showStylePicker) {
        SimpleSelectionDialog(
            title = "Palette style",
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
            title = "Language",
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
            title = "Reset customization",
            description = "Palette, language, and experimental flags will return to defaults.",
            confirmLabel = "Reset",
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

private data class PermissionEntry(
    val name: String,
    val icon: ImageVector,
    val isRequired: Boolean,
    val isGranted: Boolean,
)

private val PERMISSIONS =
    listOf(
        PermissionEntry("Location (Precise)", Icons.Filled.LocationOn, isRequired = true, isGranted = true),
        PermissionEntry("Location (Background)", Icons.Filled.PinDrop, isRequired = true, isGranted = true),
        PermissionEntry("Camera", Icons.Filled.Camera, isRequired = true, isGranted = true),
        PermissionEntry("Storage", Icons.Filled.Folder, isRequired = true, isGranted = true),
        PermissionEntry("Notifications", Icons.Filled.NotificationsNone, isRequired = false, isGranted = true),
        PermissionEntry("Activity Recognition", Icons.AutoMirrored.Filled.DirectionsRun, isRequired = false, isGranted = true),
        PermissionEntry("Bluetooth", Icons.Filled.Bluetooth, isRequired = false, isGranted = false),
    )

@Composable
private fun PermissionHealthSection(onPermissionToggle: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    SettingsSectionLabel("Permission Health")

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
                    val sweep = 360f * 0.90f
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
                    "90%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "90% — All required permissions granted",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    PermChip(label = "Required 4/4 ✓", color = DesignTokens.StatusColors.success)
                    PermChip(label = "Recommended 3/4", color = DesignTokens.StatusColors.warning)
                }
            }
        }
    }

    Spacer(Modifier.height(DesignTokens.Spacing.m))

    PERMISSIONS.forEach { perm ->
        PermissionCard(entry = perm, onToggle = onPermissionToggle)
    }
}

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
    entry: PermissionEntry,
    onToggle: () -> Unit,
) {
    val grantedColor = if (entry.isGranted) DesignTokens.StatusColors.success else DesignTokens.StatusColors.error
    val grantedLabel = if (entry.isGranted) "GRANTED" else "DENIED"
    ListItem(
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
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
                        "recommended",
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
