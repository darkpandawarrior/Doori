package com.mileway.debug

import android.content.Context
import androidx.compose.runtime.Composable
import com.mileway.core.network.api.NetworkBackendFlags

/** Release and staging builds always select the offline backend and expose no toggle. */
object DebugBackendToggle {
    fun readEnabled(context: Context): Boolean = false

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) = Unit

    fun applyBeforeKoin(context: Context) {
        NetworkBackendFlags.useRealBackend = readEnabled(context)
    }

    @Composable
    fun Entry() = Unit
}
