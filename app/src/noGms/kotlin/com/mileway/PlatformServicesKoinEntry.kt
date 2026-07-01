package com.mileway

import com.mileway.core.platform.AnalyticsHelper
import com.mileway.core.platform.AppReviewManagerFactory
import com.mileway.core.platform.AppUpdateManagerFactory
import com.mileway.core.platform.CrashReporter
import com.mileway.core.platform.LocalReferralManager
import com.mileway.core.platform.LoggingAnalyticsHelper
import com.mileway.core.platform.PlatformBindings
import com.mileway.core.platform.ReferralManager
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
        // CF.4: no crash reporting on FOSS → shared no-op.
        single<CrashReporter> { PlatformBindings().crashReporter }
    }
