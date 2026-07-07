package com.mileway.feature.profile.ui.screens

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
import com.mileway.core.security.BiometricGuard
import com.mileway.core.security.RootDetector
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_demo_auto_discard_subtitle
import com.mileway.core.ui.resources.profile_demo_auto_discard_title
import com.mileway.core.ui.resources.profile_demo_back
import com.mileway.core.ui.resources.profile_demo_biometric_guard_subtitle
import com.mileway.core.ui.resources.profile_demo_biometric_guard_title
import com.mileway.core.ui.resources.profile_demo_device_clean
import com.mileway.core.ui.resources.profile_demo_feature_flags
import com.mileway.core.ui.resources.profile_demo_gps_drift_subtitle
import com.mileway.core.ui.resources.profile_demo_gps_drift_title
import com.mileway.core.ui.resources.profile_demo_mode_only
import com.mileway.core.ui.resources.profile_demo_mode_only_desc
import com.mileway.core.ui.resources.profile_demo_no_root_signals
import com.mileway.core.ui.resources.profile_demo_ok
import com.mileway.core.ui.resources.profile_demo_root_signals_detected
import com.mileway.core.ui.resources.profile_demo_run_root_check
import com.mileway.core.ui.resources.profile_demo_security_tests
import com.mileway.core.ui.resources.profile_demo_signal_item
import com.mileway.core.ui.resources.profile_demo_signals_found
import com.mileway.core.ui.resources.profile_demo_simulate_offline_subtitle
import com.mileway.core.ui.resources.profile_demo_simulate_offline_title
import com.mileway.core.ui.resources.profile_demo_simulate_root_subtitle
import com.mileway.core.ui.resources.profile_demo_simulate_root_title
import com.mileway.core.ui.resources.profile_demo_subtitle
import com.mileway.core.ui.resources.profile_demo_test_biometric
import com.mileway.core.ui.resources.profile_demo_title
import com.mileway.core.ui.resources.profile_demo_view_root_clean
import com.mileway.core.ui.resources.profile_demo_view_root_detected
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.ui.previews.LightDarkPreview
import com.mileway.feature.profile.viewmodel.DemoSettingsAction
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import org.jetbrains.compose.resources.stringResource
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
                title = stringResource(Res.string.profile_demo_title),
                subtitle = stringResource(Res.string.profile_demo_subtitle),
                depth = DesignTokens.NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.BugReport,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_demo_back))
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
                            stringResource(Res.string.profile_demo_mode_only),
                            style = MaterialTheme.typography.titleSmall,
                            color = MilewayColors.warning,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(Res.string.profile_demo_mode_only_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MilewayColors.warning)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.profile_demo_feature_flags), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            DemoToggle(
                title = stringResource(Res.string.profile_demo_simulate_root_title),
                subtitle = stringResource(Res.string.profile_demo_simulate_root_subtitle),
                checked = settings.simulateRoot,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleSimulateRoot) },
            )

            DemoToggle(
                title = stringResource(Res.string.profile_demo_simulate_offline_title),
                subtitle = stringResource(Res.string.profile_demo_simulate_offline_subtitle),
                checked = settings.simulateOffline,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleSimulateOffline) },
            )

            DemoToggle(
                title = stringResource(Res.string.profile_demo_biometric_guard_title),
                subtitle = stringResource(Res.string.profile_demo_biometric_guard_subtitle),
                checked = settings.biometricGuardEnabled,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleBiometricGuard) },
            )

            DemoToggle(
                title = stringResource(Res.string.profile_demo_gps_drift_title),
                subtitle = stringResource(Res.string.profile_demo_gps_drift_subtitle),
                checked = settings.simulateGpsDrift,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleGpsDrift) },
            )

            DemoToggle(
                title = stringResource(Res.string.profile_demo_auto_discard_title),
                subtitle = stringResource(Res.string.profile_demo_auto_discard_subtitle),
                checked = settings.autoDiscardEnabled,
                onToggle = { viewModel.onAction(DemoSettingsAction.ToggleAutoDiscard) },
                warningColor = MilewayColors.warning,
            )

            Spacer(Modifier.height(24.dp))

            Text(stringResource(Res.string.profile_demo_security_tests), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = { rootDialogResult = RootDetector.check() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.profile_demo_run_root_check))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = onOpenRootGuard,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.profile_demo_view_root_clean))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = onOpenRootGuardDetected,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.profile_demo_view_root_detected))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                shape = DesignTokens.Shape.button,
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
                Text(stringResource(Res.string.profile_demo_test_biometric))
            }
        }
    }

    rootDialogResult?.let { result ->
        AppActionSheet(
            onDismiss = { rootDialogResult = null },
            title =
                if (result.isRooted) {
                    stringResource(
                        Res.string.profile_demo_root_signals_detected,
                    )
                } else {
                    stringResource(Res.string.profile_demo_device_clean)
                },
        ) {
            if (result.signals.isEmpty()) {
                Text(stringResource(Res.string.profile_demo_no_root_signals))
            } else {
                Text(stringResource(Res.string.profile_demo_signals_found))
                result.signals.forEach { signal ->
                    Text(stringResource(Res.string.profile_demo_signal_item, signal), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = { rootDialogResult = null },
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.profile_demo_ok))
            }
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
    PreviewSurface {
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
    PreviewSurface {
        androidx.compose.foundation.layout.Column {
            Text("Signals found:")
            Text("• su binary found at /system/xbin/su", style = MaterialTheme.typography.bodySmall)
            Text("• test-keys build", style = MaterialTheme.typography.bodySmall)
        }
    }
}
