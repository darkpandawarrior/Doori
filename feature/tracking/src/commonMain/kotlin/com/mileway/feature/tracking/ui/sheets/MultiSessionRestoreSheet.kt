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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_not_now
import com.mileway.core.ui.resources.tracking_action_resume
import com.mileway.core.ui.resources.tracking_multi_different_persona
import com.mileway.core.ui.resources.tracking_multi_draft_journey
import com.mileway.core.ui.resources.tracking_multi_journey_in_progress
import com.mileway.core.ui.resources.tracking_multi_no_data
import com.mileway.core.ui.resources.tracking_multi_ready_resume
import com.mileway.core.ui.resources.tracking_multi_restore_title
import com.mileway.core.ui.resources.tracking_plural_interrupted_journeys
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.tracking.service.RestorableSession
import com.mileway.feature.tracking.service.SessionValidationStatus
import com.mileway.feature.tracking.ui.components.StatusBadge
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
                text = stringResource(Res.string.tracking_multi_restore_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = pluralStringResource(Res.plurals.tracking_plural_interrupted_journeys, sessions.size, sessions.size),
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
            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(Res.string.tracking_action_not_now), fontWeight = FontWeight.Bold)
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
                    text =
                        if (session.isDraft) {
                            stringResource(
                                Res.string.tracking_multi_draft_journey,
                            )
                        } else {
                            stringResource(Res.string.tracking_multi_journey_in_progress)
                        },
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
                        SessionValidationStatus.VALID -> stringResource(Res.string.tracking_multi_ready_resume) to MilewayColors.success
                        SessionValidationStatus.OWNER_MISMATCH -> stringResource(Res.string.tracking_multi_different_persona) to MilewayColors.warning
                        SessionValidationStatus.EMPTY -> stringResource(Res.string.tracking_multi_no_data) to MilewayColors.neutral
                    }
                StatusBadge(text = badgeText, color = badgeColor)
            }
            TextButton(
                shape = DesignTokens.Shape.button,
                onClick = { onResume(session.routeId) },
                enabled = session.status != SessionValidationStatus.EMPTY,
            ) {
                Text(stringResource(Res.string.tracking_action_resume))
            }
        }
    }
}
