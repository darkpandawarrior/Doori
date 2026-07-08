package com.mileway

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.appfunctions.service.AppFunctionConfiguration
import com.mileway.appfunctions.MileageAppFunctions
import com.mileway.core.common.AppLog
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.di.coreUiModule
import com.mileway.feature.agent.di.agentModule
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.payables.di.payablesModule
import com.mileway.feature.cards.di.cardsModule
import com.mileway.feature.travel.di.travelModule
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.profile.di.profileModule
import com.mileway.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.viewmodel.CheckInViewModel
import com.mileway.seeder.DatabaseSeeder
import com.mileway.ui.auth.authModule
import com.mileway.ui.auth.pinModule
import com.mileway.ui.home.firstLoginBannerModule
import com.mileway.ui.home.whatsNewModule
import com.mileway.ui.home.homeModule
import com.mileway.debug.WormaCeptorHelper
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.di.stubModule
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.worker.AutoDiscardTask
import com.mileway.feature.tracking.worker.MileageMaintenanceTask
import com.mileway.feature.tracking.worker.MilewayWorkerFactory
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.enqueuePeriodic
import dev.brewkits.kmpworkmanager.kmpWorkerModule
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.mileway.core.data.watch.PhoneSnapshotSync
import com.mileway.core.ui.di.initKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import dev.tmapps.konnection.Konnection

val appModule = module {
    single { DatabaseSeeder(get(), get()) }

    // V15 RV.4: engagement-gated in-app review tracker (in-memory counters for the demo).
    single { com.mileway.core.platform.ReviewTracker() }

    // V15 DL.4: shared deep-link handler (runtime/iOS-bridge links observe its incoming flow).
    single<com.mileway.core.platform.DeepLinkHandler> { com.mileway.core.platform.DefaultDeepLinkHandler() }

    // V15 FCM.1/FCM.2: shared push token store (FCM service writes onNewToken here) + offline messaging.
    single<com.mileway.core.platform.PushTokenStore> { com.mileway.core.platform.InMemoryPushTokenStore() }
    single<com.mileway.core.platform.PushMessaging> { com.mileway.core.platform.LocalPushMessaging(get()) }

    // V15 RF: shared referral store + base manager. The flavor module binds ReferralManager (gms wraps this
    // with Install Referrer capture; noGms uses it directly).
    single<com.mileway.core.platform.ReferralStore> { com.mileway.core.platform.InMemoryReferralStore() }
    single { com.mileway.core.platform.LocalReferralManager(get()) }

    // V15 CF.1: typed feature-flag reader over ConfigProvider.getFeatureFlags() (env-overridable).
    single {
        com.mileway.core.platform.FeatureFlags(
            get<com.mileway.core.network.config.ConfigProvider>().getFeatureFlags(),
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

    // F0.5: master-search aggregator. getAll<SearchProvider>() collects every feature's contribution
    // (each binds its own provider in its module), so this fans out with zero coupling to feature modules.
    single { com.mileway.core.data.search.MasterSearchRepository(getAll<com.mileway.core.data.search.SearchProvider>()) }
    viewModel { com.mileway.ui.search.MasterSearchViewModel(get()) }
}

class MilewayApplication : Application(), SingletonImageLoader.Factory, AppFunctionConfiguration.Provider {

    // Coil 3: SingletonImageLoader.Factory#newImageLoader now receives a PlatformContext.
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
            add(SvgDecoder.Factory())
        }
        .build()

    // P7.5: AppFunctions looks up the enclosing-class instance for @AppFunction methods through
    // this factory (KSP-generated code calls it lazily, at execution time). The lambda resolves
    // Koin at call time via KoinPlatform (not the `get()` Application extension), because this
    // property is built during Application construction — BEFORE onCreate()/initKoin() runs.
    override val appFunctionConfiguration: AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(MileageAppFunctions::class.java) {
                val koin = org.koin.mp.KoinPlatform.getKoin()
                MileageAppFunctions(
                    watchFacade = koin.get(),
                    expenseRepository = koin.get(),
                    snapshotCache = koin.get(),
                )
            }
            .build()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // KMP logging (Napier), only base an antilog on debuggable builds; release stays silent.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) AppLog.init()
        // Initialize WormaCeptor for HTTP inspection in debug builds (no-op in release).
        WormaCeptorHelper.init(this)
        // Initialize konnection for KMP network connectivity monitoring.
        Konnection.createInstance(this)
        // KOIN.1: shared bootstrap. initKoin() prepends platformModule() (the per-platform service graph,
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
                kmpWorkerModule(workerFactory = MilewayWorkerFactory()),
                loggingModule,
                mediaModule,
                profileModule,
                approvalsModule,
                payablesModule,
                travelModule,
                cardsModule,
                agentModule,
                paymentsModule,
                eventsModule,
                homeModule,
                firstLoginBannerModule,
                whatsNewModule,
                authModule,
                pinModule,
                appModule
            ),
            appDeclaration = {
                androidContext(this@MilewayApplication)
                androidLogger(Level.ERROR)
            },
        )
        appScope.launch {
            get<DatabaseSeeder>().seedIfEmpty()
            scheduleWeeklyMaintenance()
            seedAppShortcuts()
        }
        // P2.9: phone->watch snapshot sync. Harmless on noGms (WatchSyncBridge is a Noop there).
        get<PhoneSnapshotSync>().start(appScope)
        // P-C.4: run ghost-session reconciliation off the main thread immediately after Koin is up.
        appScope.launch {
            val outcome = get<SessionReconciliationPolicy>().reconcile()
            val source = get<com.mileway.core.data.session.CurrentTrackDataSource>()
            if (outcome is SessionReconciliationPolicy.Outcome.DiscardStale) source.clearSession()
            get<ReconciliationResultHolder>().post(outcome)
        }
    }

    /** SH.3: publish home-screen quick actions that deep-link into the main flows. */
    private fun seedAppShortcuts() {
        get<com.mileway.core.platform.AppShortcuts>().setDynamicShortcuts(
            listOf(
                com.mileway.core.platform.AppShortcut("track", "Track", "Track a trip", "mileway://track"),
                com.mileway.core.platform.AppShortcut("log", "Log miles", "Log miles & expenses", "mileway://log"),
                com.mileway.core.platform.AppShortcut("profile", "Profile", "Open your profile", "mileway://profile"),
            ),
        )
    }

    private suspend fun scheduleWeeklyMaintenance() {
        val scheduler = get<BackgroundTaskScheduler>()
        scheduler.enqueuePeriodic(
            MileageMaintenanceTask.TASK_ID,
            MileageMaintenanceTask.WORKER_CLASS,
            MileageMaintenanceTask.INTERVAL_MINUTES.minutes.inWholeMilliseconds,
        )
        scheduler.enqueuePeriodic(
            AutoDiscardTask.TASK_ID,
            AutoDiscardTask.WORKER_CLASS,
            AutoDiscardTask.INTERVAL_MINUTES.minutes.inWholeMilliseconds,
        )
    }
}
