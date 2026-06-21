package com.miletracker.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

/** How the current session was established. */
enum class SessionKind { NONE, CREDENTIALS, GUEST }

/** Persisted sign-in state. [isSignedIn] is true once the user has signed in (any kind). */
data class SessionState(
    val kind: SessionKind = SessionKind.NONE,
    val email: String? = null,
) {
    val isSignedIn: Boolean get() = kind != SessionKind.NONE
    val isGuest: Boolean get() = kind == SessionKind.GUEST
}

/** iOS mirror of the app-wide sign-in session (see the androidMain doc). */
class SessionRepository {
    private val kindKey = stringPreferencesKey("session_kind")
    private val emailKey = stringPreferencesKey("session_email")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "user_session.preferences_pb").toPath() },
        )

    val sessionState: Flow<SessionState> =
        store.data.map { prefs ->
            val kind =
                prefs[kindKey]?.let { stored ->
                    SessionKind.entries.firstOrNull { it.name == stored }
                } ?: SessionKind.NONE
            SessionState(kind = kind, email = prefs[emailKey])
        }

    suspend fun signInWithCredentials(email: String) {
        store.edit { prefs ->
            prefs[kindKey] = SessionKind.CREDENTIALS.name
            prefs[emailKey] = email
        }
    }

    suspend fun continueAsGuest() {
        store.edit { prefs ->
            prefs[kindKey] = SessionKind.GUEST.name
            prefs.remove(emailKey)
        }
    }

    suspend fun signOut() {
        store.edit { it.clear() }
    }
}
