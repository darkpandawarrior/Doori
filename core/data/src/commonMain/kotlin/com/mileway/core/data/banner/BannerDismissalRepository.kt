package com.mileway.core.data.banner

import com.mileway.core.data.dao.BannerDismissalDao
import com.mileway.core.data.model.db.BannerDismissedEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P13.1: reads/writes the persisted per-account banner-dismissal set. Keyed off the active
 * account so each persona keeps its own dismissals; a null active account resolves to the empty set
 * (nothing dismissed).
 */
class BannerDismissalRepository(
    private val dao: BannerDismissalDao,
    private val activeAccount: ActiveAccountSource,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeDismissed(): Flow<Set<String>> =
        activeAccount.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(emptySet())
            } else {
                dao.observeDismissedIds(accountId).map { it.toSet() }
            }
        }

    suspend fun dismiss(bannerId: String) {
        val accountId = activeAccount.activeAccountId.first() ?: return
        dao.upsert(BannerDismissedEntity(accountId = accountId, bannerId = bannerId, dismissedAtMs = nowMs()))
    }
}
