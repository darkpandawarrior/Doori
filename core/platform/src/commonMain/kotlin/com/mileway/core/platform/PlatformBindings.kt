package com.mileway.core.platform

import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManager
import com.siddharth.kmp.appshell.AppUpdateManager
import com.siddharth.kmp.appshell.DocumentScanner
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.appshell.NoOpAnalyticsHelper
import com.siddharth.kmp.appshell.NoOpAppReviewManager
import com.siddharth.kmp.appshell.NoOpAppUpdateManager
import com.siddharth.kmp.appshell.NoOpDocumentScanner
import com.siddharth.kmp.appshell.NoOpLocationTracker
import com.siddharth.kmp.appshell.NoOpNotificationScheduler
import com.siddharth.kmp.appshell.NoOpPermissionsProvider
import com.siddharth.kmp.appshell.NoOpPushMessaging
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.PermissionsProvider
import com.siddharth.kmp.appshell.PushMessaging
import com.siddharth.kmp.common.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

// Single facade grouping all platform service interfaces — both this module's own (Haptics,
// ReferralManager, …) and :app-shell's (LocationTracker, NotificationScheduler, …).
// Bound as `single<PlatformBindings>` in each platform's Koin module.
// No-op defaults let commonMain code run in tests without a real platform.
data class PlatformBindings(
    val locationTracker: LocationTracker = NoOpLocationTracker,
    val documentScanner: DocumentScanner = NoOpDocumentScanner,
    val notificationScheduler: NotificationScheduler = NoOpNotificationScheduler,
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

// LocationTracker / DocumentScanner / NotificationScheduler / PermissionsProvider / AppUpdateManager /
// AppReviewManager / PushMessaging / AnalyticsHelper no-op defaults now live in :app-shell
// (NoOpDefaults.kt, public) — reused directly above rather than re-declared here.

private object NoOpBiometricAuthenticator : BiometricAuthenticator {
    override fun isAvailable(): Boolean = false

    override suspend fun authenticate(reason: String): BiometricResult = BiometricResult.Unavailable
}

private object NoOpDeepLinkHandler : DeepLinkHandler {
    override val incoming: Flow<String> = emptyFlow()

    override fun handle(url: String) = Unit
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

private object NoOpCrashReporter : CrashReporter {
    override fun log(breadcrumb: String) = Unit

    override fun recordException(
        throwable: Throwable,
        message: String?,
    ) = Unit

    override fun setCustomKey(
        key: String,
        value: String,
    ) = Unit

    override fun setUserId(id: String?) = Unit
}
