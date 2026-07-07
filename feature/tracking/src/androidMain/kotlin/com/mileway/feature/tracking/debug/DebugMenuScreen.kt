package com.mileway.feature.tracking.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenuScreen(
    onBack: () -> Unit,
    onOpenHttpInspector: (() -> Unit)? = null,
    onOpenNetworkLog: (() -> Unit)? = null,
    onOpenShowcase: (() -> Unit)? = null,
    viewModel: DebugMenuComposeViewModel = koinViewModel(),
    configProvider: ConfigProvider = koinInject(),
    heapUsedMb: Long = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
    heapTotalMb: Long = Runtime.getRuntime().totalMemory() / (1024 * 1024),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val uiState = ui.debugMenuUiState
    val searchQuery = ui.searchQuery
    val availableProfiles = ui.availableProfiles
    val selectedProfile = ui.selectedProfile
    val context = LocalContext.current

    var showRestartDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var restartResult by remember { mutableStateOf<ApplyResult?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Developer Options",
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onAction(DebugMenuComposeAction.UpdateSearchQuery(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search options") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onAction(DebugMenuComposeAction.UpdateSearchQuery("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )
            }

            // Config snapshot, always shown when not searching
            if (searchQuery.isEmpty()) {
                item {
                    ConfigSnapshotCard(
                        uiState = uiState,
                        configProvider = configProvider,
                    )
                }
            }

            // Performance / memory
            if (searchQuery.isEmpty()) {
                item {
                    PerformanceCard(
                        onRunGc = { viewModel.onAction(DebugMenuComposeAction.RunGarbageCollection) },
                        usedMb = heapUsedMb,
                        totalMb = heapTotalMb,
                    )
                }
            }

            // Profile presets
            if (searchQuery.isEmpty()) {
                item {
                    ProfilePresetsCard(
                        profiles = availableProfiles,
                        selectedProfile = selectedProfile,
                        onProfileSelected = { viewModel.onAction(DebugMenuComposeAction.SelectProfile(it)) },
                    )
                }
            }

            // Tracking / location options
            item {
                DebugSectionCard(
                    title = "Location & Tracking",
                    icon = Icons.Default.LocationOn,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.trackingOptions.forEach { (name, enabled) ->
                            if (searchMatches(name, searchQuery)) {
                                DebugToggleRow(
                                    title = name,
                                    checked = enabled,
                                    onCheckedChange = { viewModel.onAction(DebugMenuComposeAction.ToggleTrackingOption(name)) },
                                )
                            }
                        }
                    }
                }
            }

            // Feature flags
            item {
                DebugSectionCard(
                    title = "Feature Flags",
                    icon = Icons.Default.Flag,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.featureOptions.forEach { (name, enabled) ->
                            if (searchMatches(name, searchQuery)) {
                                DebugToggleRow(
                                    title = name,
                                    checked = enabled,
                                    onCheckedChange = { viewModel.onAction(DebugMenuComposeAction.ToggleFeatureOption(name)) },
                                )
                            }
                        }
                    }
                }
            }

            // Network inspector (WormaCeptor), only shown when host app wires the intent
            if (searchQuery.isEmpty() && onOpenHttpInspector != null) {
                item {
                    NetworkInspectorCard(onOpen = onOpenHttpInspector)
                }
            }

            // V21 §3 Wave 4: local network log (NetworkLogScreen) — request/response history +
            // curl replay + API tester, backed by NetworkLogStore rather than an external intent.
            if (searchQuery.isEmpty() && onOpenNetworkLog != null) {
                item {
                    NetworkLogCard(onOpen = onOpenNetworkLog)
                }
            }

            // Component showcase browser, only shown when host app wires the launcher
            if (searchQuery.isEmpty() && onOpenShowcase != null) {
                item {
                    ShowcaseBrowserCard(onOpen = onOpenShowcase)
                }
            }

            // Actions: location export, reset, restart
            if (searchQuery.isEmpty()) {
                item {
                    OfflineActionsCard(
                        onExportLastTrack = { viewModel.exportMostRecentTrack(context) },
                        onClearCache = { viewModel.clearAppCache(context) },
                        onResetDefaults = { showClearAllDialog = true },
                        onRestart = {
                            val result = viewModel.applyChanges()
                            restartResult = result
                            showRestartDialog = true
                        },
                    )
                }
            }
        }
    }

    // Restart confirmation
    if (showRestartDialog) {
        val changeNote = restartResult?.let { "${it.changeCount} debug option(s) toggled in-memory. " }.orEmpty()
        ActionConfirmationBottomSheet(
            title = "Restart app?",
            description = "${changeNote}The app will close and relaunch. Any unsaved work may be lost.",
            confirmLabel = "Restart",
            dismissLabel = "Not now",
            icon = Icons.Default.RestartAlt,
            tone = ActionConfirmationToneType.Warning,
            onConfirm = {
                showRestartDialog = false
                viewModel.performAppRestart(context)
            },
            onDismiss = { showRestartDialog = false },
        )
    }

    // Reset-to-defaults confirmation
    if (showClearAllDialog) {
        ActionConfirmationBottomSheet(
            title = "Reset to defaults?",
            description = "All debug toggles will be reset to their default state.",
            confirmLabel = "Reset",
            dismissLabel = "Cancel",
            icon = Icons.Default.RestartAlt,
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                viewModel.onAction(DebugMenuComposeAction.ClearAllDebugSettings)
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Config snapshot card, shows offline-relevant flags from ConfigProvider
// ---------------------------------------------------------------------------

@Composable
private fun ConfigSnapshotCard(
    uiState: DebugMenuUiState,
    configProvider: ConfigProvider,
) {
    val trackCfg = configProvider.getTrackMilesConfig()
    val logCfg = configProvider.getLogMilesConfig()

    val snapshot =
        buildConfigSnapshot(
            uiState = uiState,
            trackMilesV2 = trackCfg.trackMilesV2,
            geoCheckIn = trackCfg.geoCheckInEnabled,
            manualCheckIn = trackCfg.allowManualCheckIn,
            currency = configProvider.getCurrency(),
            tenant = trackCfg.tenantCode,
            service = logCfg.service,
            allowMockLocations = uiState.trackingOptions["Allow Mock Locations"] == true,
        )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Config Snapshot",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (uiState.enabledOptionsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "${uiState.enabledOptionsCount} on",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            snapshot.forEach { (key, value) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** Pure function that builds the key-value snapshot shown in the config card. */
fun buildConfigSnapshot(
    uiState: DebugMenuUiState,
    trackMilesV2: Boolean,
    geoCheckIn: Boolean,
    manualCheckIn: Boolean,
    currency: String,
    tenant: String,
    service: String,
    allowMockLocations: Boolean,
): Map<String, String> =
    buildMap {
        put("Tenant", tenant.ifBlank { "—" })
        put("Currency", currency.ifBlank { "—" })
        put("Service", service.ifBlank { "—" })
        put("Track Miles V2", if (trackMilesV2) "enabled" else "disabled")
        put("Geo check-in", if (geoCheckIn) "on" else "off")
        put("Manual check-in", if (manualCheckIn) "on" else "off")
        put("Mock locations (debug)", if (allowMockLocations) "allowed" else "blocked")
        put("Debug flags active", uiState.enabledOptionsCount.toString())
    }

// ---------------------------------------------------------------------------
// Performance card, GC trigger + memory readout
// ---------------------------------------------------------------------------

@Composable
internal fun PerformanceCard(
    onRunGc: () -> Unit,
    usedMb: Long = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024),
    totalMb: Long = Runtime.getRuntime().totalMemory() / (1024 * 1024),
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Heap used: ${usedMb}MB / ${totalMb}MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRunGc,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Run garbage collection")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Profile presets card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePresetsCard(
    profiles: List<DebugProfile>,
    selectedProfile: DebugProfile?,
    onProfileSelected: (DebugProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Debug Profiles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Apply a preset bundle of debug flags in one tap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    value = selectedProfile?.name ?: "Select a profile",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors =
                        ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true,
                            ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = profile.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onProfileSelected(profile)
                                expanded = false
                            },
                            trailingIcon = {
                                if (selectedProfile?.name == profile.name) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // Show which flags the active profile turns on
            selectedProfile?.let { profile ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Active: ${profile.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        val enabledKeys = profile.options.filter { it.value }.keys
                        if (enabledKeys.isNotEmpty()) {
                            Text(
                                text = enabledKeys.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Network inspector card (WormaCeptor), only shown when callback is wired
// ---------------------------------------------------------------------------

@Composable
private fun NetworkInspectorCard(onOpen: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Network Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Inspect live HTTP traffic, headers, and response bodies",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open WormaCeptor")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Network log card (NetworkLogScreen), only shown when callback is wired
// ---------------------------------------------------------------------------

@Composable
private fun NetworkLogCard(onOpen: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Network Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Recorded requests, curl replay, and a minimal API tester",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Network Log")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Component showcase card, launch the debug component browser
// ---------------------------------------------------------------------------

@Composable
private fun ShowcaseBrowserCard(onOpen: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Component Showcase",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Browse all UI components in isolation with mock data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Showcase Browser")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Offline actions card
// ---------------------------------------------------------------------------

@Composable
private fun OfflineActionsCard(
    onExportLastTrack: () -> Unit,
    onClearCache: () -> Unit,
    onResetDefaults: () -> Unit,
    onRestart: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Debug Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            DebugActionRow(
                icon = Icons.Default.Share,
                title = "Export most recent track",
                description = "Share the latest saved track as a file",
                onClick = onExportLastTrack,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            DebugActionRow(
                icon = Icons.Default.Delete,
                title = "Clear app cache",
                description = "Wipe cached files (export cache, etc.)",
                onClick = onClearCache,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            DebugActionRow(
                icon = Icons.Default.Settings,
                title = "Reset debug flags to defaults",
                description = "Turn off all toggled options",
                onClick = onResetDefaults,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            DebugActionRow(
                icon = Icons.Default.PowerSettingsNew,
                title = "Apply & restart app",
                description = "Restart now so flag changes take effect",
                onClick = onRestart,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared sub-composables
// ---------------------------------------------------------------------------

@Composable
fun DebugSectionCard(
    title: String,
    icon: ImageVector = Icons.Default.Settings,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = { onCheckedChange() })
        },
        modifier = Modifier.clickable { onCheckedChange() },
    )
}

@Composable
private fun DebugActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

// ---------------------------------------------------------------------------
// Helpers (pure, safe to unit-test)
// ---------------------------------------------------------------------------

fun searchMatches(
    name: String,
    query: String,
): Boolean = query.isBlank() || name.contains(query, ignoreCase = true)

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Debug Section Card – collapsed")
@Composable
private fun PreviewDebugSectionCardCollapsed() {
    com.mileway.core.ui.previews.PreviewSurface {
        DebugSectionCard(title = "Location & Tracking", icon = Icons.Default.LocationOn) {
            Text("Toggle content hidden until expanded")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Debug Toggle Row")
@Composable
private fun PreviewDebugToggleRow() {
    com.mileway.core.ui.previews.PreviewSurface {
        DebugToggleRow(title = "Allow Mock Locations", checked = true, onCheckedChange = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Network Inspector Card")
@Composable
private fun PreviewNetworkInspectorCard() {
    com.mileway.core.ui.previews.PreviewSurface {
        NetworkInspectorCard(onOpen = {})
    }
}
