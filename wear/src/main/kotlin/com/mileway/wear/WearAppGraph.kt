package com.mileway.wear

import android.content.Context
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.platform.di.platformModule
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.RecognizedActivity
import com.mileway.stub.di.stubModule
import com.mileway.wear.gms.watchSyncKoinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * `:wear`'s own [ActivityRecognizer]: a single UNKNOWN emission, never updated.
 *
 * `trackingModule` is shared with the phone, and `LocationTrackingService` resolves an
 * [ActivityRecognizer] from Koin — but the two real bindings live in `:app`'s flavor source sets
 * (Play Services on gms, the placeholder classifier on noGms), neither of which is on the watch's
 * classpath, and `:wear` must not depend on `:app`. Without a binding here the watch graph is
 * incomplete: the first watch-side caller that starts the tracking service gets
 * `NoDefinitionFound` at runtime rather than a compile error.
 *
 * ponytail: intentionally not a classifier — the IMU MotionState fusion (`core:platform`) is the
 * real signal on the watch too. If Wear ever needs coarse activity, bind a gms-flavor
 * `ActivityRecognition`-backed impl under `wear/src/gms` (where play-services is already
 * available), not here.
 */
private class WearActivityRecognizer : ActivityRecognizer {
    override val activity: Flow<RecognizedActivity> = flowOf(RecognizedActivity.UNKNOWN)
}

/**
 * P2.4: `:wear`'s own module for its Compose screens' ViewModels — kept separate from
 * `trackingModule` (which is shared with the phone/iOS graph and knows nothing about Wear-specific
 * presentation types like [com.mileway.wear.WearViewModel]) — plus the watch-side bindings for
 * contracts `trackingModule` consumes but does not itself provide (see [WearActivityRecognizer]).
 */
private val wearModule =
    module {
        viewModelOf(::WearViewModel)
        single<ActivityRecognizer> { WearActivityRecognizer() }
    }

/**
 * P2.1: the Wear app's own Koin bootstrap.
 *
 * Deliberately does NOT reuse `core:ui`'s `initKoin()` — that helper exists to prepend
 * `platformModule()` for the CMP phone/iOS graph, but `core:ui` itself is the Compose Multiplatform
 * theming module (`MilewayTheme`, the phone Material3 design system) that Wear OS must never depend
 * on (Wear renders with `androidx.wear.compose`, its own theme — see P2.3). [WearAppGraph] wires the
 * same headless graph the phone boots (`platformModule` + `coreDataModule` + `stubModule` +
 * `trackingModule`, all Android-actual/mock, zero network) without pulling in a single Compose
 * Multiplatform dependency, so [WearActivity], the tile and the complication services (P2.6/P2.7)
 * can all resolve `WatchFacade` and friends from the one shared instance.
 *
 * P2.9: also wires [watchSyncKoinModule] — the per-flavor `WatchSyncBridge` binding
 * (`wear/src/gms`'s real Data Layer bridge, `wear/src/noGms`'s [com.mileway.core.data.watch.NoopWatchSyncBridge]).
 */
object WearAppGraph {

    /** Idempotent: Activity, tile and complication processes may all call this before touching Koin. */
    fun start(context: Context) {
        val alreadyStarted = runCatching { KoinPlatform.getKoin() }.isSuccess
        if (alreadyStarted) stopKoin()
        startKoin {
            androidContext(context.applicationContext)
            androidLogger(Level.ERROR)
            modules(
                platformModule(),
                coreDataModule,
                stubModule,
                trackingModule,
                watchSyncKoinModule(),
                wearModule,
            )
        }
    }
}
