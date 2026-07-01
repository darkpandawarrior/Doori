package com.mileway.stub.di

import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import org.koin.dsl.module

val stubModule =
    module {
        single { DemoConfigManager() }
        single<ConfigProvider> { get<DemoConfigManager>() }
        single<MilewayNetworkApi> { FakeTrackingNetworkApi() }
    }
