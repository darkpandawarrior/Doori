package com.mileway.core.network

import dev.tmapps.konnection.Konnection
import kotlinx.coroutines.flow.Flow

object NetworkMonitor {
    val isConnectedFlow: Flow<Boolean>
        get() = Konnection.instance.observeHasConnection()

    suspend fun isConnected(): Boolean = Konnection.instance.isConnected()
}
