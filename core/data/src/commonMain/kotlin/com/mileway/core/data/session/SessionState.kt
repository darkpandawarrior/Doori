package com.mileway.core.data.session

/** How the current session was established. */
enum class SessionKind { NONE, CREDENTIALS, GUEST }

/** Default mock tenant every synthesized identity is stamped with (no real multi-tenant backend yet). */
const val DEFAULT_SESSION_TENANT = "DEMO-TENANT"

/**
 * Persisted sign-in state. [isSignedIn] is true once the user has signed in (any kind).
 *
 * [employeeCode], [tenant] and [signedInAtMillis] are a mock identity block — deterministically
 * synthesized at sign-in time (see [SessionRepository]) rather than sourced from a real backend,
 * so downstream ownership-pointer/session-isolation logic (started_by_* columns, persona-switch
 * coordination) has something real and stable to key off instead of nothing.
 *
 * [displayName], [officeName], [themeColorHex] and [currencySymbol] are PLAN_V22 P7.1's post-login
 * bootstrap block — synthesized by [MockPostLoginInitializer] on sign-in (still no network call),
 * shaped like a real `profile/me` response instead of the two bare fields sign-in used to write.
 * [isFirstLoginPending] gates the one-shot welcome banner `FirstLoginBannerViewModel` clears after
 * its first display.
 *
 * [hasPin] is PLAN_V22 P7.4's local PIN-gate flag: true once the reviewer has completed
 * `SetPinScreen` for this session. `LauncherActivity` uses it to decide whether a fresh sign-in
 * routes to `AppStage.PIN`'s setup path (`SetPinScreen`) or its verify path (`CheckPinScreen`).
 *
 * [hasShownWelcomeDisclaimer] is PLAN_V22 P7.5's flag: true once `WelcomeDisclaimerSheet` has been
 * shown on `LoginScreen` for this device/install, so the real location/notification permission
 * disclaimer surfaces exactly once rather than replaying on every relaunch of the login screen.
 */
data class SessionState(
    val kind: SessionKind = SessionKind.NONE,
    val email: String? = null,
    val employeeCode: String? = null,
    val tenant: String = DEFAULT_SESSION_TENANT,
    val signedInAtMillis: Long? = null,
    val displayName: String? = null,
    val officeName: String? = null,
    val themeColorHex: String? = null,
    val currencySymbol: String? = null,
    val isFirstLoginPending: Boolean = false,
    val hasPin: Boolean = false,
    val hasShownWelcomeDisclaimer: Boolean = false,
    // PLAN_V24 P1.3: true once this session cleared the MFA step (per-login, reset on fresh sign-in).
    val mfaDone: Boolean = false,
) {
    val isSignedIn: Boolean get() = kind != SessionKind.NONE
    val isGuest: Boolean get() = kind == SessionKind.GUEST
}

/**
 * Deterministically derives a mock employee code from a sign-in email. Deterministic (not
 * random) so the same email always synthesizes the same identity across sign-ins, which
 * downstream ownership-pointer matching (e.g. `AccountBinding`, P3.3) depends on.
 */
fun deriveEmployeeCode(email: String): String = "EMP-" + email.hashCode().toString().takeLast(4)
