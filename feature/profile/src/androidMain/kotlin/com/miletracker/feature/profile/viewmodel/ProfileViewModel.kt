package com.miletracker.feature.profile.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.theme.AccentPalette
import com.miletracker.core.ui.theme.AppLanguage
import com.miletracker.core.ui.theme.ExperimentalFlags
import com.miletracker.core.ui.theme.MilewayTheme
import com.miletracker.core.ui.theme.ThemeController
import com.miletracker.feature.profile.model.AccountAnalyticsSnapshot
import com.miletracker.feature.profile.model.ProfileUiState
import com.miletracker.feature.profile.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ProfileAction {
    data class SwitchAccount(val id: String) : ProfileAction

    data object OpenSessionsDialog : ProfileAction

    data object DismissSessionsDialog : ProfileAction

    data object TogglePushNotifications : ProfileAction

    data object ToggleUsageAnalytics : ProfileAction

    data class RaisePreferenceMessage(val message: String) : ProfileAction

    data object ClearPreferenceMessage : ProfileAction
}

/** No one-shot effects; preference messages live in state. Present to satisfy the MVI contract. */
sealed interface ProfileEffect

/**
 * Backs the whole account suite (Account hub, Profile Details, Preferences, Settings/Help).
 * Its own state ([ProfileUiState]) is mutated only through [onAction]; theme-related reactive
 * state and setters delegate to [ThemeController] (a dedicated controller, not this VM's state),
 * so they remain passthrough properties/methods.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val themeController: ThemeController,
) : BaseViewModel<ProfileUiState, ProfileEffect, ProfileAction>(
        repository.accounts().let { acc ->
            ProfileUiState(
                header = repository.header(),
                profile = repository.richProfile(),
                completion = repository.completion(),
                sessions = repository.sessions(),
                accounts = acc,
                selectedAccountId = acc.firstOrNull()?.id.orEmpty(),
                analytics = AccountAnalyticsSnapshot.demo(),
            )
        },
    ) {
    /** Backwards-compatible alias; screens read [state]. */
    val uiState: StateFlow<ProfileUiState> = state

    override fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SwitchAccount -> setState { copy(selectedAccountId = action.id) }
            ProfileAction.OpenSessionsDialog -> setState { copy(showSessionsDialog = true) }
            ProfileAction.DismissSessionsDialog -> setState { copy(showSessionsDialog = false) }
            ProfileAction.TogglePushNotifications ->
                setState { copy(preferences = preferences.copy(pushNotifications = !preferences.pushNotifications)) }
            ProfileAction.ToggleUsageAnalytics ->
                setState { copy(preferences = preferences.copy(usageAnalytics = !preferences.usageAnalytics)) }
            is ProfileAction.RaisePreferenceMessage -> setState { copy(preferenceMessage = action.message) }
            ProfileAction.ClearPreferenceMessage -> setState { copy(preferenceMessage = null) }
        }
    }

    // ── Theme delegations (ThemeController owns this reactive state) ───────────

    /** `null` = follow system, `true` = force dark, `false` = force light. */
    val darkThemeOverride: StateFlow<Boolean?> = themeController.darkThemeOverride

    /** Currently selected curated theme (Design Language v2; default Matrix). */
    val milewayTheme: StateFlow<MilewayTheme> = themeController.milewayTheme

    val accentPalette: StateFlow<AccentPalette> = themeController.accentPalette

    val customSeedHex: StateFlow<String> = themeController.customSeedHex

    val useSystemColors: StateFlow<Boolean> = themeController.useSystemColors

    val paletteStyle: StateFlow<String> = themeController.paletteStyle

    val language: StateFlow<AppLanguage> = themeController.language

    val experimentalFlags: StateFlow<ExperimentalFlags> = themeController.experimentalFlags

    private val _useMiles = MutableStateFlow(true)
    val useMiles: StateFlow<Boolean> = _useMiles.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun toggleUnits() {
        _useMiles.value = !_useMiles.value
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    fun setDarkTheme(dark: Boolean?) = themeController.set(dark)

    fun setPalette(palette: AccentPalette) = themeController.setPalette(palette)

    fun setCustomSeed(hex: String) = themeController.setCustomSeed(hex)

    fun setUseSystemColors(enabled: Boolean) = themeController.setUseSystemColors(enabled)

    fun setPaletteStyle(style: String) = themeController.setPaletteStyle(style)

    fun setLanguage(language: AppLanguage) = themeController.setLanguage(language)

    fun toggleBatteryAwareTracking() {
        val current = themeController.experimentalFlags.value
        themeController.updateExperimentalFlags(current.copy(batteryAwareTracking = !current.batteryAwareTracking))
    }

    fun toggleLowEndDeviceTuning() {
        val current = themeController.experimentalFlags.value
        themeController.updateExperimentalFlags(current.copy(lowEndDeviceTuning = !current.lowEndDeviceTuning))
    }

    fun toggleAggressiveGpsFilter() {
        val current = themeController.experimentalFlags.value
        themeController.updateExperimentalFlags(current.copy(aggressiveGpsFilter = !current.aggressiveGpsFilter))
    }

    fun resetCustomization() = themeController.resetCustomization()
}
