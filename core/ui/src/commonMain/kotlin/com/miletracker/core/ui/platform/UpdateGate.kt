package com.miletracker.core.ui.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miletracker.core.platform.UpdateAvailability
import com.miletracker.core.platform.UpdateConfig
import com.miletracker.core.platform.UpdateMode
import com.miletracker.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.miletracker.core.ui.components.sheet.ActionConfirmationToneType

/**
 * UP.4 — shared in-app update gate (commonMain, used by both platforms).
 *
 * On first composition it asks [com.miletracker.core.platform.AppUpdateManager] (from
 * [LocalAppUpdateManager]) whether an update is available, then:
 * - FORCED  → replaces [content] with a full-screen blocking wall ("Update required").
 * - FLEXIBLE → shows [content] with a dismissible dialog over it.
 * - none / disabled → just renders [content].
 *
 * The check degrades to no-op (renders content) when there is no store connection — the manager returns
 * [UpdateAvailability.NotAvailable].
 */
@Composable
fun UpdateGate(
    config: UpdateConfig,
    content: @Composable () -> Unit,
) {
    val updateManager = LocalAppUpdateManager.current
    var availability by remember { mutableStateOf<UpdateAvailability>(UpdateAvailability.NotAvailable) }

    LaunchedEffect(config) {
        if (config.enabled) availability = updateManager.checkForUpdate(config)
    }

    val available = availability as? UpdateAvailability.Available
    if (available != null && available.mode == UpdateMode.FORCED) {
        ForcedUpdateWall(onUpdate = { updateManager.startUpdate(UpdateMode.FORCED) })
        return
    }

    content()

    if (available != null && available.mode == UpdateMode.FLEXIBLE) {
        ActionConfirmationBottomSheet(
            title = "Update available",
            description = "A new version of Mileway is available with the latest improvements.",
            confirmLabel = "Update",
            dismissLabel = "Later",
            icon = Icons.Rounded.SystemUpdate,
            tone = ActionConfirmationToneType.Info,
            onConfirm = {
                updateManager.startUpdate(UpdateMode.FLEXIBLE)
                availability = UpdateAvailability.NotAvailable
            },
            onDismiss = { availability = UpdateAvailability.NotAvailable },
        )
    }
}

@Composable
private fun ForcedUpdateWall(onUpdate: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Update required",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "A newer version of Mileway is required to continue. Please update to keep tracking.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(onClick = onUpdate, modifier = Modifier.padding(top = 32.dp)) {
                Text("Update now")
            }
        }
    }
}
