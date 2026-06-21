package com.miletracker.feature.profile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.security.BiometricGuard
import com.miletracker.core.security.RootDetector
import com.miletracker.core.ui.components.sheet.AppActionSheet
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.MileTrackerTheme
import com.miletracker.feature.profile.ui.previews.LightDarkPreview
import com.miletracker.feature.profile.viewmodel.DemoSettingsAction
import com.miletracker.feature.profile.viewmodel.DemoSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoSettingsScreen(
    onBack: () -> Unit,
    onOpenRootGuard: () -> Unit = {},
    onOpenRootGuardDetected: () -> Unit = {},
    viewModel: DemoSettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var rootDialogResult by remember { mutableStateOf<RootDetector.RootCheckResult?>(null) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Demo Settings",
                subtitle = "Interactive feature toggles",
                depth = DesignTokens.NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1),
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            "Demo Mode Only",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE65100),
                        )
                    },
                    supportingContent = {
                        Text(
                            "These toggles showcase architecture capabilities. They have no effect in production.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFE65100))
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Feature Flags", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            DemoToggle(
                title = "Simulate Root Detection",
                subtitle = "App treats this device as rooted: shows RootGuardScreen on restart",
                checked = settings.simulateRoot,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleSimulateRoot) },
            )

            DemoToggle(
                title = "Simulate Offline Mode",
                subtitle = "NetworkMonitor reports disconnected; data served from Room/DataStore",
                checked = settings.simulateOffline,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleSimulateOffline) },
            )

            DemoToggle(
                title = "Biometric Guard",
                subtitle = "Require biometric auth before viewing Cards and Advance screens",
                checked = settings.biometricGuardEnabled,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleBiometricGuard) },
            )

            DemoToggle(
                title = "GPS Drift Simulation",
                subtitle = "Tracking service adds ±30m noise to coordinates to demo spike detection",
                checked = settings.simulateGpsDrift,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleGpsDrift) },
            )

            DemoToggle(
                title = "Auto-Discard at End of Day",
                subtitle = "WorkManager discards any active journey at 22:00: demo of AutoDiscardWorker",
                checked = settings.autoDiscardEnabled,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleAutoDiscard) },
                warningColor = Color(0xFFFF8F00),
            )

            Spacer(Modifier.height(24.dp))

            Text("Security Tests", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { rootDialogResult = RootDetector.check() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Run Root Detection Check")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenRootGuard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View Root Guard: Clean Device")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenRootGuardDetected,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View Root Guard: Detected (Demo)")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val availability = BiometricGuard.checkAvailability(context)
                        if (availability == BiometricGuard.Availability.Available) {
                            BiometricGuard.showPrompt(
                                activity = activity,
                                title = "Biometric Auth Demo",
                                subtitle = "Verifying your identity",
                                onSuccess = {
                                    Toast.makeText(context, "Biometric auth succeeded", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        } else {
                            Toast.makeText(context, "Biometric: $availability", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "BiometricGuard requires FragmentActivity", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A237E),
                    ),
            ) {
                Text("Test Biometric Prompt")
            }
        }
    }

    rootDialogResult?.let { result ->
        AppActionSheet(
            onDismiss = { rootDialogResult = null },
            title = if (result.isRooted) "Root Signals Detected" else "Device Appears Clean",
        ) {
            if (result.signals.isEmpty()) {
                Text("No root signals found. This device appears to be a standard environment.")
            } else {
                Text("Signals found:")
                result.signals.forEach { signal ->
                    Text("• $signal", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = { rootDialogResult = null }, modifier = Modifier.fillMaxWidth()) { Text("OK") }
        }
    }
}

@Composable
private fun DemoToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    warningColor: Color? = null,
) {
    ListItem(
        headlineContent = {
            Text(title, color = if (warningColor != null && checked) warningColor else Color.Unspecified)
        },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = { onToggle() })
        },
    )
}

@LightDarkPreview
@Composable
private fun DemoTogglePreview() {
    MileTrackerTheme {
        androidx.compose.foundation.layout.Column {
            DemoToggle(
                title = "Biometric Guard",
                subtitle = "Require biometric auth before viewing Cards and Advance screens",
                checked = true,
                onToggle = {},
            )
            DemoToggle(
                title = "Simulate Root Detection",
                subtitle = "App treats this device as rooted: shows RootGuardScreen on restart",
                checked = false,
                onToggle = {},
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun RootDetectionContentPreview() {
    MileTrackerTheme {
        androidx.compose.foundation.layout.Column {
            Text("Signals found:")
            Text("• su binary found at /system/xbin/su", style = MaterialTheme.typography.bodySmall)
            Text("• test-keys build", style = MaterialTheme.typography.bodySmall)
        }
    }
}
