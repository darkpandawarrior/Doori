package com.mileway.feature.profile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.vehicle.GarageVehicle
import com.mileway.core.data.vehicle.VehicleCatalog
import com.mileway.core.data.vehicle.VehicleMakeModelCatalog
import com.mileway.core.data.vehicle.VehicleServices
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.GarageVerification
import com.mileway.feature.profile.viewmodel.VehicleGarageViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P11.2: the vehicle garage — vehicle list with an active-vehicle switch, per-vehicle
 * service-set checkboxes, an aggregate verification chip (VEHICLE-category documents), an
 * add-vehicle form (seeded make/model + photo), and — for multi-vehicle (gig) personas — a per-
 * vehicle availability-window editor. All local (Room + registry). Reached from a plugin-gated
 * profile-hub tile; the screen is a defensive no-op surface if opened without vehicles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleGarageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSelfAudit: (String) -> Unit = {},
    viewModel: VehicleGarageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GarageHeader(onBack = onBack, verification = state.verification)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                items(state.vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        availabilityEditorEnabled = state.availabilityEditorEnabled,
                        selfAuditEnabled = state.selfAuditEnabled,
                        onSetActive = { viewModel.setActive(vehicle.id) },
                        onRemove = { viewModel.removeVehicle(vehicle.id) },
                        onToggleService = { viewModel.toggleService(vehicle.id, it) },
                        onSetAvailability = { start, end, rate -> viewModel.setAvailability(vehicle.id, start, end, rate) },
                        onClearAvailability = { viewModel.clearAvailability(vehicle.id) },
                        onOpenSelfAudit = { onOpenSelfAudit(vehicle.id) },
                    )
                }
                if (state.canAddVehicle) {
                    item {
                        OutlinedButton(onClick = { showAddSheet = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(DesignTokens.Spacing.s))
                            Text(grv("garage_add_vehicle", "Add vehicle"))
                        }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }

    if (showAddSheet) {
        AddVehicleSheet(
            onAdd = { brand, model, reg, year, color, seats, typeKey, photo ->
                viewModel.addVehicle(brand, model, reg, year, color, seats, typeKey, photo)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }
}

@Composable
private fun GarageHeader(
    onBack: () -> Unit,
    verification: GarageVerification,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = grv("garage_back", "Back"))
        }
        Text(
            grv("garage_title", "My garage"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        VerificationChip(verification)
    }
}

@Composable
private fun VerificationChip(verification: GarageVerification) {
    val (label, color, icon) =
        when (verification) {
            GarageVerification.VERIFIED -> Triple(grv("garage_verified", "Verified"), Color(0xFF16A34A), Icons.Default.CheckCircle)
            GarageVerification.PENDING -> Triple(grv("garage_pending", "Pending"), Color(0xFFEA580C), Icons.Default.HourglassEmpty)
            GarageVerification.INCOMPLETE -> Triple(grv("garage_incomplete", "Incomplete"), Color(0xFFB91C1C), Icons.Default.Warning)
        }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}

@Composable
private fun VehicleCard(
    vehicle: GarageVehicle,
    availabilityEditorEnabled: Boolean,
    selfAuditEnabled: Boolean,
    onSetActive: () -> Unit,
    onRemove: () -> Unit,
    onToggleService: (String) -> Unit,
    onSetAvailability: (Int, Int, Double) -> Unit,
    onClearAvailability: () -> Unit,
    onOpenSelfAudit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = vehicle.isActive, onClick = onSetActive)
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                Column(modifier = Modifier.weight(1f)) {
                    Text(vehicle.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${vehicle.registrationNumber} • ${vehicle.color} • ${vehicle.seats} ${grv("garage_seats", "seats")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (vehicle.isActive) {
                    Text(grv("garage_active", "Active"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = grv("garage_remove", "Remove"))
                }
            }
            Spacer(Modifier.padding(top = DesignTokens.Spacing.xs))
            Text(grv("garage_services", "Used for"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                VehicleServices.all.forEach { service ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = service in vehicle.services, onCheckedChange = { onToggleService(service) })
                        Text(serviceLabel(service), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (availabilityEditorEnabled) {
                AvailabilityEditor(
                    vehicle = vehicle,
                    onSet = onSetAvailability,
                    onClear = onClearAvailability,
                )
            }
            if (selfAuditEnabled) {
                Spacer(Modifier.padding(top = DesignTokens.Spacing.xs))
                OutlinedButton(onClick = onOpenSelfAudit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text(grv("garage_self_audit", "Self-audit"))
                }
            }
        }
    }
}

/** Gig-driver P2P availability window: a simple start/end (HH:mm minute-of-day) + ₹/hour editor. */
@Composable
private fun AvailabilityEditor(
    vehicle: GarageVehicle,
    onSet: (Int, Int, Double) -> Unit,
    onClear: () -> Unit,
) {
    val existing = vehicle.availability
    var start by remember(vehicle.id) { mutableStateOf(existing?.startMinute?.let(::minutesToHhmm) ?: "09:00") }
    var end by remember(vehicle.id) { mutableStateOf(existing?.endMinute?.let(::minutesToHhmm) ?: "18:00") }
    var rate by remember(vehicle.id) { mutableStateOf(existing?.ratePerHour?.takeIf { it >= 0 }?.toInt()?.toString() ?: "80") }

    Spacer(Modifier.padding(top = DesignTokens.Spacing.s))
    Text(grv("garage_availability", "Availability (rentable)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = start, onValueChange = {
            start = it
        }, label = { Text(grv("garage_from", "From")) }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = end,
            onValueChange = { end = it },
            label = { Text(grv("garage_to", "To")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it.filter(Char::isDigit) },
            label = { Text(grv("garage_rate_per_hour", "₹ / hour")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (existing != null) {
            TextButton(onClick = onClear) { Text(grv("garage_clear", "Clear")) }
        }
        TextButton(onClick = {
            val s = hhmmToMinutes(start)
            val e = hhmmToMinutes(end)
            if (s != null && e != null) onSet(s, e, rate.toDoubleOrNull() ?: 0.0)
        }) { Text(grv("garage_save", "Save")) }
    }
    existing?.let {
        Text(
            grv("garage_availability_set", "Available {from}–{to} at ₹{rate}/hr")
                .replace("{from}", minutesToHhmm(it.startMinute))
                .replace("{to}", minutesToHhmm(it.endMinute))
                .replace("{rate}", it.ratePerHour.toInt().toString()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleSheet(
    onAdd: (String, String, String, Int, String, Int, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var brand by remember { mutableStateOf(VehicleMakeModelCatalog.makes.first()) }
    var model by remember { mutableStateOf(VehicleMakeModelCatalog.modelsFor(brand).first()) }
    var typeKey by remember { mutableStateOf(VehicleCatalog.entries.first().key) }
    var reg by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2022") }
    var color by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("2") }
    var photo by remember { mutableStateOf("") }

    val photoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) photo = uri.toString()
        }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l).padding(bottom = DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(grv("garage_add_title", "Add a vehicle"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LabeledDropdown(
                label = grv("garage_brand", "Brand"),
                selected = brand,
                options = VehicleMakeModelCatalog.makes,
                onSelect = {
                    brand = it
                    model = VehicleMakeModelCatalog.modelsFor(it).first()
                },
            )
            LabeledDropdown(
                label = grv("garage_model", "Model"),
                selected = model,
                options = VehicleMakeModelCatalog.modelsFor(brand),
                onSelect = { model = it },
            )
            LabeledDropdown(
                label = grv("garage_type", "Vehicle type"),
                selected = vehicleTypeLabel(typeKey),
                options = VehicleCatalog.entries.map { it.key },
                optionLabel = { vehicleTypeLabel(it) },
                onSelect = { typeKey = it },
            )
            OutlinedTextField(value = reg, onValueChange = {
                reg = it.uppercase()
            }, label = { Text(grv("garage_reg", "Registration number")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    label = { Text(grv("garage_year", "Year")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = seats,
                    onValueChange = { seats = it.filter(Char::isDigit).take(2) },
                    label = { Text(grv("garage_seats_label", "Seats")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(value = color, onValueChange = {
                color = it
            }, label = { Text(grv("garage_color", "Colour")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (photo.isBlank()) grv("garage_add_photo", "Add photo") else grv("garage_photo_added", "Photo added ✓"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(grv("garage_cancel", "Cancel")) }
                FilterChip(
                    selected = true,
                    onClick = {
                        if (reg.isNotBlank() && color.isNotBlank()) {
                            onAdd(brand, model, reg, year.toIntOrNull() ?: 2022, color, seats.toIntOrNull() ?: 2, typeKey, photo)
                        }
                    },
                    label = { Text(grv("garage_save_vehicle", "Save vehicle")) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    optionLabel: (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected, modifier = Modifier.weight(1f))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun serviceLabel(service: String): String =
    when (service) {
        VehicleServices.COMMUTE -> "Commute"
        VehicleServices.BUSINESS -> "Business"
        VehicleServices.DELIVERY -> "Delivery"
        else -> service
    }

private fun vehicleTypeLabel(key: String): String = key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }

private fun minutesToHhmm(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = (minutes % 60).coerceIn(0, 59)
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

private fun hhmmToMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** Screen-internal labels via the dynamic resolver with an English fallback (no generated symbols). */
@Composable
private fun grv(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
