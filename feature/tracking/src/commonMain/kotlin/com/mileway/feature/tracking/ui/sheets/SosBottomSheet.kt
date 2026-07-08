package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.platform.LocalShareSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.sos_alerted
import com.mileway.core.ui.resources.sos_contacts_count
import com.mileway.core.ui.resources.sos_disable
import com.mileway.core.ui.resources.sos_no_contacts
import com.mileway.core.ui.resources.sos_notif_body
import com.mileway.core.ui.resources.sos_notif_time
import com.mileway.core.ui.resources.sos_notif_title
import com.mileway.core.ui.resources.sos_send
import com.mileway.core.ui.resources.sos_share_message
import com.mileway.core.ui.resources.sos_share_status
import com.mileway.core.ui.resources.sos_subtitle
import com.mileway.core.ui.resources.sos_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.tracking.viewmodel.SosViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P3.5: the emergency SOS sheet, reachable from `TrackMilesScreen` when
 * `driverEmergencyModeEnabled` is on. "Send SOS alert" logs a Notification Centre entry (the
 * demo's simulated "we notified your contacts") and shows an inline confirmation; "Share ride
 * status" hands a composed message to the platform share sheet. No network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosBottomSheet(
    onDismiss: () -> Unit,
    viewModel: SosViewModel = koinViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val shareSheet = LocalShareSheet.current
    var sent by remember { mutableStateOf(false) }

    val notifTitle = stringResource(Res.string.sos_notif_title)
    val notifBody = stringResource(Res.string.sos_notif_body)
    val notifTime = stringResource(Res.string.sos_notif_time)
    val shareMessage = stringResource(Res.string.sos_share_message)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFB91C1C),
                modifier = Modifier.height(40.dp),
            )
            Text(
                stringResource(Res.string.sos_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.sos_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            when {
                contacts.isEmpty() -> {
                    Text(
                        stringResource(Res.string.sos_no_contacts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                sent -> {
                    Text(
                        stringResource(Res.string.sos_alerted, contacts.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF16A34A),
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = { shareSheet.share(text = shareMessage) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(Res.string.sos_share_status)) }
                }
                else -> {
                    Text(
                        stringResource(Res.string.sos_contacts_count, contacts.size),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            viewModel.logAlert(notifTitle, notifBody, notifTime)
                            sent = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(Res.string.sos_send)) }
                    OutlinedButton(
                        onClick = { shareSheet.share(text = shareMessage) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(Res.string.sos_share_status)) }
                }
            }

            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.sos_disable))
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
        }
    }
}
