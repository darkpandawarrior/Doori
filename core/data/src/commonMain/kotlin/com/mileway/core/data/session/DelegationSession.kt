package com.mileway.core.data.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P7.3: the "acting on behalf of" layer that sits on TOP of the base signed-in session
 * without replacing it — the session-delegation concept (a manager temporarily operating as one of
 * their reportees), NOT the approval-delegation concept modeled by
 * [Delegation][com.mileway.feature.profile.model.Delegation] (which delegates approval authority to
 * a teammate). The two are deliberately kept separate per PLAN_V22 §2's architecture note.
 *
 * The base identity is never touched while acting (it stays in [SessionRepository]); only this
 * overlay flips. [effectiveSignedInIdentity] resolves the two into the identity a new trip is
 * stamped with, so session isolation ([doesSessionBelongTo]) keeps the delegate's trips separate
 * from the manager's own.
 */
data class DelegationState(
    val isActing: Boolean = false,
    val actingName: String? = null,
    val actingEmail: String? = null,
    val actingCode: String? = null,
)

/**
 * Read/write contract over the session-delegation overlay, mirroring [ActiveAccountSource]'s
 * interface-plus-platform-impl split so `commonMain` code (the tracking stamp path, the profile
 * "Act on behalf" screen) can depend on it while the DataStore-backed
 * [DelegationSessionController] stays androidMain/iosMain-only.
 */
interface DelegationSessionSource {
    val delegationState: Flow<DelegationState>

    /** Convenience projection — the flag PLAN_V22 left declared-but-unimplemented. */
    val isActingAsDelegate: Flow<Boolean>

    /**
     * Begin acting as the named delegate. Blocks nested delegation: returns `false` (a no-op) if a
     * delegation is already active, matching the source's `AccountSwitchManager` guard.
     */
    suspend fun startDelegation(
        name: String,
        email: String,
        code: String,
    ): Boolean

    /** Restore the base identity (end acting). Idempotent when not acting. */
    suspend fun endDelegation()
}

/** A default source that is never acting — the tracking VM/Koin-graph fallback (see [NoSessionSource]). */
object NoDelegationSessionSource : DelegationSessionSource {
    override val delegationState: Flow<DelegationState> = MutableStateFlow(DelegationState()).asStateFlow()
    override val isActingAsDelegate: Flow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun startDelegation(
        name: String,
        email: String,
        code: String,
    ): Boolean = false

    override suspend fun endDelegation() = Unit
}

/**
 * In-memory, non-persistent implementation holding the full start/end/nested-block logic. Used by
 * unit tests and the deterministic Koin test graphs (the real DataStore round-trip is exercised by
 * the platform [DelegationSessionController], mirroring how [ActiveAccountStore] is faked in
 * `ActiveAccountStoreTest`).
 */
class InMemoryDelegationSessionSource(
    seed: DelegationState = DelegationState(),
) : DelegationSessionSource {
    private val state = MutableStateFlow(seed)
    override val delegationState: Flow<DelegationState> = state.asStateFlow()
    override val isActingAsDelegate: Flow<Boolean> = state.map { it.isActing }

    override suspend fun startDelegation(
        name: String,
        email: String,
        code: String,
    ): Boolean {
        if (state.value.isActing) return false
        state.value = DelegationState(isActing = true, actingName = name, actingEmail = email, actingCode = code)
        return true
    }

    override suspend fun endDelegation() {
        state.value = DelegationState()
    }
}
