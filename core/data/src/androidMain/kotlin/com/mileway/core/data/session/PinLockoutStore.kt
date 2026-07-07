package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull

private val Context.pinLockoutDataStore by preferencesDataStore(name = "pin_lockout")

/**
 * PLAN_V24 P1.4: DataStore-backed [PinLockoutSource] keyed by account id, so a tiered PIN lockout
 * survives process death. Two prefs per account: cumulative failed attempts + the lockout-until
 * epoch millis.
 */
class PinLockoutStore(private val context: Context) : PinLockoutSource {
    override suspend fun getState(accountId: String): PinLockoutState {
        val prefs = context.pinLockoutDataStore.data.firstOrNull() ?: return PinLockoutState()
        return PinLockoutState(
            failedAttempts = prefs[attemptsKey(accountId)] ?: 0,
            lockoutUntilMillis = prefs[untilKey(accountId)] ?: 0L,
        )
    }

    override suspend fun setState(
        accountId: String,
        state: PinLockoutState,
    ) {
        context.pinLockoutDataStore.edit { prefs ->
            prefs[attemptsKey(accountId)] = state.failedAttempts
            prefs[untilKey(accountId)] = state.lockoutUntilMillis
        }
    }

    override suspend fun clear(accountId: String) {
        context.pinLockoutDataStore.edit { prefs ->
            prefs.remove(attemptsKey(accountId))
            prefs.remove(untilKey(accountId))
        }
    }

    private fun attemptsKey(accountId: String) = intPreferencesKey("pin_lockout_attempts_$accountId")

    private fun untilKey(accountId: String) = longPreferencesKey("pin_lockout_until_$accountId")
}
