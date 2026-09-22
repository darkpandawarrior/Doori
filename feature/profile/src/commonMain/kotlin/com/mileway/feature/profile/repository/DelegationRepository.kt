package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.model.db.DelegationEntity
import com.mileway.feature.profile.model.Delegation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Generated ids carry the last eight digits of the creation timestamp - unique enough offline. */
private const val ID_SUFFIX_DIGITS = 8

/**
 * PLAN_V22 P6.3: Room-backed store for `DelegationScreen`'s "My Delegations" list — the
 * approval-delegation concept ("delegate your approval authority to a teammate"), distinct from
 * the account-switch/session-delegate concept (see PLAN_V22 §2's Architecture note; not merged
 * here). Replaces the screen's `mutableStateListOf` seed, which reset on navigation away.
 */
class DelegationRepository(
    private val dao: DelegationDao,
    private val clock: Clock = Clock.System,
) {
    /** Live, creation-ordered list of this user's outgoing delegations. */
    fun observeAll(): Flow<List<Delegation>> = dao.observeAll().map { rows -> rows.map { it.toDelegation() } }

    /**
     * Adds a new delegation. [delegateName] and [scope] must both be non-blank — callers should
     * validate before calling this (the ViewModel surfaces the error instead of silently no-oping
     * here), matching the reference app's blank-reason guard.
     */
    suspend fun add(
        delegateName: String,
        scope: String,
        expiresAtMillis: Long,
    ) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            DelegationEntity(
                id = "DEL-" + now.toString().takeLast(ID_SUFFIX_DIGITS),
                delegateName = delegateName,
                scope = scope,
                expiresAtMillis = expiresAtMillis,
                isActive = true,
                createdAt = now,
            ),
        )
    }

    /** Revokes (deletes) [id] outright — irreversible, matching the screen's confirmation-sheet copy. */
    suspend fun revoke(id: String) = dao.delete(id)

    /** Pauses/resumes [id] without revoking it. */
    suspend fun setActive(
        id: String,
        isActive: Boolean,
    ) = dao.setActive(id, isActive)

    private fun DelegationEntity.toDelegation(): Delegation =
        Delegation(
            id = id,
            delegateName = delegateName,
            scope = scope,
            expiresAtMillis = expiresAtMillis,
            isActive = isActive,
        )
}
