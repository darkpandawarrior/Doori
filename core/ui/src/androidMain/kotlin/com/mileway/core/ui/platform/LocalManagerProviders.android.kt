package com.mileway.core.ui.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mileway.core.platform.PlatformBindings
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ShareSheet
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import org.koin.mp.KoinPlatform

/**
 * Android [LocalManagerProvider]: seeds the manager composition locals.
 *
 * Activity-scoped managers (update/review) are built from the host Activity via their Koin-bound factory;
 * app-scoped managers (share/analytics/referral) are resolved directly. Everything degrades to the shared
 * [PlatformBindings] no-op when the binding (or Koin itself) is absent, so it never crashes, including
 * previews/tests where Koin may not be started.
 */
@Composable
actual fun LocalManagerProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val defaults = remember { PlatformBindings() }
    val koin = remember { runCatching { KoinPlatform.getKoin() }.getOrNull() }

    val appUpdateManager =
        remember(activity, koin) {
            val factory = koin?.getOrNull<AppUpdateManagerFactory>()
            if (factory != null && activity != null) factory.create(activity) else defaults.appUpdateManager
        }
    val appReviewManager =
        remember(activity, koin) {
            val factory = koin?.getOrNull<AppReviewManagerFactory>()
            if (factory != null && activity != null) factory.create(activity) else defaults.appReviewManager
        }
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

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
