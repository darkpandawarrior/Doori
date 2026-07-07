package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_not_now
import com.mileway.core.ui.resources.tracking_stranger_desc
import com.mileway.core.ui.resources.tracking_stranger_resume
import com.mileway.core.ui.resources.tracking_stranger_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.tracking.viewmodel.StrangerSessionConfig
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V22 P3.5: cold-start reconciliation dialog — shown instead of silently restoring a
 * persisted in-progress trip whose `started_by_*` ownership pointer doesn't match the currently
 * active persona. This is the local, offline half of the reference app's
 * `syncWithServerOngoingSession` reconciliation; there is no multi-device server half here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrangerSessionSheet(
    config: StrangerSessionConfig,
    onResume: () -> Unit,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.SwitchAccount,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            Text(
                text = stringResource(Res.string.tracking_stranger_title, config.ownerLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.tracking_stranger_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Text(stringResource(Res.string.tracking_stranger_resume), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Text(stringResource(Res.string.tracking_action_not_now), fontWeight = FontWeight.Bold)
            }
        }
    }
}
