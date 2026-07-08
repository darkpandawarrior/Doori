package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.credentialDataStore by preferencesDataStore(name = "login_credential")

/**
 * PLAN_V24 P1.5: DataStore-backed [CredentialSource] keyed by account id. Stores only the
 * salted-hash digest ([hashPassword]); the raw password is never persisted.
 */
class CredentialStore(private val context: Context) : CredentialSource {
    override suspend fun ensureSeeded(accountId: String) {
        val existing =
            context.credentialDataStore.data
                .map { prefs -> prefs[keyFor(accountId)] }
                .firstOrNull()
        if (existing == null) setPassword(accountId, DEFAULT_PASSWORD)
    }

    override suspend fun verify(
        accountId: String,
        password: String,
    ): Boolean {
        ensureSeeded(accountId)
        val stored =
            context.credentialDataStore.data
                .map { prefs -> prefs[keyFor(accountId)] }
                .firstOrNull()
        return stored != null && stored == hashPassword(accountId, password)
    }

    override suspend fun setPassword(
        accountId: String,
        password: String,
    ) {
        context.credentialDataStore.edit { prefs -> prefs[keyFor(accountId)] = hashPassword(accountId, password) }
    }

    private fun keyFor(accountId: String) = stringPreferencesKey("credential_$accountId")
}
