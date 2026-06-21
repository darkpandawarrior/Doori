package com.miletracker

import com.miletracker.core.platform.AnalyticsHelper
import com.miletracker.core.platform.AppReviewManagerFactory
import com.miletracker.core.platform.AppUpdateManagerFactory
import com.miletracker.core.platform.CrashReporter
import com.miletracker.core.platform.ReferralManager
import com.miletracker.platform.gms.AndroidInstallReferrerManager
import com.miletracker.platform.gms.FirebaseAnalyticsHelper
import com.miletracker.platform.gms.FirebaseCrashReporter
import com.miletracker.platform.gms.PlayAppReviewManagerFactoryImpl
import com.miletracker.platform.gms.PlayAppUpdateManagerFactoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * gms flavor: real Play-Core / Firebase platform services.
 *
 * Mirrors [mapsKoinModule] — one per-flavor entry point. Review / FCM / analytics / crash factories are
 * bound here as their phases land (RV.2 / FCM.2 / CF.3 / CF.4).
 */
fun platformServicesKoinModule(): Module =
    module {
        single<AppUpdateManagerFactory> { PlayAppUpdateManagerFactoryImpl() }
        single<AppReviewManagerFactory> { PlayAppReviewManagerFactoryImpl() }
        // RF.2: wrap the shared LocalReferralManager with Install Referrer capture (fires once on creation).
        single<ReferralManager> {
            AndroidInstallReferrerManager(androidContext(), get()).also { it.captureInstallReferrer() }
        }
        // CF.3: real Firebase analytics on the Play build.
        single<AnalyticsHelper> { FirebaseAnalyticsHelper(androidContext()) }
        // CF.4: real Firebase Crashlytics on the Play build.
        single<CrashReporter> { FirebaseCrashReporter() }
    }
