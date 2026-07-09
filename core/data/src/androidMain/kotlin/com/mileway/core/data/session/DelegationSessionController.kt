package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.delegationSessionDataStore by preferencesDataStore(name = "delegation_session")

/**
 * PLAN_V24 P7.3: DataStore-backed session-delegation overlay, persisted so "Acting as <name>"
 * survives process death (the base identity in [SessionRepository] is never touched while acting).
 * Mirrors [ActiveAccountStore]'s androidMain/iosMain platform-impl split; the shared contract and
 * the nested-block/state logic are documented on [DelegationSessionSource] /
 * [InMemoryDelegationSessionSource].
 */
class DelegationSessionController(private val context: Context) : DelegationSessionSource {
    private val actingKey = booleanPreferencesKey("delegation_is_acting")
    private val nameKey = stringPreferencesKey("delegation_acting_name")
    private val emailKey = stringPreferencesKey("delegation_acting_email")
    private val codeKey = stringPreferencesKey("delegation_acting_code")

    override val delegationState: Flow<DelegationState> =
        context.delegationSessionDataStore.data.map { prefs ->
            DelegationState(
                isActing = prefs[actingKey] ?: false,
                actingName = prefs[nameKey],
                actingEmail = prefs[emailKey],
                actingCode = prefs[codeKey],
            )
        }

    override val isActingAsDelegate: Flow<Boolean> =
        context.delegationSessionDataStore.data.map { prefs -> prefs[actingKey] ?: false }

    override suspend fun startDelegation(
        name: String,
        email: String,
        code: String,
    ): Boolean {
        // Block nested delegation: already acting → no-op (source's AccountSwitchManager guard).
        if (delegationState.first().isActing) return false
        context.delegationSessionDataStore.edit { prefs ->
            prefs[actingKey] = true
            prefs[nameKey] = name
            prefs[emailKey] = email
            prefs[codeKey] = code
        }
        return true
    }

    override suspend fun endDelegation() {
        context.delegationSessionDataStore.edit { it.clear() }
    }
}
