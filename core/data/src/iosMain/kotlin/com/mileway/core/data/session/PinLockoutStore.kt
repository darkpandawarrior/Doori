package com.mileway.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

/** iOS mirror of the per-account tiered PIN lockout store (see the androidMain doc). */
class PinLockoutStore : PinLockoutSource {
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "pin_lockout.preferences_pb").toPath() },
        )

    override suspend fun getState(accountId: String): PinLockoutState {
        val prefs = store.data.firstOrNull() ?: return PinLockoutState()
        return PinLockoutState(
            failedAttempts = prefs[attemptsKey(accountId)] ?: 0,
            lockoutUntilMillis = prefs[untilKey(accountId)] ?: 0L,
        )
    }

    override suspend fun setState(
        accountId: String,
        state: PinLockoutState,
    ) {
        store.edit { prefs ->
            prefs[attemptsKey(accountId)] = state.failedAttempts
            prefs[untilKey(accountId)] = state.lockoutUntilMillis
        }
    }

    override suspend fun clear(accountId: String) {
        store.edit { prefs ->
            prefs.remove(attemptsKey(accountId))
            prefs.remove(untilKey(accountId))
        }
    }

    private fun attemptsKey(accountId: String) = intPreferencesKey("pin_lockout_attempts_$accountId")

    private fun untilKey(accountId: String) = longPreferencesKey("pin_lockout_until_$accountId")
}
