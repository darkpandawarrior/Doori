package com.mileway

import android.content.Context
import com.mileway.stub.FakeTrackingNetworkApi
import com.mileway.stub.di.stubModule
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that Koin stub module wires together without definition errors.
 */
class KoinModulesTest : KoinTest {

    @Before
    fun setUp() {
        try { stopKoin() } catch (_: Exception) {}
        startKoin {
            androidContext(mockk<Context>(relaxed = true))
            modules(stubModule)
        }
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (_: Exception) {}
    }

    @Test
    fun `stub module provides ConfigProvider`() {
        val configProvider by inject<com.mileway.core.network.config.ConfigProvider>()
        assertNotNull(configProvider)
        assertTrue(configProvider.isMilesEnabled(), "Miles should be enabled in demo")
        assertTrue(configProvider.isLogMilesEnabled(), "Log Miles should be enabled in demo")
    }

    @Test
    fun `stub module provides MilewayNetworkApi as FakeTrackingNetworkApi`() {
        val networkApi by inject<com.mileway.core.network.api.MilewayNetworkApi>()
        assertNotNull(networkApi)
        assertTrue(networkApi is FakeTrackingNetworkApi, "API should be fake implementation")
    }

    @Test
    fun `stub module DemoConfigManager is the same instance as ConfigProvider`() {
        val configProvider by inject<com.mileway.core.network.config.ConfigProvider>()
        val demoConfig by inject<com.mileway.stub.DemoConfigManager>()
        assertTrue(configProvider === demoConfig, "ConfigProvider should be same instance as DemoConfigManager")
    }
}
