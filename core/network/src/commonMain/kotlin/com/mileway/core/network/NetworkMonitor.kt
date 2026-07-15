package com.mileway.core.network

import dev.tmapps.konnection.Konnection
import kotlinx.coroutines.flow.Flow

object NetworkMonitor {
    val isConnectedFlow: Flow<Boolean>
        get() = Konnection.instance.observeHasConnection()

    suspend fun isConnected(): Boolean = Konnection.instance.isConnected()

    // PLAN_V33 A6: Konnection.isConnected() is itself non-suspend (a cached/synchronous read under
    // the hood) — this exposes that directly for callers needing a plain `() -> Boolean`, like
    // screenStateStream's `isOnline` param, without wrapping a suspend fun.
    fun isConnectedNow(): Boolean = Konnection.instance.isConnected()
}
