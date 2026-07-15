package com.mileway.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mileway.core.platform.PlatformBindings
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ShareSheet
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManager
import com.siddharth.kmp.appshell.AppUpdateManager
import org.koin.mp.KoinPlatform

/**
 * Desktop [LocalManagerProvider]: same no-Activity-needed shape as the iOS actual (a dashboard window
 * isn't Activity/UIViewController-scoped either) — resolves plain Koin singletons, falling back to the
 * shared [PlatformBindings] no-op when a binding or Koin itself is absent.
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
