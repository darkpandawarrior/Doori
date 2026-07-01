package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface DemoSettingsAction {
    data object ToggleSimulateRoot : DemoSettingsAction

    data object ToggleSimulateOffline : DemoSettingsAction

    data object ToggleBiometricGuard : DemoSettingsAction

    data object ToggleGpsDrift : DemoSettingsAction

    data object ToggleAutoDiscard : DemoSettingsAction
}

/** No one-shot effects for the settings toggles; present to satisfy the MVI contract. */
sealed interface DemoSettingsEffect

class DemoSettingsViewModel(
    private val repository: DemoSettingsRepository,
) : BaseViewModel<DemoSettings, DemoSettingsEffect, DemoSettingsAction>(DemoSettings()) {
    /** Backwards-compatible alias; screens read [state]. */
    val settings: StateFlow<DemoSettings> = state

    init {
        viewModelScope.launch {
            repository.settings.collect { s -> setState { s } }
        }
    }

    override fun onAction(action: DemoSettingsAction) {
        when (action) {
            DemoSettingsAction.ToggleSimulateRoot -> viewModelScope.launch { repository.toggleSimulateRoot() }
            DemoSettingsAction.ToggleSimulateOffline -> viewModelScope.launch { repository.toggleSimulateOffline() }
            DemoSettingsAction.ToggleBiometricGuard -> viewModelScope.launch { repository.toggleBiometricGuard() }
            DemoSettingsAction.ToggleGpsDrift -> viewModelScope.launch { repository.toggleGpsDrift() }
            DemoSettingsAction.ToggleAutoDiscard -> viewModelScope.launch { repository.toggleAutoDiscard() }
        }
    }
}
