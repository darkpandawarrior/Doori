package com.mileway

import com.mileway.core.data.watch.WatchSyncBridge
import com.mileway.core.media.BarcodeDecoder
import com.mileway.core.platform.ReferralManager
import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.RealLocationSourceFactory
import com.mileway.platform.gms.AndroidInstallReferrerManager
import com.mileway.platform.gms.FirebaseAnalyticsHelper
import com.mileway.platform.gms.FirebaseCrashReporter
import com.mileway.platform.gms.PlayAppReviewManagerFactoryImpl
import com.mileway.platform.gms.PlayAppUpdateManagerFactoryImpl
import com.mileway.platform.gms.WearDataLayerWatchSyncBridge
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import com.siddharth.kmp.appshell.LocationTracker
import com.siddharth.kmp.appshell.gms.GmsFusedLocationTracker
import com.siddharth.kmp.common.CrashReporter
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * gms flavor: real Play-Core / Firebase platform services.
 *
 * Mirrors [mapsKoinModule], one per-flavor entry point. Review / FCM / analytics / crash factories are
 * bound here as their phases land (RV.2 / FCM.2 / CF.3 / CF.4).
 */
fun platformServicesKoinModule(): Module =
    module {
        // L13: fused location on the gms flavor — overrides core:platform's default
        // AndroidLocationTracker (plain LocationManager) the same way trackingModule's
        // NotificationScheduler override works (last-registered Koin definition wins; see
        // core:ui's initKoin kdoc). GmsFusedLocationTracker + play-services-location live in the
        // opt-in app-shell-location-gms module (gmsImplementation only), so noGms never sees them.
        single<LocationTracker> { GmsFusedLocationTracker(androidContext()) }
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
        // P2.9: real Data Layer WatchSyncBridge (PhoneSnapshotSync, the observe+push loop, is
        // bound once in coreDataModule since it's flavor-agnostic — see its doc comment).
        single<WatchSyncBridge> { WearDataLayerWatchSyncBridge(androidContext()) }
        // FLFD.2 fix: real ML Kit barcode decoding, gms flavor ONLY (see BarcodeDecoder.kt).
        single<BarcodeDecoder> { MlKitBarcodeDecoder() }
        // PLAN_V37 Phase 1: real Play Services ActivityRecognition, gms flavor ONLY — moved out of
        // feature/tracking's shared androidMain (see GmsActivityRecognizer.kt kdoc).
        single<ActivityRecognizer> { GmsActivityRecognizer(androidContext()) }
        // L13: real fused GPS, gms flavor ONLY — moved out of feature/tracking's shared androidMain
        // (see GmsFusedLocationSource.kt kdoc for why).
        single<RealLocationSourceFactory> {
            RealLocationSourceFactory { forceGpsOnly, initialIntervalMs ->
                GmsFusedLocationSource(androidContext(), initialIntervalMs, forceGpsOnly)
            }
        }
    }
