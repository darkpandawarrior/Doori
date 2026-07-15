package com.mileway.stub.di

import com.mileway.core.data.network.DataStoreBaseUrlProvider
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.StubPersonaPresetProvider
import com.siddharth.kmp.network.createHttpClient
import org.koin.dsl.module

/**
 * PLAN_V33 C3 (iOS pass): iOS counterpart of the Android `stubModule` — same bindings (ConfigProvider,
 * MilewayNetworkApi behind [NetworkBackendFlags.useRealBackend], PersonaPresetProvider), just swapping
 * [DataStoreBaseUrlProvider]'s constructor (no `androidContext()` on iOS; its no-arg iOS actual writes
 * to the app's temp directory instead of a Context-scoped DataStore).
 *
 * Before this, iOS had no [MilewayNetworkApi] binding at all — `get<MilewayNetworkApi>()` inside
 * `trackingModule` (VehiclePricingRepository, MileageSubmissionViewModel, realLocationSend/
 * realMilesSubmitSend) and `get<ConfigProvider>()` (TrackingConfigManager) would have thrown the
 * moment either was actually resolved.
 */
val stubModule =
    module {
        single { DemoConfigManager() }
        single<ConfigProvider> { get<DemoConfigManager>() }
        single<MilewayNetworkApi> {
            if (NetworkBackendFlags.useRealBackend) {
                KtorMilewayNetworkApi(
                    client =
                        createHttpClient(
                            // AUTH-DEFERRED: no session to clear yet — TokenProvider/real 401
                            // handling lands with B-auth/A-auth.
                            onUnauthorized = { },
                        ),
                    baseUrlProvider = DataStoreBaseUrlProvider(),
                )
            } else {
                FakeTrackingNetworkApi()
            }
        }
        // PLAN_V24 P0.2: the real PRESET layer — overrides core:data's EmptyPersonaPresetProvider
        // (Koin last-definition-wins). Persona differentiation for the Plugin Registry.
        single<PersonaPresetProvider> { StubPersonaPresetProvider() }
    }
