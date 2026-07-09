package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.isSessionFresh
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.theme.AccentPalette
import com.mileway.core.ui.theme.AppLanguage
import com.mileway.core.ui.theme.ExperimentalFlags
import com.mileway.core.ui.theme.MilewayThemeVariant
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.model.AccountAnalyticsSnapshot
import com.mileway.feature.profile.model.NotificationChannels
import com.mileway.feature.profile.model.ProfileUiState
import com.mileway.feature.profile.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface ProfileAction {
    /**
     * P2.3: no longer switches immediately — gates on [DemoSettingsRepository.biometricGuardEnabled]
     * first. When enabled, emits [ProfileEffect.RequestBiometricGate] for the screen to run
     * `BiometricGuard`; when disabled, sets `pendingSwitchAccountId` so the screen shows
     * [SwitchAccountPinSheet][com.mileway.feature.profile.ui.screens.SwitchAccountPinSheet]. Either
     * path calls [CommitAccountSwitch] on success.
     */
    data class SwitchAccount(val id: String) : ProfileAction

    /** The gate ([SwitchAccount]'s biometric or PIN path) succeeded; performs the real switch. */
    data class CommitAccountSwitch(val id: String) : ProfileAction

    /** The PIN sheet or biometric prompt was dismissed/cancelled without switching. */
    data object CancelAccountSwitch : ProfileAction

    /**
     * Biometric guard is enabled but no usable biometric hardware/enrolment exists on-device —
     * falls back to the PIN sheet instead of silently committing the switch unconfirmed.
     */
    data class FallBackToPinGate(val id: String) : ProfileAction

    data object TogglePushNotifications : ProfileAction

    data object ToggleUsageAnalytics : ProfileAction

    /** P6.5: Preferences' Notification Center channel toggles (DataStore-backed, see [DemoSettingsRepository]). */
    data object TogglePushChannel : ProfileAction

    data object ToggleWhatsappChannel : ProfileAction

    data object ToggleSlackChannel : ProfileAction

    data class RaisePreferenceMessage(val message: String) : ProfileAction

    data object ClearPreferenceMessage : ProfileAction

    /** P1.3: adds a new switchable persona (never active on creation). */
    data class AddDemoAccount(val displayName: String, val employeeCode: String, val organization: String) : ProfileAction

    /** P1.3: removes a persona; a no-op + [RaisePreferenceMessage] when it's active or the last remaining one. */
    data class RemoveDemoAccount(val accountId: String) : ProfileAction

    /** P1.3: opens [AccountDetailsSheet][com.mileway.feature.profile.ui.screens.AccountDetailsSheet] for a persona. */
    data class ViewAccountDetails(val accountId: String) : ProfileAction

    /** P1.3: dismisses the details sheet opened by [ViewAccountDetails]. */
    data object DismissAccountDetails : ProfileAction

    /**
     * P2.4: signs out of [accountId]. If other personas remain, this just removes [accountId] and
     * switches to another one (mirrors [RemoveDemoAccount] for a non-active persona, but works even
     * when [accountId] is the currently-active one — that's the whole point of "sign out"). If
     * [accountId] is the last remaining persona, this clears the whole local session via
     * [SessionRepository.signOut] and emits [ProfileEffect.NavigateToLogin] instead.
     */
    data class SignOut(val accountId: String) : ProfileAction

    /**
     * P3.4: dismisses the "Trip in progress — pause and switch?" notice
     * [ProfileUiState.pausedTripNotice] surfaces after [CommitAccountSwitch] paused the outgoing
     * persona's running trip.
     */
    data object DismissPausedTripNotice : ProfileAction

    /**
     * P3.2: dismisses [ProfileUiState.showReconfirmIdentity]'s soft "Re-confirm it's you" sheet.
     * Just hides it for this composition — since Mileway has no real secret to protect, dismissal
     * isn't gated on a credential; the sheet won't reappear until the ViewModel's next freshness
     * check (see [ProfileViewModel]'s init) finds the session stale again.
     */
    data object DismissReconfirmIdentity : ProfileAction
}

sealed interface ProfileEffect {
    /**
     * P2.3: raised by [ProfileAction.SwitchAccount] when [DemoSettingsRepository.biometricGuardEnabled]
     * is on. `ProfileScreen` (Android-only, since `BiometricPrompt` needs a `FragmentActivity`) runs
     * `BiometricGuard.showPrompt` and dispatches [ProfileAction.CommitAccountSwitch] on success.
     */
    data class RequestBiometricGate(val accountId: String) : ProfileEffect

    /**
     * P2.4: the last persona was just signed out of (no personas remain). `LauncherActivity`
     * (Android-only; it owns [AppStage][com.mileway.AppStage]) reacts to this by transitioning
     * back to the login screen. Actually driven by [SessionRepository.sessionState] flipping to
     * signed-out — this effect is a one-shot nudge for screens that want to react immediately
     * rather than waiting on the DataStore round-trip.
     */
    data object NavigateToLogin : ProfileEffect
}

/**
 * Backs the whole account suite (Account hub, Profile Details, Preferences, Settings/Help).
 * Its own state ([ProfileUiState]) is mutated only through [onAction]; theme-related reactive
 * state and setters delegate to [ThemeController] (a dedicated controller, not this VM's state),
 * so they remain passthrough properties/methods.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val themeController: ThemeController,
    private val activeAccountSource: ActiveAccountSource,
    private val demoSettingsRepository: DemoSettingsRepository,
    private val sessionRepository: SessionRepository,
    // P3.4: pause/restore hook for a running trip when the active persona switches. Defaulted to
    // null so existing test call sites (5 files, all built before this task) don't need updating;
    // production Koin always supplies the real singleton.
    private val sessionCoordinator: MockAccountSessionCoordinator? = null,
    // P10.1: registry-backed notifications toggle (persisted per-account). Nullable-defaulted for
    // the same reason as sessionCoordinator above — direct-construction test sites omit it and get
    // the pre-P10.1 in-memory fallback; production Koin (viewModelOf) always resolves the real
    // singleton from the graph (defaults are ignored by viewModelOf when the type is bound).
    private val pluginRegistry: com.mileway.core.data.plugin.PluginRegistry? = null,
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

    init {
        // P1.2: `accounts()` above seeds the initial render synchronously (unchanged shape);
        // once the Room-backed store is seeded, switch the list to the live DAO-backed Flow so
        // it stays observably in sync with adds/removes (P1.3) instead of a one-shot snapshot.
        viewModelScope.launch {
            repository.seedAccountsIfEmpty()
            // P2.1: the persisted active-account pointer survives process death; applied once, on
            // the first Room emission, so it wins over the constructor's synchronous
            // `acc.firstOrNull()` seed instead of being masked by it (`stillSelected` would
            // otherwise always be true on that first emission and this would never run).
            val persistedActiveId = activeAccountSource.activeAccountId.first()
            var appliedPersistedId = false
            repository.observeAccounts().collect { accounts ->
                setState {
                    val nextSelected =
                        when {
                            !appliedPersistedId && persistedActiveId != null && accounts.any { it.id == persistedActiveId } ->
                                persistedActiveId
                            accounts.any { it.id == selectedAccountId } -> selectedAccountId
                            else -> accounts.firstOrNull()?.id.orEmpty()
                        }
                    copy(accounts = accounts, selectedAccountId = nextSelected)
                }
                appliedPersistedId = true
            }
        }
        // P6.5: Preferences' Notification Center channel toggles — live-synced from DataStore so
        // they persist across restart, same as every other DemoSettingsRepository-backed toggle.
        demoSettingsRepository.settings
            .map { NotificationChannels(it.pushChannelEnabled, it.whatsappChannelEnabled, it.slackChannelEnabled) }
            .onEach { channels -> setState { copy(notificationChannels = channels) } }
            .launchIn(viewModelScope)

        // P3.2: one-shot staleness check against the 25-minute soft window — checked once per
        // ViewModel lifetime (this screen is a top-level tab, recreated rarely) rather than on a
        // recurring timer, so the sheet surfaces once per staleness window instead of replaying on
        // every recomposition.
        viewModelScope.launch {
            val session = sessionRepository.sessionState.first()
            if (!isSessionFresh(Clock.System.now().toEpochMilliseconds(), session.signedInAtMillis)) {
                setState { copy(showReconfirmIdentity = true) }
            }
        }
    }

    override fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SwitchAccount -> requestAccountSwitch(action.id)
            is ProfileAction.CommitAccountSwitch -> commitAccountSwitch(action.id)
            ProfileAction.CancelAccountSwitch -> setState { copy(pendingSwitchAccountId = null) }
            is ProfileAction.FallBackToPinGate -> setState { copy(pendingSwitchAccountId = action.id) }
            ProfileAction.TogglePushNotifications ->
                setState { copy(preferences = preferences.copy(pushNotifications = !preferences.pushNotifications)) }
            ProfileAction.ToggleUsageAnalytics ->
                setState { copy(preferences = preferences.copy(usageAnalytics = !preferences.usageAnalytics)) }
            ProfileAction.TogglePushChannel -> viewModelScope.launch { demoSettingsRepository.togglePushChannel() }
            ProfileAction.ToggleWhatsappChannel -> viewModelScope.launch { demoSettingsRepository.toggleWhatsappChannel() }
            ProfileAction.ToggleSlackChannel -> viewModelScope.launch { demoSettingsRepository.toggleSlackChannel() }
            is ProfileAction.RaisePreferenceMessage -> setState { copy(preferenceMessage = action.message) }
            ProfileAction.ClearPreferenceMessage -> setState { copy(preferenceMessage = null) }
            is ProfileAction.AddDemoAccount -> addDemoAccount(action)
            is ProfileAction.RemoveDemoAccount -> removeDemoAccount(action.accountId)
            is ProfileAction.ViewAccountDetails -> viewAccountDetails(action.accountId)
            ProfileAction.DismissAccountDetails -> setState { copy(accountDetailsSheet = null) }
            is ProfileAction.SignOut -> signOut(action.accountId)
            ProfileAction.DismissPausedTripNotice -> setState { copy(pausedTripNotice = null) }
            ProfileAction.DismissReconfirmIdentity -> setState { copy(showReconfirmIdentity = false) }
        }
    }

    /**
     * P2.4: has-other-accounts vs no-other-accounts branching for signing out (no
     * FCM-unsubscribe/server-logout half — there is no backend to call). `SessionRepository
     * .signOut()` already existed but had zero call sites before this task.
     */
    private fun signOut(accountId: String) {
        val accounts = currentState.accounts
        val hasOtherAccounts = accounts.any { it.id != accountId }
        viewModelScope.launch {
            repository.removeAccount(accountId)
            if (hasOtherAccounts) {
                val next = accounts.first { it.id != accountId }.id
                activeAccountSource.setActiveAccountId(next)
                repository.setActiveAccount(next)
                setState { copy(selectedAccountId = next) }
            } else {
                sessionRepository.signOut()
                emitEffect(ProfileEffect.NavigateToLogin)
            }
        }
    }

    /**
     * P2.3: gates the switch behind biometric or PIN confirmation instead of committing
     * immediately. A no-op if [accountId] is already the active persona (avoids prompting for a
     * switch to the same account, e.g. a stray re-tap of the already-selected chip).
     */
    private fun requestAccountSwitch(accountId: String) {
        if (accountId == currentState.selectedAccountId) return
        viewModelScope.launch {
            if (demoSettingsRepository.settings.first().biometricGuardEnabled) {
                emitEffect(ProfileEffect.RequestBiometricGate(accountId))
            } else {
                setState { copy(pendingSwitchAccountId = accountId) }
            }
        }
    }

    /**
     * P2.2/P2.3: the real switch, not a cosmetic UI-state flag — called only after
     * [requestAccountSwitch]'s gate succeeds. Updates local state immediately (so the switcher row
     * reflects the tap without waiting on I/O), then runs [sessionCoordinator] (P3.4 — pauses a
     * running trip started by the outgoing persona and restores the incoming persona's own paused
     * trip, if any) **before** persisting the choice to [activeAccountSource] (survives process
     * death, P2.1) and [ProfileRepository.setActiveAccount] (flips the DAO's exclusive `isActive`
     * row, P1.1/P1.2) — both writes drive downstream re-queries: `ActiveAccountSource
     * .activeAccountId` is what `SavedTracksViewModel` collects to re-scope Journeys/Expenses to
     * the new persona.
     */
    private fun commitAccountSwitch(accountId: String) {
        setState { copy(selectedAccountId = accountId, pendingSwitchAccountId = null) }
        viewModelScope.launch {
            val outcome = sessionCoordinator?.onPersonaSwitch(accountId)?.getOrNull()
            if (outcome is MockAccountSessionCoordinator.Outcome.Paused) {
                setState { copy(pausedTripNotice = accountId) }
            }
            activeAccountSource.setActiveAccountId(accountId)
            repository.setActiveAccount(accountId)
        }
    }

    private fun addDemoAccount(action: ProfileAction.AddDemoAccount) {
        viewModelScope.launch {
            repository.addAccount(action.displayName, action.employeeCode, action.organization)
        }
    }

    /**
     * Guard mirrors the reference app's `AccountCard` disabled-delete-on-active rule plus the implicit
     * "can't remove your only account" invariant: removing the currently-active persona or the
     * last remaining one is a no-op with a snackbar instead of a crash-prone empty account list.
     */
    private fun removeDemoAccount(accountId: String) {
        val accounts = currentState.accounts
        val isActive = accountId == currentState.selectedAccountId
        val isLast = accounts.size <= 1
        when {
            isActive -> setState { copy(preferenceMessage = "Switch to another persona before removing this one") }
            isLast -> setState { copy(preferenceMessage = "You need at least one account") }
            else -> viewModelScope.launch { repository.removeAccount(accountId) }
        }
    }

    private fun viewAccountDetails(accountId: String) {
        val account = currentState.accounts.find { it.id == accountId } ?: return
        setState { copy(accountDetailsSheet = account) }
    }

    // ── Theme delegations (ThemeController owns this reactive state) ───────────

    /** `null` = follow system, `true` = force dark, `false` = force light. */
    val darkThemeOverride: StateFlow<Boolean?> = themeController.darkThemeOverride

    /** Currently selected curated theme (Design Language v2; default Matrix). */
    val milewayTheme: StateFlow<MilewayThemeVariant> = themeController.milewayTheme

    val accentPalette: StateFlow<AccentPalette> = themeController.accentPalette

    val customSeedHex: StateFlow<String> = themeController.customSeedHex

    val useSystemColors: StateFlow<Boolean> = themeController.useSystemColors

    val paletteStyle: StateFlow<String> = themeController.paletteStyle

    val language: StateFlow<AppLanguage> = themeController.language

    // PLAN_V24 P10.7: the 7 experimental-optimization toggles are registry-backed CAPABILITY plugins
    // now (persisted per-account, also shown on the Master Plugin experimental section). Null
    // pluginRegistry (test-only) falls back to in-memory flags = the pre-P10.7 behaviour.
    private val fallbackExperimental = MutableStateFlow(ExperimentalFlags())
    val experimentalFlags: StateFlow<ExperimentalFlags> =
        pluginRegistry?.let { reg ->
            combine(
                reg.observe("exp_battery_aware"),
                reg.observe("exp_low_end_tuning"),
                reg.observe("exp_aggressive_gps"),
                reg.observe("exp_capture_kalman"),
                reg.observe("exp_path_simplification"),
                reg.observe("exp_gap_telemetry"),
                reg.observe("exp_imu_logging"),
            ) { v ->
                ExperimentalFlags(
                    batteryAwareTracking = v[0],
                    lowEndDeviceTuning = v[1],
                    aggressiveGpsFilter = v[2],
                    captureKalman = v[3],
                    pathSimplification = v[4],
                    gapTelemetry = v[5],
                    imuLogging = v[6],
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, ExperimentalFlags())
        } ?: fallbackExperimental.asStateFlow()

    private val _useMiles = MutableStateFlow(true)
    val useMiles: StateFlow<Boolean> = _useMiles.asStateFlow()

    // P10.1: registry-backed so the toggle persists per-account across VM recreation instead of
    // being an in-memory MutableStateFlow. NOTED SKIP for behavior — the tracking FGS notification
    // is mandatory and no offline path reads this flag; persistence satisfies "not remember-only".
    // Null pluginRegistry (test-only) falls back to an in-memory flag = the pre-P10.1 behavior.
    private val fallbackNotifications = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> =
        pluginRegistry?.observe("notificationsEnabled")
            ?.stateIn(viewModelScope, SharingStarted.Eagerly, true)
            ?: fallbackNotifications.asStateFlow()

    fun toggleUnits() {
        _useMiles.value = !_useMiles.value
    }

    fun toggleNotifications() {
        val registry = pluginRegistry
        if (registry == null) {
            fallbackNotifications.value = !fallbackNotifications.value
            return
        }
        viewModelScope.launch {
            registry.setUserOverride(
                "notificationsEnabled",
                com.mileway.core.data.plugin.PluginValue.Bool(!notificationsEnabled.value),
            )
        }
    }

    fun setDarkTheme(dark: Boolean?) = themeController.set(dark)

    /** Select a curated Design Language v2 theme (Matrix / Amoled / Ion / Daybreak). */
    fun setMilewayTheme(theme: MilewayThemeVariant) = themeController.setMilewayTheme(theme)

    fun setPalette(palette: AccentPalette) = themeController.setPalette(palette)

    fun setCustomSeed(hex: String) = themeController.setCustomSeed(hex)

    fun setUseSystemColors(enabled: Boolean) = themeController.setUseSystemColors(enabled)

    fun setPaletteStyle(style: String) = themeController.setPaletteStyle(style)

    fun setLanguage(language: AppLanguage) = themeController.setLanguage(language)

    fun toggleBatteryAwareTracking() =
        toggleExperimental("exp_battery_aware", { it.batteryAwareTracking }) { copy(batteryAwareTracking = !batteryAwareTracking) }

    fun toggleLowEndDeviceTuning() = toggleExperimental("exp_low_end_tuning", { it.lowEndDeviceTuning }) { copy(lowEndDeviceTuning = !lowEndDeviceTuning) }

    fun toggleAggressiveGpsFilter() = toggleExperimental("exp_aggressive_gps", { it.aggressiveGpsFilter }) { copy(aggressiveGpsFilter = !aggressiveGpsFilter) }

    fun toggleCaptureKalman() = toggleExperimental("exp_capture_kalman", { it.captureKalman }) { copy(captureKalman = !captureKalman) }

    fun togglePathSimplification() = toggleExperimental("exp_path_simplification", { it.pathSimplification }) { copy(pathSimplification = !pathSimplification) }

    fun toggleGapTelemetry() = toggleExperimental("exp_gap_telemetry", { it.gapTelemetry }) { copy(gapTelemetry = !gapTelemetry) }

    fun toggleImuLogging() = toggleExperimental("exp_imu_logging", { it.imuLogging }) { copy(imuLogging = !imuLogging) }

    // Registry-backed toggle (persist a USER override); in-memory fallback when no registry (tests).
    private fun toggleExperimental(
        id: String,
        current: (ExperimentalFlags) -> Boolean,
        flip: ExperimentalFlags.() -> ExperimentalFlags,
    ) {
        val registry = pluginRegistry
        if (registry == null) {
            fallbackExperimental.value = fallbackExperimental.value.flip()
            return
        }
        val next = !current(experimentalFlags.value)
        viewModelScope.launch {
            registry.setUserOverride(id, com.mileway.core.data.plugin.PluginValue.Bool(next))
        }
    }

    fun resetCustomization() {
        themeController.resetCustomization()
        val registry = pluginRegistry
        if (registry == null) {
            fallbackExperimental.value = ExperimentalFlags()
            return
        }
        viewModelScope.launch {
            listOf(
                "exp_battery_aware",
                "exp_low_end_tuning",
                "exp_aggressive_gps",
                "exp_capture_kalman",
                "exp_path_simplification",
                "exp_gap_telemetry",
                "exp_imu_logging",
            ).forEach { registry.clearUserOverride(it) }
        }
    }
}
