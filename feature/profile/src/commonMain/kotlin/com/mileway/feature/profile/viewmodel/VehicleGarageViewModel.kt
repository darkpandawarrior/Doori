package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.vehicle.AvailabilityWindow
import com.mileway.core.data.vehicle.GarageRepository
import com.mileway.core.data.vehicle.GarageVehicle
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.data.verification.DocumentCategory
import com.mileway.feature.profile.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Aggregate verification state of the garage's VEHICLE-category documents. */
enum class GarageVerification { VERIFIED, PENDING, INCOMPLETE }

data class VehicleGarageUiState(
    val vehicles: List<GarageVehicle> = emptyList(),
    val multipleVehiclesEnabled: Boolean = false,
    /** The gig-driver availability editor is shown only for multi-vehicle (gig) personas. */
    val availabilityEditorEnabled: Boolean = false,
    val verification: GarageVerification = GarageVerification.INCOMPLETE,
    /** P12.6: the per-vehicle self-audit entry shows only when the `selfAudit` plugin is on. */
    val selfAuditEnabled: Boolean = false,
) {
    /** In single-vehicle mode, the add affordance hides once a vehicle exists. */
    val canAddVehicle: Boolean get() = multipleVehiclesEnabled || vehicles.isEmpty()
}

/**
 * PLAN_V24 P11.2 — drives the vehicle garage. Vehicles come from the shared [GarageRepository]
 * (core:data); the per-vehicle verification chip aggregates the VEHICLE-category documents from
 * [DocumentRepository]; `multipleVehiclesEnabled` gates single- vs multi-vehicle mode and the
 * gig availability editor. All local — Room + registry, no backend.
 */
class VehicleGarageViewModel(
    private val garage: GarageRepository,
    private val documents: DocumentRepository,
    private val pluginRegistry: PluginRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(VehicleGarageUiState())
    val state: StateFlow<VehicleGarageUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            garage.seedIfEmpty()
            documents.seedIfEmpty()
        }
        combine(
            garage.observeAll(),
            pluginRegistry.observe("multipleVehiclesEnabled"),
            documents.observeAll(),
            pluginRegistry.observe("selfAudit"),
        ) { vehicles, multiEnabled, docs, selfAudit ->
            _state.value.copy(
                vehicles = vehicles,
                multipleVehiclesEnabled = multiEnabled,
                availabilityEditorEnabled = multiEnabled,
                verification = aggregateVerification(docs),
                selfAuditEnabled = selfAudit,
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addVehicle(
        brand: String,
        model: String,
        registrationNumber: String,
        year: Int,
        color: String,
        seats: Int,
        vehicleTypeKey: String,
        photoUri: String,
    ) {
        val vehicle =
            GarageVehicle(
                id = "veh_${Uuid.random()}",
                brand = brand,
                model = model,
                registrationNumber = registrationNumber,
                year = year,
                color = color,
                seats = seats,
                vehicleTypeKey = vehicleTypeKey,
                photoUri = photoUri,
                isActive = false,
                services = setOf(com.mileway.core.data.vehicle.VehicleServices.COMMUTE),
                availability = null,
                createdAtMs = 0L,
            )
        viewModelScope.launch { garage.add(vehicle) }
    }

    fun removeVehicle(id: String) {
        viewModelScope.launch { garage.remove(id) }
    }

    fun setActive(id: String) {
        viewModelScope.launch { garage.setActive(id) }
    }

    fun toggleService(
        id: String,
        service: String,
    ) {
        val vehicle = _state.value.vehicles.firstOrNull { it.id == id } ?: return
        val next = if (service in vehicle.services) vehicle.services - service else vehicle.services + service
        viewModelScope.launch { garage.setServices(id, next) }
    }

    fun setAvailability(
        id: String,
        startMinute: Int,
        endMinute: Int,
        ratePerHour: Double,
    ) {
        viewModelScope.launch { garage.setAvailability(id, AvailabilityWindow(startMinute, endMinute, ratePerHour)) }
    }

    fun clearAvailability(id: String) {
        viewModelScope.launch { garage.setAvailability(id, null) }
    }

    private fun aggregateVerification(docs: List<com.mileway.core.data.verification.VerificationDocument>): GarageVerification {
        val vehicleDocs = docs.filter { it.category == DocumentCategory.VEHICLE }
        if (vehicleDocs.isEmpty()) return GarageVerification.INCOMPLETE
        return when {
            vehicleDocs.all { it.status == DocStatus.VERIFIED } -> GarageVerification.VERIFIED
            vehicleDocs.any { it.status == DocStatus.APPROVAL_PENDING || it.status == DocStatus.UPLOADED } ->
                GarageVerification.PENDING
            else -> GarageVerification.INCOMPLETE
        }
    }
}
