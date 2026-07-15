package com.mileway.core.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.siddharth.kmp.network.BaseUrlProvider
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

/**
 * PLAN_V33 A3: iOS counterpart of the Android [DataStoreBaseUrlProvider] — default is the iOS
 * Simulator loopback (`localhost` resolves to the host Mac from the Simulator's network
 * namespace). A real device needs the host's LAN IP set via [setBaseUrl] — no auto-discovery.
 */
class DataStoreBaseUrlProvider : BaseUrlProvider {
    private val key = stringPreferencesKey("server_base_url")
    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "server_base_url.preferences_pb").toPath() },
        )

    override suspend fun baseUrl(): String = store.data.first()[key] ?: DEFAULT_BASE_URL

    suspend fun setBaseUrl(url: String) {
        store.edit { prefs -> prefs[key] = url }
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "http://localhost:8080"
    }
}
