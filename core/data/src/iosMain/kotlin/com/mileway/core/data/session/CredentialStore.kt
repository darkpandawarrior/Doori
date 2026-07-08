package com.mileway.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

/** iOS mirror of the per-account login credential store (see the androidMain doc). */
class CredentialStore : CredentialSource {
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "login_credential.preferences_pb").toPath() },
        )

    override suspend fun ensureSeeded(accountId: String) {
        val existing = store.data.map { prefs -> prefs[keyFor(accountId)] }.firstOrNull()
        if (existing == null) setPassword(accountId, DEFAULT_PASSWORD)
    }

    override suspend fun verify(
        accountId: String,
        password: String,
    ): Boolean {
        ensureSeeded(accountId)
        val stored = store.data.map { prefs -> prefs[keyFor(accountId)] }.firstOrNull()
        return stored != null && stored == hashPassword(accountId, password)
    }

    override suspend fun setPassword(
        accountId: String,
        password: String,
    ) {
        store.edit { prefs -> prefs[keyFor(accountId)] = hashPassword(accountId, password) }
    }

    private fun keyFor(accountId: String) = stringPreferencesKey("credential_$accountId")
}
