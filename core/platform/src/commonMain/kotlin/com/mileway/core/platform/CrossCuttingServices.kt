package com.mileway.core.platform

import kotlinx.coroutines.flow.Flow

/*
 * Cross-cutting device/store capabilities that stayed Mileway-side of the :app-shell extraction
 * (see AgentHarness/plans/kmp-toolkit-family/CONSOLIDATION-BACKLOG.md #22): deep links, referral,
 * share, URL open, app shortcuts, crash reporting. Each interface is platform-neutral, implemented
 * per platform, and surfaced through Koin / the LocalManagerProvider Compose layer (PF.3). Every impl
 * MUST degrade to a no-op when its backing key/service is absent, never crash (KEY DECISION #4).
 */

// ─────────────────────────── Deep links ───────────────────────────

/** Bridges a platform-delivered URL (Android intent / iOS NSUserActivity) into the shared router. */
interface DeepLinkHandler {
    /** Hot stream of incoming deep-link URIs in raw string form. */
    val incoming: Flow<String>

    /** Called by the platform entry point when a URL arrives. */
    fun handle(url: String)
}

// ─────────────────────────── Referral ───────────────────────────

/** A referral attribution captured from an install referrer or a deferred deep link. */
data class ReferralData(
    val code: String,
    val source: String? = null,
    val campaign: String? = null,
)

/** Referral codes + attribution. Android: InstallReferrer; iOS: deferred deep link / pasteboard. */
interface ReferralManager {
    suspend fun myReferralCode(): String

    fun pendingReferral(): Flow<ReferralData?>

    suspend fun redeem(code: String): Boolean
}

// ─────────────────────────── Share ───────────────────────────

/** Native share sheet. Android: ACTION_SEND / ShareCompat; iOS: UIActivityViewController. */
interface ShareSheet {
    fun share(
        text: String,
        subject: String? = null,
        fileUri: String? = null,
    )
}

// ─────────────────────────── URL opener ───────────────────────────

/** Open a URL in the native browser or registered handler. Android: ACTION_VIEW; iOS: UIApplication.open. */
interface UrlOpener {
    fun open(url: String)
}

// ─────────────────────────── App shortcuts ───────────────────────────

/** A home-screen quick action mapped to a deep link. */
data class AppShortcut(
    val id: String,
    val shortLabel: String,
    val longLabel: String,
    val deepLink: String,
)

/** Dynamic quick actions. Android: ShortcutManagerCompat; iOS: UIApplicationShortcutItem. */
interface AppShortcuts {
    fun setDynamicShortcuts(shortcuts: List<AppShortcut>)
}

// ─────────────────────────── Crash reporting ───────────────────────────

/** Crash + non-fatal reporting. gms: Firebase Crashlytics; noGms + iOS: no-op. */
interface CrashReporter {
    fun log(message: String)

    fun recordException(throwable: Throwable)

    fun setCustomKey(
        key: String,
        value: String,
    )

    fun setEnabled(enabled: Boolean)
}
