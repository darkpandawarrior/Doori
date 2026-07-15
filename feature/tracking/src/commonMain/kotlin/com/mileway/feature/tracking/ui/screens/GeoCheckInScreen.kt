@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.util.haversineMeters
import com.mileway.core.maps.MapSurface
import com.mileway.core.platform.UrlOpener
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_copy
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_geo_additional_details
import com.mileway.core.ui.resources.tracking_geo_checkin_subtitle
import com.mileway.core.ui.resources.tracking_geo_checkin_title
import com.mileway.core.ui.resources.tracking_geo_checkin_type
import com.mileway.core.ui.resources.tracking_geo_nearby_locations
import com.mileway.core.ui.resources.tracking_geo_override_checkin
import com.mileway.core.ui.resources.tracking_geo_select_type
import com.mileway.core.ui.resources.tracking_manual_checkin_current_location
import com.mileway.core.ui.resources.tracking_open_in_maps
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.repository.HardwareEventRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Full-screen geo check-in flow. Shows demo location, a type picker, nearby vendor chips,
 * a dynamic form, and an optional radius warning when outside the geofence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoCheckInScreen(
    onBack: () -> Unit,
    configManager: TrackingConfigManager = koinInject(),
    hardwareEventRepository: HardwareEventRepository = koinInject(),
    mapSurface: MapSurface = koinInject(),
) {
    val clipboardManager = LocalClipboardManager.current
    val urlOpener = koinInject<UrlOpener>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val checkInTypes = remember { configManager.getCheckInTypes() }
    val vendors = remember { configManager.getVendorCenters() }
    val demoLat = configManager.getDemoLat()
    val demoLng = configManager.getDemoLng()
    val checkInRadius = configManager.getGeoCheckInRadiusMeters()

    var selectedType by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedVendorId by remember { mutableStateOf<String?>(null) }
    var formValues by remember { mutableStateOf(mapOf<String, String>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Sort vendors by Haversine distance from demo location
    val sortedVendors =
        remember(demoLat, demoLng) {
            vendors.sortedBy { haversineMeters(demoLat, demoLng, it.lat, it.lng) }
        }
    val selectedVendor = sortedVendors.firstOrNull { it.id == selectedVendorId }
    val vendorDistanceM = selectedVendor?.let { haversineMeters(demoLat, demoLng, it.lat, it.lng) }
    val formSchema =
        remember(selectedType) {
            selectedType?.let { configManager.getCheckInFormSchema(it) } ?: emptyList()
        }
    val formValid = formSchema.isEmpty() || formSchema.all { (key, _) -> !formValues[key].isNullOrBlank() }
    val canCheckIn = selectedType != null && formValid

    fun copyCoords() {
        clipboardManager.setText(AnnotatedString("$demoLat, $demoLng"))
        scope.launch { snackbarHostState.showSnackbar("Coordinates copied") }
    }

    fun openInMaps() {
        urlOpener.open("geo:$demoLat,$demoLng?q=$demoLat,$demoLng")
    }

    fun doCheckIn() {
        if (!canCheckIn) return
        scope.launch {
            isSubmitting = true
            delay(1_200)
            val locationLabel = selectedVendor?.name ?: "Current Location"
            hardwareEventRepository.insert(
                HardwareEvent(
                    token = "checkin_${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
                    eventType = EventType.CHECK_IN,
                    time = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    lat = demoLat,
                    lng = demoLng,
                    event = "Geo check-in at $locationLabel ($selectedType)",
                ),
            )
            isSubmitting = false
            snackbarHostState.showSnackbar("Checked in at $locationLabel ✓")
            delay(600)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.tracking_geo_checkin_title),
                subtitle = stringResource(Res.string.tracking_geo_checkin_subtitle),
                titleIcon = Icons.Default.LocationOn,
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.tracking_cd_back))
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                if (vendorDistanceM != null && vendorDistanceM > checkInRadius) {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(DesignTokens.Spacing.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(DesignTokens.Spacing.s))
                            Text(
                                text =
                                    "You are ${vendorDistanceM.toInt()} m away from ${selectedVendor.name}. " +
                                        "Check-in radius is ${checkInRadius.toInt()} m.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                        OutlinedButton(
                            shape = DesignTokens.Shape.button,
                            onClick = { doCheckIn() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(Res.string.tracking_geo_override_checkin)) }
                        Button(
                            shape = DesignTokens.Shape.button,
                            onClick = { doCheckIn() },
                            modifier = Modifier.weight(1f),
                            enabled = canCheckIn && !isSubmitting,
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(Res.string.tracking_geo_checkin_title))
                            }
                        }
                    }
                } else {
                    Button(
                        shape = DesignTokens.Shape.button,
                        onClick = { doCheckIn() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canCheckIn && !isSubmitting,
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(Res.string.tracking_geo_checkin_title))
                        }
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
            // ── Location Card ───────────────────────────────────────────────
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
                            stringResource(Res.string.tracking_manual_checkin_current_location),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    // Map preview centred on the demo check-in location (provider = active flavor)
                    mapSurface.LocationPinMap(
                        latitude = demoLat,
                        longitude = demoLng,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(DesignTokens.Shape.button),
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
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
                        OutlinedButton(
                            shape = DesignTokens.Shape.button,
                            onClick = { openInMaps() },
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.tracking_open_in_maps))
                        }
                        OutlinedButton(
                            shape = DesignTokens.Shape.button,
                            onClick = { copyCoords() },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.tracking_action_copy))
                        }
                    }
                }
            }

            // ── Check-In Type Picker ────────────────────────────────────────
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
                    Text(
                        stringResource(Res.string.tracking_geo_checkin_type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedType ?: stringResource(Res.string.tracking_geo_select_type),
                            onValueChange = {},
                            readOnly = true,
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
                                        formValues = emptyMap()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Nearby Locations ────────────────────────────────────────────
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
                    Text(
                        stringResource(Res.string.tracking_geo_nearby_locations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        sortedVendors.take(7).forEach { vendor ->
                            val distM = haversineMeters(demoLat, demoLng, vendor.lat, vendor.lng)
                            val isSelected = vendor.id == selectedVendorId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedVendorId = if (isSelected) null else vendor.id
                                },
                                label = {
                                    Column {
                                        Text(vendor.name, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "${distM.toLong()} m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color =
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                        )
                                    }
                                },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            )
                        }
                    }
                }
            }

            // ── Custom Form ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = selectedType != null && formSchema.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
            ) {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    ) {
                        Text(
                            stringResource(Res.string.tracking_geo_additional_details),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        formSchema.forEach { (label, hint) ->
                            val value = formValues[label] ?: ""
                            OutlinedTextField(
                                value = value,
                                onValueChange = { formValues = formValues + (label to it) },
                                label = { Text(label) },
                                placeholder = { Text(hint) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) { Text("${value.length}/100") }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}
