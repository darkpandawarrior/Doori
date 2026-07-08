package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.model.db.SessionEntity
import com.mileway.core.network.model.UserSession
import com.mileway.feature.profile.model.ActiveSession
import com.mileway.stub.ProfileMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * PLAN_V22 P6.4: Room-backed store for `ActiveSessionsScreen`'s device-session list — replaces
 * `stub.ProfileMockData.sessions()`'s bare in-memory list (previously read straight through
 * `ProfileRepository.sessions()` into the read-only `SessionsDialog`) so a per-session revoke or
 * the "Sign out all other sessions" bulk action actually persists across app kill/relaunch.
 */
class ActiveSessionsRepository(private val dao: SessionDao) {
    /** Live, most-recently-active-first list of this account's device sessions. */
    fun observeAll(): Flow<List<ActiveSession>> = dao.observeAll().map { rows -> rows.map { it.toActiveSession() } }

    /** Seeds the original demo sessions if the table is empty (first run only). */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        dao.upsertAll(ProfileMockData.sessions().mapIndexed { index, session -> session.toEntity(index) })
    }

    /** Revokes (deletes) [id] outright. Callers must never pass the current-device row's id. */
    suspend fun revoke(id: String) = dao.delete(id)

    /** "Sign out all other sessions" — removes every row except the current device's. */
    suspend fun revokeAllExceptCurrent() = dao.deleteAllExceptCurrent()

    private fun SessionEntity.toActiveSession(): ActiveSession =
        ActiveSession(
            id = id,
            deviceName = deviceName,
            platform = platform,
            lastActiveMillis = lastActiveMillis,
            isCurrent = isCurrent,
            os = os,
            appVersion = appVersion,
            ip = ip,
        )

    private fun UserSession.toEntity(index: Int): SessionEntity =
        SessionEntity(
            id = "SESSION-$index",
            deviceName = deviceName,
            platform = platform,
            lastActiveMillis = lastActiveMillis,
            isCurrent = isCurrent,
            os = os,
            appVersion = appVersion,
            ip = ip,
        )
}
