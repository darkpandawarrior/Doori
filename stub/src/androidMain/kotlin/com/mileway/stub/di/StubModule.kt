package com.mileway.stub.di

import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.StubPersonaPresetProvider
import org.koin.dsl.module

val stubModule =
    module {
        single { DemoConfigManager() }
        single<ConfigProvider> { get<DemoConfigManager>() }
        single<MilewayNetworkApi> { FakeTrackingNetworkApi() }
        // PLAN_V24 P0.2: the real PRESET layer — overrides core:data's EmptyPersonaPresetProvider
        // (Koin last-definition-wins). Persona differentiation for the Plugin Registry.
        single<PersonaPresetProvider> { StubPersonaPresetProvider() }
    }
