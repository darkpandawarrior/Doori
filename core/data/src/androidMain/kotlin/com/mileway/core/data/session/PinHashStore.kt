package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.pinHashDataStore by preferencesDataStore(name = "pin_hash")

/**
 * DataStore-backed [PinHashSource] keyed by account id, so each demo persona (P1.1's
 * `MockAccountEntity`) can hold its own independent PIN — mirrors [ActiveAccountStore]'s exact
 * shape (per-key `stringPreferencesKey`, suspend read/write) rather than inventing a new DataStore
 * idiom for this task.
 */
class PinHashStore(
    private val context: Context,
) : PinHashSource {
    override suspend fun getPinHash(accountId: String): String? =
        context.pinHashDataStore.data
            .map { prefs -> prefs[keyFor(accountId)] }
            .firstOrNull()

    override suspend fun setPinHash(
        accountId: String,
        pinHash: String,
    ) {
        context.pinHashDataStore.edit { prefs -> prefs[keyFor(accountId)] = pinHash }
    }

    private fun keyFor(accountId: String) = stringPreferencesKey("pin_hash_$accountId")
}
