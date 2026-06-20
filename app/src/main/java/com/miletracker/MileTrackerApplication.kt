package com.miletracker

import android.app.Application
import android.content.pm.ApplicationInfo
import com.miletracker.core.common.AppLog
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder
import com.miletracker.core.data.di.coreDataModule
import com.miletracker.core.ui.di.coreUiModule
import com.miletracker.feature.agent.di.agentModule
import com.miletracker.feature.approvals.di.approvalsModule
import com.miletracker.feature.payables.di.payablesModule
import com.miletracker.feature.cards.di.cardsModule
import com.miletracker.feature.travel.di.travelModule
import com.miletracker.feature.logging.di.loggingModule
import com.miletracker.feature.media.di.mediaModule
import com.miletracker.feature.profile.di.profileModule
import com.miletracker.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.miletracker.feature.tracking.di.trackingModule
import com.miletracker.feature.tracking.viewmodel.CheckInViewModel
import com.miletracker.seeder.DatabaseSeeder
import com.miletracker.ui.home.homeModule
import com.miletracker.debug.WormaCeptorHelper
import com.miletracker.stub.DemoConfigManager
import com.miletracker.stub.di.stubModule
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.miletracker.feature.tracking.service.MileageMaintenanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.miletracker.core.ui.di.initKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import dev.tmapps.konnection.Konnection
import java.util.concurrent.TimeUnit

val appModule = module {
    single { DatabaseSeeder(get(), get()) }

    // V15 RV.4: engagement-gated in-app review tracker (in-memory counters for the demo).
    single { com.miletracker.core.platform.ReviewTracker() }

    // V15 DL.4: shared deep-link handler (runtime/iOS-bridge links observe its incoming flow).
    single<com.miletracker.core.platform.DeepLinkHandler> { com.miletracker.core.platform.DefaultDeepLinkHandler() }

    // V15 FCM.1/FCM.2: shared push token store (FCM service writes onNewToken here) + offline messaging.
    single<com.miletracker.core.platform.PushTokenStore> { com.miletracker.core.platform.InMemoryPushTokenStore() }
    single<com.miletracker.core.platform.PushMessaging> { com.miletracker.core.platform.LocalPushMessaging(get()) }

    // V15 RF: shared referral store + base manager. The flavor module binds ReferralManager (gms wraps this
    // with Install Referrer capture; noGms uses it directly).
    single<com.miletracker.core.platform.ReferralStore> { com.miletracker.core.platform.InMemoryReferralStore() }
    single { com.miletracker.core.platform.LocalReferralManager(get()) }

    // V15 CF.1: typed feature-flag reader over ConfigProvider.getFeatureFlags() (env-overridable).
    single {
        com.miletracker.core.platform.FeatureFlags(
            get<com.miletracker.core.network.config.ConfigProvider>().getFeatureFlags(),
        )
    }

    // Geofence location list: convert DemoConfigManager's mock locations into
    // CheckInValidator.CheckInLocation for local offline radius validation.
    single<List<CheckInLocation>> {
        get<DemoConfigManager>().getMockCheckInLocations().map { mock ->
            CheckInLocation(
                id = mock.id,
                name = mock.name,
                lat = mock.lat,
                lng = mock.lng,
                type = mock.type,
                radiusMeters = mock.radiusMeters
            )
        }
    }

    viewModel {
        CheckInViewModel(
            locationRepo = get(),
            hardwareEventRepo = get(),
            currentTrackRepository = get(),
            geoCheckInLocations = get<List<CheckInLocation>>(),
            defaultRadiusMeters = get<DemoConfigManager>().getDefaultGeoCheckInRadiusMeters()
        )
    }
}

class MileTrackerApplication : Application(), SingletonImageLoader.Factory {

    // Coil 3: SingletonImageLoader.Factory#newImageLoader now receives a PlatformContext.
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
            add(SvgDecoder.Factory())
        }
        .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // KMP logging (Napier) — only base an antilog on debuggable builds; release stays silent.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) AppLog.init()
        // Initialize WormaCeptor for HTTP inspection in debug builds (no-op in release).
        WormaCeptorHelper.init(this)
        // Initialize konnection for KMP network connectivity monitoring.
        Konnection.createInstance(this)
        // KOIN.1: shared bootstrap. initKoin() prepends platformModule() (the per-platform service graph —
        // LocationTracker/NotificationScheduler/TextRecognizer/BackgroundScheduler on Android) to the list,
        // wiring it into the Android graph for the first time. The NotificationScheduler it adds duplicates
        // trackingModule's binding; Koin override keeps the last one (same AndroidNotificationScheduler impl).
        initKoin(
            modules = listOf(
                mapsKoinModule(),
                platformServicesKoinModule(),
                coreDataModule,
                coreUiModule,
                stubModule,
                trackingModule,
                loggingModule,
                mediaModule,
                profileModule,
                approvalsModule,
                payablesModule,
                travelModule,
                cardsModule,
                agentModule,
                homeModule,
                appModule
            ),
            appDeclaration = {
                androidContext(this@MileTrackerApplication)
                androidLogger(Level.ERROR)
            },
        )
        appScope.launch {
            get<DatabaseSeeder>().seedIfEmpty()
            scheduleWeeklyMaintenance()
            seedAppShortcuts()
        }
    }

    /** SH.3: publish home-screen quick actions that deep-link into the main flows. */
    private fun seedAppShortcuts() {
        get<com.miletracker.core.platform.AppShortcuts>().setDynamicShortcuts(
            listOf(
                com.miletracker.core.platform.AppShortcut("track", "Track", "Track a trip", "miletracker://track"),
                com.miletracker.core.platform.AppShortcut("log", "Log miles", "Log miles & expenses", "miletracker://log"),
                com.miletracker.core.platform.AppShortcut("profile", "Profile", "Open your profile", "miletracker://profile"),
            ),
        )
    }

    private fun scheduleWeeklyMaintenance() {
        val request = PeriodicWorkRequestBuilder<MileageMaintenanceWorker>(7, TimeUnit.DAYS)
            .addTag(MileageMaintenanceWorker.TAG)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MileageMaintenanceWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
