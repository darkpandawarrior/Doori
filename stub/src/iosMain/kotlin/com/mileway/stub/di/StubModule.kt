package com.mileway.stub.di

import com.mileway.core.data.network.DataStoreBaseUrlProvider
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.mileway.core.network.auth.AuthApi
import com.mileway.core.network.auth.AuthTokenStore
import com.mileway.core.network.auth.withBearerAuth
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.platform.FeatureFlags
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.StubPersonaPresetProvider
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.settings.SecureSettingsFactory
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
        // V36 review fix: HomeScreen's koinInject<FeatureFlags>() default had no iOS binding at
        // all (only Android's app-level `appModule` constructs it) — would have thrown the moment
        // HomeScreen resolved it. Mirrors that same construction (ConfigProvider.getFeatureFlags()).
        single { FeatureFlags(get<ConfigProvider>().getFeatureFlags()) }
        single { DataStoreBaseUrlProvider() }
        // PLAN_V34 P2/A6: refresh token in the toolkit's encrypted Settings (Keychain on iOS);
        // AuthApi is the login()/refresh()/logout() seam built on a bare (non-bearer) client.
        single { AuthTokenStore(SecureSettingsFactory().create()) }
        single { AuthApi(createHttpClient(), get<DataStoreBaseUrlProvider>(), get()) }
        single<MilewayNetworkApi> {
            if (NetworkBackendFlags.useRealBackend) {
                val tokenStore = get<AuthTokenStore>()
                KtorMilewayNetworkApi(
                    client =
                        createHttpClient(
                            // A real 401 on a non-auth route now means "refresh already failed" (the
                            // withBearerAuth-wrapped client tried refresh+retry first) — clear the
                            // session so the app's own sign-out flow picks it up.
                            onUnauthorized = { tokenStore.clear() },
                        ).withBearerAuth(tokenStore, get()),
                    baseUrlProvider = get<DataStoreBaseUrlProvider>(),
                )
            } else {
                FakeTrackingNetworkApi()
            }
        }
        // PLAN_V24 P0.2: the real PRESET layer — overrides core:data's EmptyPersonaPresetProvider
        // (Koin last-definition-wins). Persona differentiation for the Plugin Registry.
        single<PersonaPresetProvider> { StubPersonaPresetProvider() }
    }
