package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.settings.StorageArea
import com.mileway.core.data.settings.StorageRepository
import com.mileway.core.data.settings.formatStorageBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** P31.MISC.2: one [StorageArea] pre-formatted for display. */
data class StorageAreaUi(
    val area: StorageArea,
    val sizeLabel: String,
)

data class StorageManagementUiState(
    val areas: List<StorageAreaUi> = emptyList(),
    val clearingAreaId: String? = null,
    val lastClearedAreaId: String? = null,
)

/** P31.MISC.2: state for the storage-management screen — [StorageRepository.storageAreas] tiered
 * Safe/Caution/Danger, each with a working clear action. Caution/Danger clears are gated by a
 * confirmation sheet in the UI layer; this ViewModel only performs the clear once confirmed. */
class StorageManagementViewModel(private val repository: StorageRepository) : ViewModel() {
    private val _state = MutableStateFlow(StorageManagementUiState())
    val state: StateFlow<StorageManagementUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val areas =
                withContext(Dispatchers.IO) { repository.storageAreas() }
                    .map { StorageAreaUi(it, formatStorageBytes(it.bytes)) }
            _state.update { it.copy(areas = areas) }
        }
    }

    /** Clears [areaId] (already confirmed by the caller for Caution/Danger tiers), then refreshes. */
    fun clearArea(areaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(clearingAreaId = areaId) }
            withContext(Dispatchers.IO) { repository.clearArea(areaId) }
            refresh()
            _state.update { it.copy(clearingAreaId = null, lastClearedAreaId = areaId) }
        }
    }

    fun clearLastClearedFlag() {
        _state.update { it.copy(lastClearedAreaId = null) }
    }
}
