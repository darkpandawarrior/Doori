package com.mileway.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.core.ui.platform.LocalReducedMotion
import com.mileway.feature.advances.di.advancesModule
import com.mileway.feature.agent.di.agentModule
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.cards.di.cardsModule
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.media.repository.FakeMediaRepository
import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.payables.di.payablesModule
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.profile.di.profileModule
import com.mileway.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.service.AppSyncTrigger
import com.mileway.feature.tracking.viewmodel.CheckInViewModel
import com.mileway.feature.travel.di.travelModule
import com.mileway.feature.whatsnew.di.whatsNewFeatureModule
import com.mileway.shared.ui.MilewayApp
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.di.stubModule
import com.mileway.ui.auth.authModule
import com.mileway.ui.auth.pinModule
import com.mileway.ui.home.firstLoginBannerModule
import com.mileway.ui.home.homeModule
import com.mileway.ui.home.whatsNewModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIViewController

/**
 * iOS's half of feature:media's graph. `mediaModule` (commonMain) binds everything that is
 * platform-agnostic and deliberately leaves `MediaRepository` unbound, because its only real
 * implementation, `RealMediaRepository`, does EXIF-corrected bitmap work and ML Kit OCR against an
 * `android.content.Context`. Android contributes `androidMediaModule`; iOS binds the offline
 * `FakeMediaRepository` that already ships in commonMain, so the media screens resolve and run
 * against canned OCR/upload results rather than crashing on a missing definition.
 *
 * ponytail: a Fake, not a Vision/PHPicker-backed iOS implementation. Writing that is a real feature,
 * not a parity move, and nothing on the iOS shell reaches the camera capture path yet — the screens
 * that do (`CameraCaptureScreen`, `AttachmentSelectionScreen`, `DocumentScanLauncher`) are still
 * androidMain-only. Swap this binding when the iOS capture path lands.
 */
private val iosMediaModule =
    module {
        single<MediaRepository> { FakeMediaRepository() }
    }

/**
 * V36 review FIX 4: `TrackMilesScreen`'s `checkInViewModel: CheckInViewModel = koinViewModel()`
 * default was unresolvable on iOS. Android only ever constructs [CheckInViewModel] in
 * `MilewayApplication.kt`'s app-level `appModule` (composition root), never inside `trackingModule`
 * itself — `feature:tracking` deliberately has no `:stub`/`DemoConfigManager` dependency on either
 * platform. This mirrors that same composition-root placement on iOS rather than adding a new
 * `:stub` dependency to `feature:tracking`. The `List<CheckInLocation>` mapping is duplicated (not
 * shared) from Android's block for the same reason: no module both platforms' code lives in without
 * introducing a new cross-module dependency for one mapping.
 */
private val iosCheckInModule =
    module {
        single<List<CheckInLocation>> {
            get<DemoConfigManager>().getMockCheckInLocations().map { mock ->
                CheckInLocation(
                    id = mock.id,
                    name = mock.name,
                    lat = mock.lat,
                    lng = mock.lng,
                    type = mock.type,
                    radiusMeters = mock.radiusMeters,
                )
            }
        }
        viewModel {
            CheckInViewModel(
                locationRepo = get(),
                hardwareEventRepo = get(),
                currentTrackRepository = get(),
                geoCheckInLocations = get<List<CheckInLocation>>(),
                defaultRadiusMeters = get<DemoConfigManager>().defaultGeoCheckInRadiusMeters,
            )
        }
    }

/**
 * iOS Compose entry point that renders the **real** Mileway app-shell ([MilewayApp]) — the shared
 * home dashboard + core feature screens under a bottom-tab bar — instead of the old component
 * showcase. Boots the shared Koin graph with every module the shell's screens resolve. Swift's
 * `ContentView` should call `MilewayAppViewControllerKt.MilewayAppViewController()`.
 */
fun MilewayAppViewController(): UIViewController {
    AppLog.init()
    initKoin(
        modules =
            listOf(
                coreDataModule,
                coreUiModule,
                iosAppModule,
                // PLAN_V33 C3: mirrors Android's MilewayApplication module list — trackingModule's
                // get<MilewayNetworkApi>()/get<ConfigProvider>() calls resolve from here.
                stubModule,
                homeModule,
                advancesModule,
                trackingModule,
                loggingModule,
                travelModule,
                // PLAN_V36 P8: the List/Detail screens MilewayApp now overlays need their repository
                // + ViewModels resolvable — Android gets this from MilewayApplication's module list.
                whatsNewFeatureModule,
                // iOS Koin parity, 2026-08-05. These eight were registered in MilewayApplication.kt
                // (Android's composition root) and missing here, so any screen resolving from them
                // crashed or silently no-op'd on iOS. Verified addable: approvals/events/payables/
                // payments carry nothing in androidMain but an AndroidManifest.xml, agentModule is
                // commonMain, and authModule/pinModule live in shared/commonMain.
                //
                // cardsModule joined them once its definition was hoisted out of
                // feature:cards/src/androidMain into commonMain. It carried no android import at
                // all — only commonMain types (CardsMockDataProviderFactory, CardSecurityManager
                // and the four card ViewModels) — so the hoist was a file move, not a rewrite.
                // :shared takes feature:cards as an api() dependency of commonMain, so iosMain
                // resolves it from here.
                //
                // profileModule and mediaModule joined them the same way, once each was SPLIT
                // rather than moved. Both were blocked by a small Android-only tail, not by their
                // bulk: ProfileModule.kt called org.koin.android.ext.koin.androidContext for
                // SecureKeyStore (plus the toolkit's Android-only AI artifacts and the two storage
                // ViewModels over core:data's Context-taking StorageRepository), and MediaModule.kt
                // injected an android.content.Context into RealMediaRepository. Those tails now live
                // in profileAndroidModule / androidMediaModule, registered next to their commonMain
                // halves in MilewayApplication.kt; iOS registers the commonMain halves here and
                // contributes its own MediaRepository via iosMediaModule below.
                //
                // appModule is still Android app-level and has no iOS counterpart by design.
                agentModule,
                cardsModule,
                approvalsModule,
                authModule,
                eventsModule,
                payablesModule,
                paymentsModule,
                pinModule,
                // V36 review fix: HomeScreen's koinViewModel<WhatsNewViewModel>() and
                // koinViewModel<FirstLoginBannerViewModel>() defaults (see HomeScreen's parameter
                // list) were unresolvable on iOS — Android registers both in MilewayApplication's
                // module list, iOS was missing them.
                whatsNewModule,
                firstLoginBannerModule,
                profileModule,
                mediaModule,
                iosMediaModule,
                // V36 review FIX 4: TrackMilesScreen's CheckInViewModel — see iosCheckInModule's KDoc.
                iosCheckInModule,
            ),
    )
    // PLAN_V34 P1: app-scoped outbox flush — connectivity edges for the process lifetime plus a
    // drain on every return to the foreground, mirroring Android's MilewayApplication hook.
    val appSyncTrigger = KoinPlatform.getKoin().get<AppSyncTrigger>()
    appSyncTrigger.start()
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> appSyncTrigger.onAppForeground() }
    return ComposeUIViewController {
        // PLAN_V36 P6: one-shot read of the OS-level "Reduce Motion" accessibility toggle,
        // mirroring the Android root's Settings.Global.ANIMATOR_DURATION_SCALE read — see
        // LocalReducedMotion's KDoc for why this isn't observed live.
        CompositionLocalProvider(LocalReducedMotion provides UIAccessibilityIsReduceMotionEnabled()) {
            LocalManagerProvider {
                AppHost { MilewayApp() }
            }
        }
    }
}
