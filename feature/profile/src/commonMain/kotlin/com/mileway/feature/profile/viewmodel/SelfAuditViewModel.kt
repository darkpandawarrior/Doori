package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.vehicle.GarageRepository
import com.mileway.core.data.vehicle.SelfAuditChecklist
import com.mileway.core.data.vehicle.SelfAuditRepository
import com.mileway.core.data.vehicle.VehicleAudit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P12.6 — drives one vehicle's self-audit. [checklist] is seeded from the vehicle's type;
 * the driver captures a photo per item and optionally notes an issue, then submits. Submitting
 * appends a [VehicleAudit] whose verdict [SelfAuditRepository] resolves via the shared review
 * simulator (a note containing the reject marker fails; anything else passes once the sim delay
 * elapses). All local — Room + registry, no backend.
 */
data class SelfAuditUiState(
    val vehicleName: String = "",
    val checklist: List<String> = emptyList(),
    val photos: Map<String, String> = emptyMap(),
    val note: String = "",
    val history: List<VehicleAudit> = emptyList(),
) {
    /** Ready to submit once every checklist item has a captured photo. */
    val canSubmit: Boolean get() = checklist.isNotEmpty() && photos.keys.containsAll(checklist)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SelfAuditViewModel(
    private val garage: GarageRepository,
    private val repository: SelfAuditRepository,
) : ViewModel() {
    private val vehicleId = MutableStateFlow<String?>(null)
    private val _state = MutableStateFlow(SelfAuditUiState())
    val state: StateFlow<SelfAuditUiState> = _state.asStateFlow()

    init {
        vehicleId.filterNotNull()
            .flatMapLatest { garage.observeAll() }
            .onEach { vehicles ->
                val vehicle = vehicles.firstOrNull { it.id == vehicleId.value }
                if (vehicle != null) {
                    _state.update {
                        it.copy(
                            vehicleName = vehicle.displayName,
                            checklist = SelfAuditChecklist.forVehicleType(vehicle.vehicleTypeKey),
                        )
                    }
                }
            }.launchIn(viewModelScope)

        vehicleId.filterNotNull()
            .flatMapLatest { id -> repository.observeForVehicle(id) }
            .onEach { audits -> _state.update { it.copy(history = audits) } }
            .launchIn(viewModelScope)
    }

    fun load(id: String) {
        vehicleId.value = id
    }

    fun setPhoto(
        item: String,
        uri: String,
    ) {
        _state.update { it.copy(photos = it.photos + (item to uri)) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun submit() {
        val id = vehicleId.value ?: return
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            repository.submit(id, current.photos.keys, current.note.trim())
            _state.update { it.copy(photos = emptyMap(), note = "") }
        }
    }
}
