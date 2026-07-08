package com.mileway.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

private val Context.sessionDataStore by preferencesDataStore(name = "user_session")

/**
 * App-wide sign-in session, persisted to DataStore so it survives process death and app restarts.
 *
 * The demo has no real auth, but the *session* (did the user pass the login screen, and how) must
 * persist — otherwise the splash/login theatre replays on every cold start and a guest is bounced
 * back to login, losing their place and any pending deep link. The launcher reads [sessionState]
 * to decide whether to skip straight to the app shell.
 *
 * PLAN_V22 P7.1: [signInWithCredentials] now also runs [MockPostLoginInitializer] to synthesize a
 * post-login bootstrap block (still no network call) and sets [SessionState.isFirstLoginPending]
 * so the app shell can show a one-time welcome banner.
 *
 * PLAN_V22 P7.5: [markWelcomeDisclaimerShown] persists [SessionState.hasShownWelcomeDisclaimer] so
 * `WelcomeDisclaimerSheet` surfaces on `LoginScreen` exactly once per install, not on every replay
 * of the login screen (e.g. after a sign-out).
 */
class SessionRepository(
    private val context: Context,
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
    private val emailVerifiedKey = booleanPreferencesKey("session_email_verified")
    private val avatarPathKey = stringPreferencesKey("session_avatar_path")
    private val corporateEmailKey = stringPreferencesKey("session_corporate_email")

    override val sessionState: Flow<SessionState> =
        context.sessionDataStore.data.map { prefs ->
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
                emailVerified = prefs[emailVerifiedKey] ?: false,
                avatarPath = prefs[avatarPathKey],
                corporateEmail = prefs[corporateEmailKey],
            )
        }

    /**
     * Persist a credentials sign-in (the demo accepts any non-empty email). Synthesizes a
     * post-login profile block via [MockPostLoginInitializer] — no profile fetch, purely local —
     * and flips [SessionState.isFirstLoginPending] so the welcome banner shows exactly once.
     */
    suspend fun signInWithCredentials(email: String) {
        val profile = postLoginInitializer.synthesizeProfile(email)
        context.sessionDataStore.edit { prefs ->
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
            // P1.3: a fresh sign-in must re-clear MFA (challenge the new login again if required).
            prefs[mfaDoneKey] = false
            // P2.1: a fresh sign-in re-opens the onboarding form (if the persona requires it).
            prefs[onboardingDoneKey] = false
            // P3.2: a fresh email starts unverified.
            prefs[emailVerifiedKey] = false
            // P4.4: corporate verification is per-account — clear it on a fresh sign-in.
            prefs.remove(corporateEmailKey)
        }
    }

    /** Persist a guest session so it survives navigation, deep links and process recreation. */
    suspend fun continueAsGuest() {
        context.sessionDataStore.edit { prefs ->
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
            // P1.3: guest sessions never do MFA.
            prefs[mfaDoneKey] = true
            // P2.1: guests skip signup onboarding.
            prefs[onboardingDoneKey] = true
        }
    }

    /**
     * Clears [SessionState.isFirstLoginPending] once the welcome banner has shown — one-shot,
     * never set back to true until the next fresh [signInWithCredentials].
     */
    suspend fun clearFirstLoginPending() {
        context.sessionDataStore.edit { prefs ->
            prefs[firstLoginPendingKey] = false
        }
    }

    /**
     * PLAN_V22 P7.4: marks this session as having completed `SetPinScreen`, so subsequent app
     * launches (until sign-out) route `AppStage.PIN` to `CheckPinScreen`'s verify path instead of
     * `SetPinScreen`'s setup path. The PIN digest itself lives in [PinHashSource], keyed by
     * [PIN_GATE_ACCOUNT_ID] — this flag is only the "has one been set" bit.
     */
    suspend fun markPinSet() {
        context.sessionDataStore.edit { prefs -> prefs[hasPinKey] = true }
    }

    /**
     * PLAN_V22 P7.5: marks that `WelcomeDisclaimerSheet` has been shown once on `LoginScreen`, so
     * it does not replay on subsequent compositions of the login screen (e.g. after sign-out).
     */
    suspend fun markWelcomeDisclaimerShown() {
        context.sessionDataStore.edit { prefs -> prefs[hasShownWelcomeDisclaimerKey] = true }
    }

    /** PLAN_V24 P1.3: marks this login's MFA step complete, so the auth flow proceeds to the PIN gate. */
    suspend fun markMfaDone() {
        context.sessionDataStore.edit { prefs -> prefs[mfaDoneKey] = true }
    }

    /**
     * PLAN_V24 P2.1: persists the signup onboarding form and marks it done. [displayName] is
     * derived from first+last by the caller; blank optional fields are simply not written.
     */
    suspend fun saveOnboarding(
        displayName: String,
        email: String?,
        gender: String,
        dateOfBirthMillis: Long?,
    ) {
        context.sessionDataStore.edit { prefs ->
            if (displayName.isNotBlank()) prefs[displayNameKey] = displayName
            if (!email.isNullOrBlank()) prefs[emailKey] = email
            if (gender.isNotBlank()) prefs[genderKey] = gender
            if (dateOfBirthMillis != null) prefs[dobKey] = dateOfBirthMillis
            prefs[onboardingDoneKey] = true
        }
    }

    /** PLAN_V24 P2.1: skip the onboarding form (allowed only when the persona shows Skip). */
    suspend fun skipOnboarding() {
        context.sessionDataStore.edit { prefs -> prefs[onboardingDoneKey] = true }
    }

    /** PLAN_V24 P2.2: records the app version whose What's-new sheet was acknowledged. */
    suspend fun markWhatsNewSeen(version: Int) {
        context.sessionDataStore.edit { prefs -> prefs[whatsNewSeenKey] = version }
    }

    /** PLAN_V24 P3.1: begin an OTP-verified phone change — persists the pending target for resume. */
    suspend fun startPhoneChange(target: String) {
        context.sessionDataStore.edit { prefs -> prefs[pendingPhoneKey] = target }
    }

    /** Commit the pending phone change on OTP verify (phone := pending, clear pending). */
    suspend fun commitPhoneChange() {
        context.sessionDataStore.edit { prefs ->
            prefs[pendingPhoneKey]?.let { prefs[phoneKey] = it }
            prefs.remove(pendingPhoneKey)
        }
    }

    /** Abandon the pending phone change (cancel / wrong-number). */
    suspend fun cancelPhoneChange() {
        context.sessionDataStore.edit { prefs -> prefs.remove(pendingPhoneKey) }
    }

    /** PLAN_V24 P3.2: mark the session email verified (after the demo verify-link is "clicked"). */
    suspend fun markEmailVerified() {
        context.sessionDataStore.edit { prefs -> prefs[emailVerifiedKey] = true }
    }

    /** PLAN_V24 P3.3: set (or, with null, clear) the local profile-photo path. */
    suspend fun setAvatarPath(path: String?) {
        context.sessionDataStore.edit { prefs ->
            if (path == null) prefs.remove(avatarPathKey) else prefs[avatarPathKey] = path
        }
    }

    /** PLAN_V24 P4.4: records the corporate email as verified (after its OTP is confirmed). */
    suspend fun markCorporateVerified(email: String) {
        context.sessionDataStore.edit { prefs -> prefs[corporateEmailKey] = email }
    }

    /** Clear the session (sign out) — returns the app to the login screen on next launch. */
    suspend fun signOut() {
        context.sessionDataStore.edit { it.clear() }
    }
}
