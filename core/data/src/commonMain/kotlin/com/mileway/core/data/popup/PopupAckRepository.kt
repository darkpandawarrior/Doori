package com.mileway.core.data.popup

import com.mileway.core.data.dao.PopupAckDao
import com.mileway.core.data.model.db.PopupAckEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P13.3: reads/writes the persisted per-account forced-popup acknowledgement set, keyed off
 * the active account so each persona keeps its own state. A null active account resolves to the
 * empty set (nothing acknowledged yet).
 */
class PopupAckRepository(
    private val dao: PopupAckDao,
    private val activeAccount: ActiveAccountSource,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeAcknowledged(): Flow<Set<String>> =
        activeAccount.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(emptySet())
            } else {
                dao.observeAcknowledgedIds(accountId).map { it.toSet() }
            }
        }

    suspend fun acknowledge(popupId: String) {
        val accountId = activeAccount.activeAccountId.first() ?: return
        dao.upsert(PopupAckEntity(accountId = accountId, popupId = popupId, acknowledgedAtMs = nowMs()))
    }
}
