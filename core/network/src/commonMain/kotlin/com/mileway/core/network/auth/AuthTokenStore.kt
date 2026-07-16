package com.mileway.core.network.auth

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PLAN_V34 P2/A6: the client's session-token lifecycle. The access token is short-lived (~15min,
 * matching the server's JWT TTL) and lives in memory only — [accessToken] resets to null on process
 * death, which is fine since [AuthApi.refresh] re-derives it from the persisted refresh token on the
 * next call. The refresh token (~30d) persists in [settings] — the toolkit's encrypted
 * `SecureSettingsFactory` output (Keychain on iOS, EncryptedSharedPreferences on Android) in
 * production DI, or an in-memory fake `Settings` in tests.
 */
class AuthTokenStore(private val settings: Settings) {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun refreshToken(): String? = settings.getStringOrNull(REFRESH_TOKEN_KEY)

    /** Stores a freshly issued/rotated token pair (login or a successful refresh). */
    fun store(
        accessToken: String,
        refreshToken: String,
    ) {
        _accessToken.value = accessToken
        settings.putString(REFRESH_TOKEN_KEY, refreshToken)
    }

    /** Wipes both tokens — called on logout, or when a refresh attempt is rejected. */
    fun clear() {
        _accessToken.value = null
        settings.remove(REFRESH_TOKEN_KEY)
    }

    private companion object {
        const val REFRESH_TOKEN_KEY = "auth_refresh_token"
    }
}
