package com.miletracker.core.platform

import kotlinx.coroutines.flow.Flow

/*
 * V15 platform-service surface: cross-cutting device/store capabilities (in-app update, review, push,
 * referral, deep links, share, shortcuts, analytics, crash reporting).
 *
 * Same contract as the original PlatformServices.kt: every interface is platform-neutral, implemented
 * per platform, and surfaced through Koin / the LocalManagerProvider Compose layer (PF.3). Each impl
 * MUST degrade to a no-op when its backing key/service is absent, never crash (KEY DECISION #4).
 */

// ─────────────────────────── In-app update ───────────────────────────

/** How an available update should be applied. */
enum class UpdateMode { FORCED, FLEXIBLE }

/** Demo-overridable gate config for the in-app update flow (mirrors a remote splash/config API). */
data class UpdateConfig(
    val enabled: Boolean = false,
    val mode: UpdateMode = UpdateMode.FLEXIBLE,
    val minSupportedVersionCode: Long = 0L,
    val staleDays: Int = 0,
    val priority: Int = 0,
)

/** Result of querying the store for a newer build. */
sealed interface UpdateAvailability {
    data object NotAvailable : UpdateAvailability

    data object InProgress : UpdateAvailability

    data object Downloaded : UpdateAvailability

    data class Available(
        val availableVersionCode: Long,
        val mode: UpdateMode,
        val priority: Int = 0,
    ) : UpdateAvailability
}

/**
 * In-app update. Android: Play-Core AppUpdateManager (gms) / no-op (noGms = F-Droid). iOS: iTunes
 * Lookup API version compare. Activity / UIViewController-scoped → provided via LocalManagerProvider.
 */
interface AppUpdateManager {
    suspend fun checkForUpdate(config: UpdateConfig): UpdateAvailability

    fun startUpdate(mode: UpdateMode)

    suspend fun completeFlexibleUpdate()
}

// ─────────────────────────── In-app review ───────────────────────────

/** In-app review prompt. Android: Play review (gms) / store intent (noGms). iOS: SKStoreReviewController. */
interface AppReviewManager {
    /** Launch the platform review flow if the host allows it; silently no-ops otherwise. */
    suspend fun promptForReview()
}

// ─────────────────────────── Deep links ───────────────────────────

/** Bridges a platform-delivered URL (Android intent / iOS NSUserActivity) into the shared router. */
interface DeepLinkHandler {
    /** Hot stream of incoming deep-link URIs in raw string form. */
    val incoming: Flow<String>

    /** Called by the platform entry point when a URL arrives. */
    fun handle(url: String)
}

// ─────────────────────────── Push messaging ───────────────────────────

/** Push token + topic surface. Android: FCM (gms) / no-op (noGms). iOS: APNs + Firebase. */
interface PushMessaging {
    suspend fun currentToken(): String?

    val onTokenRefresh: Flow<String>

    suspend fun subscribeTopic(topic: String)

    suspend fun unsubscribeTopic(topic: String)
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

// ─────────────────────────── Analytics ───────────────────────────

/**
 * A single analytics event. Param keys/values self-clamp to Firebase limits (≤ 40-char name/key,
 * ≤ 100-char value, ≤ 25 params) so impls never reject an event, openMF AnalyticsEvent pattern.
 */
data class AnalyticsEvent(
    val type: String,
    val params: Map<String, String> = emptyMap(),
) {
    /** Firebase-safe event name (≤ 40 chars). */
    val safeType: String get() = type.take(MAX_NAME)

    /** Firebase-safe params: ≤ 25 entries, keys ≤ 40 chars, values ≤ 100 chars. */
    val safeParams: Map<String, String>
        get() =
            params.entries
                .take(MAX_PARAMS)
                .associate { (key, value) -> key.take(MAX_NAME) to value.take(MAX_VALUE) }

    companion object {
        const val MAX_NAME = 40
        const val MAX_PARAMS = 25
        const val MAX_VALUE = 100
    }
}

/** Analytics sink. gms: FirebaseAnalytics; noGms + iOS: Napier-logging no-op. */
interface AnalyticsHelper {
    fun log(event: AnalyticsEvent)

    fun setUserProperty(
        name: String,
        value: String?,
    )
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
