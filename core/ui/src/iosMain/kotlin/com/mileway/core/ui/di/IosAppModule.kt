package com.mileway.core.ui.di

import com.mileway.core.platform.AnalyticsHelper
import com.mileway.core.platform.DeepLinkHandler
import com.mileway.core.platform.DefaultDeepLinkHandler
import com.mileway.core.platform.InMemoryPushTokenStore
import com.mileway.core.platform.InMemoryReferralStore
import com.mileway.core.platform.LocalPushMessaging
import com.mileway.core.platform.LocalReferralManager
import com.mileway.core.platform.LoggingAnalyticsHelper
import com.mileway.core.platform.PushMessaging
import com.mileway.core.platform.PushTokenStore
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ReferralStore
import com.mileway.core.platform.ReviewTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS app-level bindings (KOIN.2), the shared, mock/offline app services the Android `appModule` provides,
 * minus the Android-only pieces (DemoConfigManager, geofence list, WorkManager). Combined with
 * [com.mileway.core.platform.di.platformModule] (which supplies the iOS `AppUpdateManager`/`AppReviewManager`),
 * this lets `LocalManagerProvider` resolve real iOS managers instead of falling back to the `PlatformBindings`
 * no-op. FOSS analytics = Napier logging; referral = the shared local code-gen manager (no Install Referrer on iOS).
 */
val iosAppModule: Module =
    module {
        single<AnalyticsHelper> { LoggingAnalyticsHelper() }

        single<ReferralStore> { InMemoryReferralStore() }
        single { LocalReferralManager(get()) }
        single<ReferralManager> { get<LocalReferralManager>() }

        single<PushTokenStore> { InMemoryPushTokenStore() }
        single<PushMessaging> { LocalPushMessaging(get()) }

        single<DeepLinkHandler> { DefaultDeepLinkHandler() }
        // PLAN_V24 P12.3: NSUserDefaults-backed review counters + the plan's 7-day account-age gate.
        single {
            ReviewTracker(
                store = com.mileway.core.ui.review.IosReviewStateStore(),
                config = com.mileway.core.platform.ReviewGateConfig(minAccountAgeDays = 7),
            )
        }
    }
