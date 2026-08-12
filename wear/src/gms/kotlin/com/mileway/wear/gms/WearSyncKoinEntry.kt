package com.mileway.wear.gms

import com.mileway.core.data.watch.WatchSyncBridge
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import com.mileway.core.data.watch.TrackingCommandSender

/**
 * P2.9: gms flavor — binds the real Data Layer [WatchSyncBridge]. Mirrors `:app`'s per-flavor
 * `platformServicesKoinModule()` entry-point pattern: one function of this name/shape per flavor
 * source set, called from [com.mileway.wear.WearAppGraph].
 *
 * P2.10: also binds [WearTrackingCommandSender] — the watch->phone command counterpart to the
 * phone->watch [WearDataLayerSyncBridge] above. Same gms-only confinement.
 */
fun watchSyncKoinModule(): Module =
    module {
        single<WatchSyncBridge> { WearDataLayerSyncBridge(androidContext()) }
        // Bound to the interface as well: wear/src/main's ViewModel can only see the interface,
        // and a noGms watch binds neither — which is what makes the control absent there rather
        // than present and inert.
        single { WearTrackingCommandSender(androidContext()) }
        single<TrackingCommandSender> { get<WearTrackingCommandSender>() }
    }
