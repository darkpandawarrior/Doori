package com.mileway.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

/** iOS mirror of the session-delegation overlay (see the androidMain doc). */
class DelegationSessionController : DelegationSessionSource {
    private val actingKey = booleanPreferencesKey("delegation_is_acting")
    private val nameKey = stringPreferencesKey("delegation_acting_name")
    private val emailKey = stringPreferencesKey("delegation_acting_email")
    private val codeKey = stringPreferencesKey("delegation_acting_code")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "delegation_session.preferences_pb").toPath() },
        )

    override val delegationState: Flow<DelegationState> =
        store.data.map { prefs ->
            DelegationState(
                isActing = prefs[actingKey] ?: false,
                actingName = prefs[nameKey],
                actingEmail = prefs[emailKey],
                actingCode = prefs[codeKey],
            )
        }

    override val isActingAsDelegate: Flow<Boolean> =
        store.data.map { prefs -> prefs[actingKey] ?: false }

    override suspend fun startDelegation(
        name: String,
        email: String,
        code: String,
    ): Boolean {
        if (delegationState.first().isActing) return false
        store.edit { prefs ->
            prefs[actingKey] = true
            prefs[nameKey] = name
            prefs[emailKey] = email
            prefs[codeKey] = code
        }
        return true
    }

    override suspend fun endDelegation() {
        store.edit { it.clear() }
    }
}
