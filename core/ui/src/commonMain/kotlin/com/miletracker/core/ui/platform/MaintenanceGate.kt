package com.miletracker.core.ui.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * CF.5 — blocking maintenance / kill-switch wall, a sibling of the FORCED [UpdateGate] wall. When the
 * remote config kill-switch is on (or the running build is below the minimum supported version), it
 * replaces all app content with an "under maintenance / please update" screen.
 */
@Composable
fun MaintenanceGate(
    underMaintenance: Boolean,
    content: @Composable () -> Unit,
) {
    if (underMaintenance) MaintenanceWall() else content()
}

/** Pure gate predicate (kill-switch OR running build below the minimum supported version). */
fun isUnderMaintenance(
    killSwitchOn: Boolean,
    currentVersionCode: Long,
    minSupportedVersionCode: Long,
): Boolean = killSwitchOn || currentVersionCode < minSupportedVersionCode

@Composable
private fun MaintenanceWall() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Under maintenance",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text =
                    "Mileway is briefly unavailable or this version is no longer supported. " +
                        "Please update or check back soon.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
