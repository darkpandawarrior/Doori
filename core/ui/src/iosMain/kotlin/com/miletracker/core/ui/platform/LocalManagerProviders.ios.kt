package com.miletracker.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.miletracker.core.platform.AnalyticsHelper
import com.miletracker.core.platform.AppReviewManager
import com.miletracker.core.platform.AppUpdateManager
import com.miletracker.core.platform.PlatformBindings
import com.miletracker.core.platform.ReferralManager
import com.miletracker.core.platform.ShareSheet
import org.koin.mp.KoinPlatform

/**
 * iOS [LocalManagerProvider]: seeds the manager composition locals.
 *
 * iOS managers don't need a UIViewController for construction (the App-Store update check uses the bundle;
 * review uses SKStoreReviewController's window scene), so they're resolved directly from Koin as plain
 * singletons. Everything degrades to the shared [PlatformBindings] no-op when the binding — or Koin
 * itself — is absent, so it never crashes (iOS may host Compose before Koin is started).
 */
@Composable
actual fun LocalManagerProvider(content: @Composable () -> Unit) {
    val defaults = remember { PlatformBindings() }
    val koin = remember { runCatching { KoinPlatform.getKoin() }.getOrNull() }

    val appUpdateManager = remember(koin) { koin?.getOrNull<AppUpdateManager>() ?: defaults.appUpdateManager }
    val appReviewManager = remember(koin) { koin?.getOrNull<AppReviewManager>() ?: defaults.appReviewManager }
    val shareSheet = remember(koin) { koin?.getOrNull<ShareSheet>() ?: defaults.shareSheet }
    val analyticsHelper = remember(koin) { koin?.getOrNull<AnalyticsHelper>() ?: defaults.analyticsHelper }
    val referralManager = remember(koin) { koin?.getOrNull<ReferralManager>() ?: defaults.referralManager }

    ProvideManagers(
        appUpdateManager = appUpdateManager,
        appReviewManager = appReviewManager,
        shareSheet = shareSheet,
        analyticsHelper = analyticsHelper,
        referralManager = referralManager,
        content = content,
    )
}
