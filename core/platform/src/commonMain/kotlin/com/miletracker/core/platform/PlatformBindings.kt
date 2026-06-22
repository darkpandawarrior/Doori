package com.miletracker.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

// Single facade grouping all platform service interfaces.
// Bound as `single<PlatformBindings>` in each platform's Koin module.
// No-op defaults let commonMain code run in tests without a real platform.
data class PlatformBindings(
    val locationTracker: LocationTracker = NoOpLocationTracker,
    val textRecognizer: TextRecognizer = NoOpTextRecognizer,
    val documentScanner: DocumentScanner = NoOpDocumentScanner,
    val notificationScheduler: NotificationScheduler = NoOpNotificationScheduler,
    val backgroundScheduler: BackgroundScheduler = NoOpBackgroundScheduler,
    val biometricAuthenticator: BiometricAuthenticator = NoOpBiometricAuthenticator,
    val permissionsProvider: PermissionsProvider = NoOpPermissionsProvider,
    // V15 cross-cutting services. Default no-op = what noGms / iOS-without-key / tests get for free.
    val appUpdateManager: AppUpdateManager = NoOpAppUpdateManager,
    val appReviewManager: AppReviewManager = NoOpAppReviewManager,
    val deepLinkHandler: DeepLinkHandler = NoOpDeepLinkHandler,
    val pushMessaging: PushMessaging = NoOpPushMessaging,
    val referralManager: ReferralManager = NoOpReferralManager,
    val shareSheet: ShareSheet = NoOpShareSheet,
    val urlOpener: UrlOpener = NoOpUrlOpener,
    val appShortcuts: AppShortcuts = NoOpAppShortcuts,
    val analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper,
    val crashReporter: CrashReporter = NoOpCrashReporter,
)

private object NoOpLocationTracker : LocationTracker {
    override val updates: Flow<GeoPoint> = emptyFlow()

    override suspend fun current(): GeoPoint? = null

    override fun start() = Unit

    override fun stop() = Unit
}

private object NoOpTextRecognizer : TextRecognizer {
    override suspend fun recognize(imageBytes: ByteArray): String = ""
}

private object NoOpDocumentScanner : DocumentScanner {
    override suspend fun scan(maxPages: Int): List<ByteArray> = emptyList()
}

private object NoOpNotificationScheduler : NotificationScheduler {
    override suspend fun ensurePermission(): Boolean = false

    override fun notify(
        id: Int,
        title: String,
        body: String,
    ) = Unit

    override fun cancel(id: Int) = Unit
}

private object NoOpBackgroundScheduler : BackgroundScheduler {
    override fun schedulePeriodic(
        uniqueName: String,
        intervalMinutes: Long,
    ) = Unit

    override fun cancel(uniqueName: String) = Unit
}

private object NoOpBiometricAuthenticator : BiometricAuthenticator {
    override fun isAvailable(): Boolean = false

    override suspend fun authenticate(reason: String): BiometricResult = BiometricResult.Unavailable
}

private object NoOpPermissionsProvider : PermissionsProvider {
    override suspend fun isGranted(permission: AppPermission): Boolean = false

    override suspend fun request(permission: AppPermission): PermissionResult = PermissionResult.Denied
}

// ─────────────────────── V15 service no-op defaults ───────────────────────

private object NoOpAppUpdateManager : AppUpdateManager {
    override suspend fun checkForUpdate(config: UpdateConfig): UpdateAvailability = UpdateAvailability.NotAvailable

    override fun startUpdate(mode: UpdateMode) = Unit

    override suspend fun completeFlexibleUpdate() = Unit
}

private object NoOpAppReviewManager : AppReviewManager {
    override suspend fun promptForReview() = Unit
}

private object NoOpDeepLinkHandler : DeepLinkHandler {
    override val incoming: Flow<String> = emptyFlow()

    override fun handle(url: String) = Unit
}

private object NoOpPushMessaging : PushMessaging {
    override suspend fun currentToken(): String? = null

    override val onTokenRefresh: Flow<String> = emptyFlow()

    override suspend fun subscribeTopic(topic: String) = Unit

    override suspend fun unsubscribeTopic(topic: String) = Unit
}

private object NoOpReferralManager : ReferralManager {
    override suspend fun myReferralCode(): String = ""

    override fun pendingReferral(): Flow<ReferralData?> = flowOf(null)

    override suspend fun redeem(code: String): Boolean = false
}

private object NoOpShareSheet : ShareSheet {
    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) = Unit
}

private object NoOpUrlOpener : UrlOpener {
    override fun open(url: String) = Unit
}

private object NoOpAppShortcuts : AppShortcuts {
    override fun setDynamicShortcuts(shortcuts: List<AppShortcut>) = Unit
}

private object NoOpAnalyticsHelper : AnalyticsHelper {
    override fun log(event: AnalyticsEvent) = Unit

    override fun setUserProperty(
        name: String,
        value: String?,
    ) = Unit
}

private object NoOpCrashReporter : CrashReporter {
    override fun log(message: String) = Unit

    override fun recordException(throwable: Throwable) = Unit

    override fun setCustomKey(
        key: String,
        value: String,
    ) = Unit

    override fun setEnabled(enabled: Boolean) = Unit
}
