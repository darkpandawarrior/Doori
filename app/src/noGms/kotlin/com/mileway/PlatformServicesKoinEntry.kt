package com.mileway

import com.mileway.core.data.watch.NoopWatchSyncBridge
import com.mileway.core.data.watch.WatchSyncBridge
import com.siddharth.kmp.common.CrashReporter
import com.mileway.core.platform.LocalReferralManager
import com.siddharth.kmp.common.NapierCrashReporter
import com.mileway.core.platform.PlatformBindings
import com.mileway.core.platform.ReferralManager
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import com.siddharth.kmp.appshell.LoggingAnalyticsHelper
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * noGms (FOSS / F-Droid) flavor: no-op platform services, no proprietary deps.
 *
 * In-app update is a no-op on F-Droid (updates come from the store client, not Play-Core). The factory
 * returns the shared [PlatformBindings] no-op so the LocalManagerProvider wiring is identical to gms.
 */
fun platformServicesKoinModule(): Module =
    module {
        single<AppUpdateManagerFactory> { AppUpdateManagerFactory { PlatformBindings().appUpdateManager } }
        // F-Droid has no in-app review API → no-op (a store-listing intent could open f-droid.org later).
        single<AppReviewManagerFactory> { AppReviewManagerFactory { PlatformBindings().appReviewManager } }
        // RF.2: F-Droid has no Install Referrer → the shared local manager (code gen + manual redemption only).
        single<ReferralManager> { get<LocalReferralManager>() }
        // CF.3: FOSS analytics = Napier logging (no proprietary backend).
        single<AnalyticsHelper> { LoggingAnalyticsHelper() }
        // CF.4: FOSS crash reporting = local Napier breadcrumbs/exceptions (no proprietary backend).
        single<CrashReporter> { NapierCrashReporter() }
        // P2.9: no Data Layer transport on F-Droid/FOSS → NoopWatchSyncBridge (no PhoneSnapshotPublisher
        // binding here either — there is nothing to push through on this flavor).
        single<WatchSyncBridge> { NoopWatchSyncBridge() }
    }
