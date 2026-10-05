package com.mileway.debug

import android.content.Context
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mileway.core.network.api.NetworkBackendFlags

/** Debug-only backend choice, applied on the next process start before Koin is initialized. */
object DebugBackendToggle {
    private const val PREFERENCES = "debug_backend"
    private const val USE_REAL_BACKEND = "use_real_backend"

    fun readEnabled(context: Context): Boolean = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(USE_REAL_BACKEND, false)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(USE_REAL_BACKEND, enabled)
            .apply()
    }

    fun applyBeforeKoin(context: Context) {
        NetworkBackendFlags.useRealBackend = readEnabled(context)
    }

    @Composable
    fun Entry() {
        val context = LocalContext.current
        var enabled by remember(context) { mutableStateOf(readEnabled(context)) }
        val activeBackend = if (NetworkBackendFlags.useRealBackend) "Real" else "Offline mock"
        ListItem(
            headlineContent = { Text("Use real backend") },
            supportingContent = { Text("Current backend: $activeBackend. Close the app process and reopen it to apply changes.") },
            trailingContent = {
                Switch(
                    checked = enabled,
                    modifier = Modifier.semantics { contentDescription = "Use real backend" },
                    onCheckedChange = {
                        setEnabled(context, it)
                        enabled = it
                    },
                )
            },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Debug backend toggle")
@Composable
private fun PreviewDebugBackendToggle() {
    com.mileway.core.ui.previews.PreviewSurface {
        DebugBackendToggle.Entry()
    }
}
