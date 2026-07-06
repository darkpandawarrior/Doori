package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.tracking.service.RestorableSession
import com.mileway.feature.tracking.service.SessionValidationStatus
import com.mileway.feature.tracking.ui.components.StatusBadge
import kotlin.math.roundToLong

/**
 * Wave-4 §2.1: local-multi restore list — every [RestorableSession] gathered by
 * [com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel], each with its own
 * validation status, instead of [SessionRestoreBottomSheet]'s single-session flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSessionRestoreSheet(
    sessions: List<RestorableSession>,
    onResume: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.l, top = DesignTokens.Spacing.xs),
        ) {
            Text(
                text = "Restore a journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            val plural = if (sessions.size == 1) "" else "s"
            Text(
                text = "${sessions.size} interrupted journey$plural found on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            LazyColumn {
                items(sessions, key = { it.routeId }) { session ->
                    RestorableSessionRow(session, onResume)
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Not Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RestorableSessionRow(
    session: RestorableSession,
    onResume: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val distText = "${(session.distanceKm * 10).roundToLong() / 10.0} km"
                val durationMin = session.durationMs / 60_000
                Text(
                    text = if (session.isDraft) "Draft journey" else "Journey in progress",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "~$distText · ~${durationMin}m recorded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                val (badgeText, badgeColor) =
                    when (session.status) {
                        SessionValidationStatus.VALID -> "Ready to resume" to MilewayColors.success
                        SessionValidationStatus.OWNER_MISMATCH -> "Different persona" to MilewayColors.warning
                        SessionValidationStatus.EMPTY -> "No data recorded" to MilewayColors.neutral
                    }
                StatusBadge(text = badgeText, color = badgeColor)
            }
            TextButton(
                onClick = { onResume(session.routeId) },
                enabled = session.status != SessionValidationStatus.EMPTY,
            ) {
                Text("Resume")
            }
        }
    }
}
