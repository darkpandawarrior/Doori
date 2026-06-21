package com.miletracker.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.miletracker.core.platform.AnalyticsHelper
import com.miletracker.core.platform.AppReviewManager
import com.miletracker.core.platform.AppUpdateManager
import com.miletracker.core.platform.PlatformBindings
import com.miletracker.core.platform.ReferralManager
import com.miletracker.core.platform.ShareSheet

/*
 * Compose access layer for the V15 platform managers (PF.3, mirrors openMF LocalManagerProviders).
 *
 * Activity / UIViewController-scoped managers (in-app update, review, share) can't be plain Koin
 * singletons — they need the host Activity (Android) or root UIViewController (iOS). They are seeded
 * once at each app root via [LocalManagerProvider] (PF.4) and read at the call site via `LocalX.current`,
 * so shared screens need NO expect/actual at all.
 *
 * Defaults resolve to the no-op PlatformBindings() values, so a screen that reads `.current` outside a
 * provider (preview / test) degrades gracefully instead of crashing.
 */

private val defaultBindings = PlatformBindings()

val LocalAppUpdateManager = staticCompositionLocalOf<AppUpdateManager> { defaultBindings.appUpdateManager }

val LocalAppReviewManager = staticCompositionLocalOf<AppReviewManager> { defaultBindings.appReviewManager }

val LocalShareSheet = staticCompositionLocalOf<ShareSheet> { defaultBindings.shareSheet }

val LocalAnalyticsHelper = staticCompositionLocalOf<AnalyticsHelper> { defaultBindings.analyticsHelper }

val LocalReferralManager = staticCompositionLocalOf<ReferralManager> { defaultBindings.referralManager }

/**
 * Seeds the manager composition locals. Shared between the Android and iOS [LocalManagerProvider]
 * actuals so the provided-locals set stays in one place. Each actual resolves its real managers (with a
 * no-op fallback) and passes them here.
 */
@Composable
internal fun ProvideManagers(
    appUpdateManager: AppUpdateManager,
    appReviewManager: AppReviewManager,
    shareSheet: ShareSheet,
    analyticsHelper: AnalyticsHelper,
    referralManager: ReferralManager,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppUpdateManager provides appUpdateManager,
        LocalAppReviewManager provides appReviewManager,
        LocalShareSheet provides shareSheet,
        LocalAnalyticsHelper provides analyticsHelper,
        LocalReferralManager provides referralManager,
        content = content,
    )
}

/**
 * Wrap each app root's content in this (PF.4): Android reads the host Activity, iOS the root
 * UIViewController, to construct Activity/UIViewController-scoped managers and seed the locals.
 */
@Composable
expect fun LocalManagerProvider(content: @Composable () -> Unit)
