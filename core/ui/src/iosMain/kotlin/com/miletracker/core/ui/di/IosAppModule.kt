package com.miletracker.core.ui.di

import com.miletracker.core.platform.AnalyticsHelper
import com.miletracker.core.platform.DeepLinkHandler
import com.miletracker.core.platform.DefaultDeepLinkHandler
import com.miletracker.core.platform.InMemoryPushTokenStore
import com.miletracker.core.platform.InMemoryReferralStore
import com.miletracker.core.platform.LocalPushMessaging
import com.miletracker.core.platform.LocalReferralManager
import com.miletracker.core.platform.LoggingAnalyticsHelper
import com.miletracker.core.platform.PushMessaging
import com.miletracker.core.platform.PushTokenStore
import com.miletracker.core.platform.ReferralManager
import com.miletracker.core.platform.ReferralStore
import com.miletracker.core.platform.ReviewTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS app-level bindings (KOIN.2) — the shared, mock/offline app services the Android `appModule` provides,
 * minus the Android-only pieces (DemoConfigManager, geofence list, WorkManager). Combined with
 * [com.miletracker.core.platform.di.platformModule] (which supplies the iOS `AppUpdateManager`/`AppReviewManager`),
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
        single { ReviewTracker() }
    }
