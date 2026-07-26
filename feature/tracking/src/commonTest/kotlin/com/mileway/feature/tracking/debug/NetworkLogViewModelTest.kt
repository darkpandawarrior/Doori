package com.mileway.feature.tracking.debug

import com.mileway.core.network.api.NetworkBackendFlags
import com.mileway.core.network.netlog.NetworkLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the dev-only backend switch added to [NetworkLogViewModel]: toggling
 * [NetworkBackendFlags.useRealBackend] and saving a base URL both flow through [NetworkBackendFlags]
 * (the only reachable seam from commonMain — see its doc for why `DataStoreBaseUrlProvider` isn't).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLogViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        NetworkBackendFlags.useRealBackend = false
        NetworkBackendFlags.baseUrlOverride = null
        NetworkBackendFlags.onBaseUrlSubmitted = null
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        NetworkBackendFlags.useRealBackend = false
        NetworkBackendFlags.baseUrlOverride = null
        NetworkBackendFlags.onBaseUrlSubmitted = null
    }

    @Test
    fun `toggling the switch flips NetworkBackendFlags and state together`() =
        runTest {
            val viewModel = NetworkLogViewModel(store = NetworkLogStore(), httpClient = null)

            viewModel.onAction(NetworkLogAction.UseRealBackendChanged(true))

            assertEquals(true, viewModel.state.value.useRealBackend)
            assertEquals(true, NetworkBackendFlags.useRealBackend)
        }

    @Test
    fun `saving a base URL persists it to NetworkBackendFlags and invokes the platform seam`() =
        runTest {
            val viewModel = NetworkLogViewModel(store = NetworkLogStore(), httpClient = null)
            var submitted: String? = null
            NetworkBackendFlags.onBaseUrlSubmitted = { url -> submitted = url }

            viewModel.onAction(NetworkLogAction.BaseUrlChanged("http://192.168.1.10:8080"))
            viewModel.onAction(NetworkLogAction.SaveBaseUrl)

            assertEquals("http://192.168.1.10:8080", NetworkBackendFlags.baseUrlOverride)
            assertEquals("http://192.168.1.10:8080", submitted)
            assertEquals(true, viewModel.state.value.baseUrlSaved)
        }

    @Test
    fun `saving a blank base URL clears the override instead of storing blank`() =
        runTest {
            val viewModel = NetworkLogViewModel(store = NetworkLogStore(), httpClient = null)

            viewModel.onAction(NetworkLogAction.BaseUrlChanged("   "))
            viewModel.onAction(NetworkLogAction.SaveBaseUrl)

            assertNull(NetworkBackendFlags.baseUrlOverride)
        }
}
