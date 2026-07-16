package com.mileway.stub.di

import com.mileway.core.data.network.DataStoreBaseUrlProvider
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.api.impl.KtorMilewayNetworkApi
import com.mileway.core.network.auth.AuthApi
import com.mileway.core.network.auth.AuthTokenStore
import com.mileway.core.network.auth.withBearerAuth
import com.mileway.core.network.config.ConfigProvider
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.StubPersonaPresetProvider
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.settings.SecureSettingsFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// NetworkBackendFlags moved to commonMain (stub/commonMain/di/NetworkBackendFlags.kt) so the iOS
// stubModule counterpart gates on the same flag/object instead of duplicating it per platform.

val stubModule =
    module {
        single { DemoConfigManager() }
        single<ConfigProvider> { get<DemoConfigManager>() }
        single { DataStoreBaseUrlProvider(androidContext()) }
        // PLAN_V34 P2/A6: refresh token in the toolkit's encrypted Settings (EncryptedSharedPreferences
        // on Android); AuthApi is the login()/refresh()/logout() seam built on a bare (non-bearer) client.
        single { AuthTokenStore(SecureSettingsFactory(androidContext()).create()) }
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
