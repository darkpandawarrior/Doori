package com.mileway.feature.tracking.ui.onboarding

import com.mileway.core.platform.PermissionOnboardingFlow
import com.mileway.core.platform.PermissionOnboardingState
import com.mileway.core.platform.PermissionTier
import com.mileway.core.platform.PermissionTierId
import com.mileway.core.platform.TierOutcome
import com.mileway.core.platform.defaultPermissionTiers
import com.siddharth.kmp.appshell.PermissionsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One screen of the permission primer: an upfront [Intro] (explain WHY before any system dialog
 * fires — asking before explaining converts far worse than explaining first, and this is the flow that
 * decides whether the app can record anything at all), then one [Requesting] screen per
 * [PermissionOnboardingFlow] tier (rendered by the existing `PermissionOnboardingSheet` — reused,
 * not duplicated), then a single honest [Done] terminal screen once the ladder is exhausted.
 */
sealed interface PrimerStage {
    data object Intro : PrimerStage

    data class Requesting(val tier: PermissionTier) : PrimerStage

    data class Done(val outcome: PrimerOutcome) : PrimerStage
}

/**
 * The honest terminal state of the primer. Every branch renders distinct, truthful copy (task
 * requirement) — collapsing "denied" and "permanently denied" and "blocked by policy" into one
 * generic error is exactly the vague ask that gets background location denied for good.
 */
sealed interface PrimerOutcome {
    /** Required (foreground) + background both granted — tracking works everywhere, including backgrounded. */
    data object FullyGranted : PrimerOutcome

    /** Required (foreground) granted, background skipped/denied — tracking pauses when the app isn't open. */
    data object ForegroundOnly : PrimerOutcome

    /** Required (foreground) location denied once — re-askable, the system dialog can still appear. */
    data object Denied : PrimerOutcome

    /**
     * Required (foreground) location denied twice in a row. On both Android ("don't ask again") and
     * iOS a second denial stops the system dialog from ever reappearing — re-requesting in-app is a
     * silent no-op from here on, so the UI MUST deep-link to system Settings instead of looping the ask.
     */
    data object PermanentlyDenied : PrimerOutcome

    /** An enterprise/device-admin policy blocks the permission outright — no in-app or Settings fix exists. */
    data object RestrictedByPolicy : PrimerOutcome
}

/**
 * Drives [PermissionPrimerSheet]'s screen sequence over a fresh [PermissionOnboardingFlow] per attempt.
 * Does NOT re-implement permission requesting — that stays [PermissionOnboardingFlow]'s job, the single
 * [PermissionsProvider]-backed abstraction the app already uses. This class only adds: the primer intro
 * screen, a first-denial-vs-permanently-denied classification the underlying flow doesn't make (see
 * [denialCount] below), and the terminal [PrimerOutcome] the UI renders.
 *
 * @param isPolicyRestricted reports whether a device-admin/MDM policy is blocking permissions. No such
 *   signal exists on [PermissionsProvider] today (would need a `DevicePolicyManager` check wired into
 *   the platform-specific provider implementation, which lives in the shared `external/kmp-toolkit`
 *   family module — out of this task's ownership). Defaults to "never restricted"; a caller on a
 *   policy-managed fleet can plug a real check in without changing this class's shape.
 */
class PermissionPrimerController(
    private val provider: PermissionsProvider,
    private val tiers: List<PermissionTier> = defaultPermissionTiers,
    private val isPolicyRestricted: () -> Boolean = { false },
) {
    private var flow = PermissionOnboardingFlow(provider, tiers)

    private val _stage = MutableStateFlow<PrimerStage>(PrimerStage.Intro)
    val stage: StateFlow<PrimerStage> = _stage.asStateFlow()

    // ponytail: PermissionOnboardingFlow/TierOutcome (core/platform) collapses the platform's
    // PermissionResult.DeniedAlways into plain TierOutcome.Denied, so the real "don't ask again" signal
    // never reaches this class — and AndroidPermissionsProvider (external/kmp-toolkit) never returns
    // DeniedAlways in the first place. Consecutive-denial counting is the best proxy without editing
    // either file (both out of this task's ownership). Upgrade path: thread PermissionResult through
    // TierOutcome in core/platform/PermissionOnboarding.kt and drop this map once that lands.
    private val denialCount = mutableMapOf<PermissionTierId, Int>()

    /** Call once, when the sheet first appears. Skips already-granted leading tiers, then shows the intro. */
    suspend fun start() {
        flow.skipAlreadyGranted()
        _stage.value = if (flow.state.value.isComplete) PrimerStage.Done(classify(flow.state.value)) else PrimerStage.Intro
    }

    /** Intro's "Continue" action — moves from the explainer into the first (required) tier's request. */
    fun beginRequesting() = advance()

    /**
     * Request the tier currently shown. A denial on the *required* tier short-circuits straight to
     * [PrimerStage.Done] instead of walking the rest of the ladder — asking about background location
     * next makes no sense once foreground was just refused, and it's what lets a second consecutive
     * denial reliably resolve to [PrimerOutcome.PermanentlyDenied] rather than being buried behind
     * three more prompts the user never gets to answer. Denials on optional tiers just advance normally
     * ([PermissionOnboardingFlow] already treats those as skippable).
     */
    suspend fun requestCurrent() {
        val tier = flow.state.value.current ?: return
        val outcome = flow.requestCurrent()
        if (tier.required && outcome != TierOutcome.Granted) {
            denialCount[tier.id] = (denialCount[tier.id] ?: 0) + 1
            _stage.value = PrimerStage.Done(deniedOutcome(tier.id))
            return
        }
        advance()
    }

    /** Skip the current (optional) tier. No-op on the required tier — mirrors [PermissionOnboardingFlow.skipCurrent]. */
    fun skipCurrent() {
        flow.skipCurrent()
        advance()
    }

    /**
     * Re-check every tier against live provider state — call after the user returns from system Settings.
     * [PermissionOnboardingFlow.recheck] resumes at the first tier it never got to decide, which after a
     * [requestCurrent] short-circuit means it would jump straight past the still-required tier to
     * background/notifications/activity — so this checks [PermissionOnboardingState.requiredSatisfied]
     * itself first: if the user backed out of Settings without actually granting it, stay on the same
     * honest [PrimerOutcome] rather than silently moving on to asking about background location.
     */
    suspend fun recheckAfterSettings() {
        flow.recheck()
        val s = flow.state.value
        _stage.value =
            if (s.requiredSatisfied) {
                if (!s.isComplete) PrimerStage.Requesting(s.current!!) else PrimerStage.Done(classify(s))
            } else {
                PrimerStage.Done(deniedOutcome(s.tiers.first { it.required }.id))
            }
    }

    /**
     * Explicit retry from the [PrimerOutcome.Denied] terminal screen. [PermissionOnboardingFlow] always
     * advances past a tier once decided (even on denial), so there is no way to re-drive the same tier on
     * the existing flow instance — this starts a fresh one instead. [denialCount] is NOT reset here, so a
     * second denial after retrying still classifies as [PrimerOutcome.PermanentlyDenied].
     */
    suspend fun retry() {
        flow = PermissionOnboardingFlow(provider, tiers)
        flow.skipAlreadyGranted()
        advance()
    }

    private fun advance() {
        val s = flow.state.value
        _stage.value = if (!s.isComplete) PrimerStage.Requesting(s.current!!) else PrimerStage.Done(classify(s))
    }

    private fun deniedOutcome(requiredTierId: PermissionTierId): PrimerOutcome =
        if (isPolicyRestricted()) {
            PrimerOutcome.RestrictedByPolicy
        } else if ((denialCount[requiredTierId] ?: 0) >= 2) {
            PrimerOutcome.PermanentlyDenied
        } else {
            PrimerOutcome.Denied
        }

    /**
     * Only reached with [PermissionOnboardingState.requiredSatisfied] already true: [requestCurrent] and
     * [recheckAfterSettings] both short-circuit to [PrimerStage.Done] directly on a still-unsatisfied
     * required tier, and [start]'s `skipAlreadyGranted` only reaches [PermissionOnboardingState.isComplete]
     * when every tier — required included — was already granted coming in. So this only ever has to
     * distinguish "background granted too" from "foreground only".
     */
    private fun classify(s: PermissionOnboardingState): PrimerOutcome {
        if (isPolicyRestricted()) return PrimerOutcome.RestrictedByPolicy
        val backgroundGranted = s.outcomes[PermissionTierId.BACKGROUND_LOCATION] == TierOutcome.Granted
        return if (backgroundGranted) PrimerOutcome.FullyGranted else PrimerOutcome.ForegroundOnly
    }
}
