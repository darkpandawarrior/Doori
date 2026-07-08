package com.mileway.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import kotlin.time.Clock

/**
 * iOS mirror of the app-wide sign-in session (see the androidMain doc).
 *
 * PLAN_V22 P7.1: [signInWithCredentials] runs [MockPostLoginInitializer] to synthesize the same
 * post-login bootstrap block androidMain persists — still no network call.
 */
class SessionRepository(
    private val postLoginInitializer: MockPostLoginInitializer,
) : SessionSource {
    private val kindKey = stringPreferencesKey("session_kind")
    private val emailKey = stringPreferencesKey("session_email")
    private val employeeCodeKey = stringPreferencesKey("session_employee_code")
    private val tenantKey = stringPreferencesKey("session_tenant")
    private val signedInAtKey = longPreferencesKey("session_signed_in_at")
    private val displayNameKey = stringPreferencesKey("session_display_name")
    private val officeNameKey = stringPreferencesKey("session_office_name")
    private val themeColorHexKey = stringPreferencesKey("session_theme_color_hex")
    private val currencySymbolKey = stringPreferencesKey("session_currency_symbol")
    private val firstLoginPendingKey = booleanPreferencesKey("session_first_login_pending")
    private val hasPinKey = booleanPreferencesKey("session_has_pin")
    private val hasShownWelcomeDisclaimerKey = booleanPreferencesKey("session_has_shown_welcome_disclaimer")
    private val mfaDoneKey = booleanPreferencesKey("session_mfa_done")
    private val onboardingDoneKey = booleanPreferencesKey("session_onboarding_done")
    private val genderKey = stringPreferencesKey("session_gender")
    private val dobKey = longPreferencesKey("session_dob")
    private val whatsNewSeenKey = intPreferencesKey("session_whats_new_seen")
    private val phoneKey = stringPreferencesKey("session_phone")
    private val pendingPhoneKey = stringPreferencesKey("session_pending_phone")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "user_session.preferences_pb").toPath() },
        )

    override val sessionState: Flow<SessionState> =
        store.data.map { prefs ->
            val kind =
                prefs[kindKey]?.let { stored ->
                    SessionKind.entries.firstOrNull { it.name == stored }
                } ?: SessionKind.NONE
            SessionState(
                kind = kind,
                email = prefs[emailKey],
                employeeCode = prefs[employeeCodeKey],
                tenant = prefs[tenantKey] ?: DEFAULT_SESSION_TENANT,
                signedInAtMillis = prefs[signedInAtKey],
                displayName = prefs[displayNameKey],
                officeName = prefs[officeNameKey],
                themeColorHex = prefs[themeColorHexKey],
                currencySymbol = prefs[currencySymbolKey],
                isFirstLoginPending = prefs[firstLoginPendingKey] ?: false,
                hasPin = prefs[hasPinKey] ?: false,
                hasShownWelcomeDisclaimer = prefs[hasShownWelcomeDisclaimerKey] ?: false,
                mfaDone = prefs[mfaDoneKey] ?: false,
                onboardingDone = prefs[onboardingDoneKey] ?: false,
                gender = prefs[genderKey] ?: "",
                dateOfBirthMillis = prefs[dobKey],
                whatsNewLastSeenVersion = prefs[whatsNewSeenKey] ?: 0,
                phone = prefs[phoneKey] ?: "",
                pendingPhoneChangeTarget = prefs[pendingPhoneKey],
            )
        }

    suspend fun signInWithCredentials(email: String) {
        val profile = postLoginInitializer.synthesizeProfile(email)
        store.edit { prefs ->
            prefs[kindKey] = SessionKind.CREDENTIALS.name
            prefs[emailKey] = email
            prefs[employeeCodeKey] = profile.employeeCode
            prefs[tenantKey] = DEFAULT_SESSION_TENANT
            prefs[signedInAtKey] = Clock.System.now().toEpochMilliseconds()
            prefs[displayNameKey] = profile.displayName
            prefs[officeNameKey] = profile.officeName
            prefs[themeColorHexKey] = profile.themeColorHex
            prefs[currencySymbolKey] = profile.currencySymbol
            prefs[firstLoginPendingKey] = true
            prefs[mfaDoneKey] = false
            prefs[onboardingDoneKey] = false
        }
    }

    suspend fun continueAsGuest() {
        store.edit { prefs ->
            prefs[kindKey] = SessionKind.GUEST.name
            prefs.remove(emailKey)
            prefs[employeeCodeKey] = deriveEmployeeCode("guest")
            prefs[tenantKey] = DEFAULT_SESSION_TENANT
            prefs[signedInAtKey] = Clock.System.now().toEpochMilliseconds()
            prefs.remove(displayNameKey)
            prefs.remove(officeNameKey)
            prefs.remove(themeColorHexKey)
            prefs.remove(currencySymbolKey)
            prefs.remove(firstLoginPendingKey)
            prefs[mfaDoneKey] = true
            prefs[onboardingDoneKey] = true
        }
    }

    /** Clears [SessionState.isFirstLoginPending] once the welcome banner has shown. */
    suspend fun clearFirstLoginPending() {
        store.edit { prefs ->
            prefs[firstLoginPendingKey] = false
        }
    }

    /** PLAN_V22 P7.4: marks this session as having completed `SetPinScreen` (see androidMain doc). */
    suspend fun markPinSet() {
        store.edit { prefs -> prefs[hasPinKey] = true }
    }

    /** PLAN_V22 P7.5: marks `WelcomeDisclaimerSheet` as shown (see androidMain doc). */
    suspend fun markWelcomeDisclaimerShown() {
        store.edit { prefs -> prefs[hasShownWelcomeDisclaimerKey] = true }
    }

    /** PLAN_V24 P1.3: marks this login's MFA step complete (see androidMain doc). */
    suspend fun markMfaDone() {
        store.edit { prefs -> prefs[mfaDoneKey] = true }
    }

    /** PLAN_V24 P2.1: persists the signup onboarding form (see androidMain doc). */
    suspend fun saveOnboarding(
        displayName: String,
        email: String?,
        gender: String,
        dateOfBirthMillis: Long?,
    ) {
        store.edit { prefs ->
            if (displayName.isNotBlank()) prefs[displayNameKey] = displayName
            if (!email.isNullOrBlank()) prefs[emailKey] = email
            if (gender.isNotBlank()) prefs[genderKey] = gender
            if (dateOfBirthMillis != null) prefs[dobKey] = dateOfBirthMillis
            prefs[onboardingDoneKey] = true
        }
    }

    /** PLAN_V24 P2.1: skip the onboarding form (see androidMain doc). */
    suspend fun skipOnboarding() {
        store.edit { prefs -> prefs[onboardingDoneKey] = true }
    }

    /** PLAN_V24 P2.2: records the acknowledged What's-new version (see androidMain doc). */
    suspend fun markWhatsNewSeen(version: Int) {
        store.edit { prefs -> prefs[whatsNewSeenKey] = version }
    }

    /** PLAN_V24 P3.1: phone-change markers (see androidMain doc). */
    suspend fun startPhoneChange(target: String) {
        store.edit { prefs -> prefs[pendingPhoneKey] = target }
    }

    suspend fun commitPhoneChange() {
        store.edit { prefs ->
            prefs[pendingPhoneKey]?.let { prefs[phoneKey] = it }
            prefs.remove(pendingPhoneKey)
        }
    }

    suspend fun cancelPhoneChange() {
        store.edit { prefs -> prefs.remove(pendingPhoneKey) }
    }

    suspend fun signOut() {
        store.edit { it.clear() }
    }
}
