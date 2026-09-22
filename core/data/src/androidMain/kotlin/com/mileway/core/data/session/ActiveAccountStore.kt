package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activeAccountDataStore by preferencesDataStore(name = "active_account")

/**
 * The single source of truth for "which persona is active", persisted to DataStore so it
 * survives process death (see PLAN_V22 P2.1). Before this, the active persona lived only in
 * `ProfileUiState.selectedAccountId`, an in-memory VM field reset to the first seeded account on
 * every fresh process — mirrors [SessionRepository]'s exact shape (`Flow<String?>` + suspend
 * setter) rather than inventing a new DataStore idiom.
 */
class ActiveAccountStore(
    private val context: Context,
) : ActiveAccountSource {
    private val activeAccountIdKey = stringPreferencesKey("active_account_id")

    override val activeAccountId: Flow<String?> =
        context.activeAccountDataStore.data.map { prefs -> prefs[activeAccountIdKey] }

    override suspend fun setActiveAccountId(accountId: String) {
        context.activeAccountDataStore.edit { prefs ->
            prefs[activeAccountIdKey] = accountId
        }
    }
}
