package com.mileway.core.platform

import com.siddharth.kmp.appshell.AppPermission
import com.siddharth.kmp.appshell.PermissionResult
import com.siddharth.kmp.appshell.PermissionsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiered permission-onboarding model (Wave 3 parity item). Unlike [PermissionOrchestrator] (a flat
 * sequence over any [AppPermission] list), this models the *first-run tracking onboarding* specifically:
 * an ordered set of tiers, each carrying rationale + skip-impact copy so the UI can explain what degrades
 * if the user skips an optional tier, plus a resume re-check so returning from system settings recomputes
 * the whole ladder rather than trusting stale in-memory state.
 */
enum class PermissionTierId { LOCATION_FINE, BACKGROUND_LOCATION, NOTIFICATIONS, ACTIVITY_RECOGNITION }

/** One rung of the onboarding ladder. */
data class PermissionTier(
    val id: PermissionTierId,
    val permission: AppPermission,
    val required: Boolean,
    val rationale: String,
    /** What degrades if the user skips this tier. Always present, even for required tiers (pre-denial copy). */
    val skipImpact: String,
)

/** The default tiered ladder shown during tracking-permission onboarding, in prompt order. */
val defaultPermissionTiers =
    listOf(
        PermissionTier(
            id = PermissionTierId.LOCATION_FINE,
            permission = AppPermission.LOCATION,
            required = true,
            rationale = "Mileway needs your location to record trip distance and route.",
            skipImpact = "Without this, trips can't be tracked at all.",
        ),
        PermissionTier(
            id = PermissionTierId.BACKGROUND_LOCATION,
            permission = AppPermission.LOCATION_BACKGROUND,
            required = false,
            rationale = "Allow location access all the time so trips keep tracking when the app is in the background.",
            skipImpact = "Trips will pause if you switch apps or lock your phone mid-journey.",
        ),
        PermissionTier(
            id = PermissionTierId.NOTIFICATIONS,
            permission = AppPermission.NOTIFICATIONS,
            required = false,
            rationale = "Notifications show live trip status and let you stop tracking from the lock screen.",
            skipImpact = "You won't see the ongoing-trip notification or trip-end reminders.",
        ),
        PermissionTier(
            id = PermissionTierId.ACTIVITY_RECOGNITION,
            permission = AppPermission.ACTIVITY_RECOGNITION,
            required = false,
            rationale = "Activity recognition lets Mileway auto-detect driving vs walking.",
            skipImpact = "You'll need to pick your vehicle/mode manually for every trip.",
        ),
    )

/** Outcome recorded for a tier once the user (or the system) has decided. */
sealed interface TierOutcome {
    data object Granted : TierOutcome

    data object Skipped : TierOutcome

    data object Denied : TierOutcome
}

/** Snapshot of the onboarding ladder: each tier paired with its outcome so far (null = not yet decided). */
data class PermissionOnboardingState(
    val tiers: List<PermissionTier>,
    val outcomes: Map<PermissionTierId, TierOutcome> = emptyMap(),
    val currentIndex: Int = 0,
) {
    val current: PermissionTier? get() = tiers.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= tiers.size
    val requiredSatisfied: Boolean
        get() = tiers.filter { it.required }.all { outcomes[it.id] == TierOutcome.Granted }
}

/**
 * Drives [defaultPermissionTiers] (or any tier list) one at a time over a [PermissionsProvider]: skips
 * tiers already granted, records grant/skip/deny for the rest, and exposes [recheck] to recompute every
 * tier's outcome from live provider state — the "resume re-check" that runs when the app comes back from
 * system settings, since a grant made there never flows through [advance]/[skip] directly.
 */
class PermissionOnboardingFlow(
    private val provider: PermissionsProvider,
    tiers: List<PermissionTier> = defaultPermissionTiers,
) {
    private val _state = MutableStateFlow(PermissionOnboardingState(tiers))
    val state: StateFlow<PermissionOnboardingState> = _state.asStateFlow()

    /** Skip past any leading tiers already granted, without prompting. */
    suspend fun skipAlreadyGranted() {
        while (true) {
            val tier = _state.value.current ?: return
            if (provider.isGranted(tier.permission)) {
                recordAndAdvance(tier.id, TierOutcome.Granted)
            } else {
                return
            }
        }
    }

    /** Request the current tier's permission; records Granted/Denied and advances. No-op once complete. */
    suspend fun requestCurrent(): TierOutcome? {
        val tier = _state.value.current ?: return null
        val outcome = if (provider.request(tier.permission) == PermissionResult.Granted) TierOutcome.Granted else TierOutcome.Denied
        recordAndAdvance(tier.id, outcome)
        return outcome
    }

    /** User explicitly skipped the current (optional) tier. No-op on a required tier. */
    fun skipCurrent() {
        val tier = _state.value.current ?: return
        if (tier.required) return
        recordAndAdvance(tier.id, TierOutcome.Skipped)
    }

    /**
     * Resume re-check (UX requirement): re-evaluate every tier's live grant status against the provider,
     * e.g. after returning from the system settings screen. A tier previously Skipped or Denied that is now
     * actually granted is upgraded to Granted; nothing else changes. Does not move [PermissionOnboardingState.currentIndex]
     * past tiers still ungranted, so the ladder resumes where it left off.
     */
    suspend fun recheck() {
        val s = _state.value
        val updated =
            s.outcomes.toMutableMap().also { map ->
                s.tiers.forEach { tier ->
                    if (provider.isGranted(tier.permission)) map[tier.id] = TierOutcome.Granted
                }
            }
        val firstUndecidedIndex = s.tiers.indexOfFirst { updated[it.id] == null }
        val newIndex = if (firstUndecidedIndex == -1) s.tiers.size else firstUndecidedIndex
        _state.value = s.copy(outcomes = updated, currentIndex = newIndex)
    }

    private fun recordAndAdvance(
        id: PermissionTierId,
        outcome: TierOutcome,
    ) {
        val s = _state.value
        _state.value = s.copy(outcomes = s.outcomes + (id to outcome), currentIndex = s.currentIndex + 1)
    }
}
