package com.mileway.core.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.siddharth.kmp.network.BaseUrlProvider
import kotlinx.coroutines.flow.first

private val Context.serverBaseUrlDataStore by preferencesDataStore(name = "server_base_url")

/**
 * PLAN_V33 A3: DataStore-backed [BaseUrlProvider] for [com.mileway.core.network.api.impl.KtorMilewayNetworkApi].
 * Default is the Android-emulator loopback alias — `10.0.2.2` is the emulator's fixed alias for the
 * host machine's `localhost`, so it reaches a `:server` instance run locally on the dev machine. A
 * real device on the same LAN (or a different emulator host) needs the actual host LAN IP set via
 * [setBaseUrl] — no auto-discovery here, this is a manual dev/debug override.
 */
class DataStoreBaseUrlProvider(private val context: Context) : BaseUrlProvider {
    private val key = stringPreferencesKey("server_base_url")

    override suspend fun baseUrl(): String = context.serverBaseUrlDataStore.data.first()[key] ?: DEFAULT_BASE_URL

    suspend fun setBaseUrl(url: String) {
        context.serverBaseUrlDataStore.edit { prefs -> prefs[key] = url }
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "http://10.0.2.2:8080"
    }
}
