package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.platform.UrlOpener
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.tracking.manager.TrackingConfigManager
import com.miletracker.feature.tracking.repository.HardwareEventRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Full-screen manual check-in: three-section scrollable form capturing reason, check-in type,
 * and an optional date/time override. Logs a [HardwareEvent] of type [EventType.CHECK_IN] on submit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckInScreen(
    onBack: () -> Unit,
    configManager: TrackingConfigManager = koinInject(),
    hardwareEventRepository: HardwareEventRepository = koinInject(),
) {
    val clipboardManager = LocalClipboardManager.current
    val urlOpener = koinInject<UrlOpener>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val checkInTypes = remember { configManager.getCheckInTypes() }
    val demoLat = configManager.getDemoLat()
    val demoLng = configManager.getDemoLng()

    var reason by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val canSubmit = reason.isNotBlank() && !isSubmitting

    fun copyCoords() {
        clipboardManager.setText(AnnotatedString("$demoLat, $demoLng"))
        scope.launch { snackbarHostState.showSnackbar("Coordinates copied") }
    }

    fun openInMaps() {
        urlOpener.open("geo:$demoLat,$demoLng?q=$demoLat,$demoLng")
    }

    fun refreshLocation() {
        scope.launch {
            isRefreshing = true
            delay(1_000)
            isRefreshing = false
            snackbarHostState.showSnackbar("Location refreshed")
        }
    }

    fun submitCheckIn() {
        scope.launch {
            isSubmitting = true
            delay(800)
            hardwareEventRepository.insert(
                HardwareEvent(
                    token = "manual_checkin_${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
                    eventType = EventType.CHECK_IN,
                    time = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    lat = demoLat,
                    lng = demoLng,
                    event = "Manual check-in${if (selectedType != null) " ($selectedType)" else ""}: $reason",
                ),
            )
            isSubmitting = false
            snackbarHostState.showSnackbar("Check-in submitted successfully")
            delay(500)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = "Manual Check-In",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .padding(DesignTokens.Spacing.l)
                        .padding(bottom = DesignTokens.Spacing.m),
            ) {
                Button(
                    onClick = { submitCheckIn() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Submit Check-In")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            // ── Current Location Card ───────────────────────────────────────
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(DesignTokens.Spacing.s))
                        Text(
                            "Current Location",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { refreshLocation() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh location")
                            }
                        }
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.xs))
                    Text(
                        text = "${((demoLat * 1_000_000).toLong() / 1_000_000.0)}, ${((demoLng * 1_000_000).toLong() / 1_000_000.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = configManager.getDemoAccuracyLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        OutlinedButton(onClick = { openInMaps() }) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open in Maps")
                        }
                        OutlinedButton(onClick = { copyCoords() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // ── Check-in Details Card ───────────────────────────────────────
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DesignTokens.Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Text(
                        "Check-in Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 200) reason = it },
                        label = { Text("Reason / Notes") },
                        placeholder = { Text("What's the reason for this check-in?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) { Text("${reason.length}/200") }
                        },
                    )

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedType ?: "Select check-in type (optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Check-in Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                        ) {
                            checkInTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        typeExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = "Now",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date & Time") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}
