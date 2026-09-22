package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.model.db.ConnectedAccountEntity
import com.mileway.feature.profile.model.ConnectedAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V22 P6.6: Room-backed store for Preferences' "Connected Accounts" tile — replaces its
 * previous `ProfileAction.RaisePreferenceMessage("Connected Accounts is a demo placeholder.")`
 * one-shot snackbar tap with a real, persisted list of mock cab/passport-style integrations,
 * mirroring [com.mileway.feature.profile.repository.NotificationRepository]'s
 * seed-once-then-observe shape.
 */
class ConnectedAccountsRepository(
    private val dao: ConnectedAccountDao,
    private val clock: Clock = Clock.System,
) {
    /** Live, provider-name-ordered list of connected-account rows. */
    fun observeAll(): Flow<List<ConnectedAccount>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    /** Seeds the demo integrations on first run only; a no-op on every subsequent launch. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(
            listOf(
                ConnectedAccountEntity(
                    id = "cab_uber",
                    providerName = "Uber for Business",
                    category = "Cabs",
                    isConnected = true,
                    updatedAtMs = now,
                ),
                ConnectedAccountEntity(
                    id = "cab_ola",
                    providerName = "Ola Corporate",
                    category = "Cabs",
                    isConnected = false,
                    updatedAtMs = now,
                ),
                ConnectedAccountEntity(
                    id = "passport_sync",
                    providerName = "Passport Auto-fill",
                    category = "Passport",
                    isConnected = true,
                    updatedAtMs = now,
                ),
            ),
        )
    }

    /** Toggles [id]'s connection state — a local flag flip only, never a real network call. */
    suspend fun setConnected(
        id: String,
        isConnected: Boolean,
    ) = dao.setConnected(id, isConnected, clock.now().toEpochMilliseconds())

    private fun ConnectedAccountEntity.toModel(): ConnectedAccount =
        ConnectedAccount(
            id = id,
            providerName = providerName,
            category = category,
            isConnected = isConnected,
        )
}
