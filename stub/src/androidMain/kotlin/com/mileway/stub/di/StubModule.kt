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
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * PLAN_V33 A3: flips [MilewayNetworkApi] from the offline [FakeTrackingNetworkApi] to the real
 * Ktor-backed [KtorMilewayNetworkApi] (talking to `:server`, see PLAN_V33 B1-B3). Default is
 * `false` — the app's behavior is unchanged unless something sets this before Koin resolves
 * [MilewayNetworkApi]. `:app` doesn't flip it yet (out of scope for A3); to opt in, set
 * `NetworkBackendFlags.useRealBackend = true` before `initKoin(...)` runs (e.g. at the top of
 * `MilewayApplication.onCreate()`, gated behind a debug build check).
 */
object NetworkBackendFlags {
    var useRealBackend: Boolean = false
}

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
                    baseUrlProvider = DataStoreBaseUrlProvider(androidContext()),
                )
            } else {
                FakeTrackingNetworkApi()
            }
        }
        // PLAN_V24 P0.2: the real PRESET layer — overrides core:data's EmptyPersonaPresetProvider
        // (Koin last-definition-wins). Persona differentiation for the Plugin Registry.
        single<PersonaPresetProvider> { StubPersonaPresetProvider() }
    }
