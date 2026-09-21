package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.settings.StorageRepository
import com.mileway.core.data.settings.formatStorageBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PLAN_V22 P6.6: state for Preferences' "Storage" tile/sheet — a real, on-device cache-size
 * readout (never a hardcoded string) plus a working clear-cache action, replacing the tile's
 * previous `ProfileAction.RaisePreferenceMessage("Manage local data in the full app.")` snackbar
 * tap. [databaseLabel]/[cacheLabel]/[totalLabel] are pre-formatted via [formatStorageBytes] so the
 * screen never formats bytes itself.
 */
data class StorageUiState(
    val databaseLabel: String = "0 B",
    val cacheLabel: String = "0 B",
    val totalLabel: String = "0 B",
    val isClearing: Boolean = false,
    val didClear: Boolean = false,
)

class StorageViewModel(
    private val repository: StorageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the current on-device byte counts (called on init and after [clearCache]). */
    fun refresh() {
        viewModelScope.launch {
            val (databaseBytes, cacheBytes, totalBytes) =
                withContext(Dispatchers.IO) {
                    Triple(repository.databaseBytes(), repository.cacheBytes(), repository.totalBytes())
                }
            _state.update {
                it.copy(
                    databaseLabel = formatStorageBytes(databaseBytes),
                    cacheLabel = formatStorageBytes(cacheBytes),
                    totalLabel = formatStorageBytes(totalBytes),
                )
            }
        }
    }

    /** Deletes the app's cache directory contents, then refreshes the readout. */
    fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isClearing = true, didClear = false) }
            withContext(Dispatchers.IO) { repository.clearCache() }
            refresh()
            _state.update { it.copy(isClearing = false, didClear = true) }
        }
    }

    /** Dismisses the one-shot "cache cleared" confirmation without changing any persisted state. */
    fun clearDidClearFlag() {
        _state.update { it.copy(didClear = false) }
    }
}
