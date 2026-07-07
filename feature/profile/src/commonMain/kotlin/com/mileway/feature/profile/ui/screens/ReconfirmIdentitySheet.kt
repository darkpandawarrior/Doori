package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_reconfirm_body
import com.mileway.core.ui.resources.profile_reconfirm_confirm
import com.mileway.core.ui.resources.profile_reconfirm_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V22 P3.2: a soft, dismissible "Re-confirm it's you" nudge shown from `ProfileScreen` once
 * [com.mileway.core.data.session.isSessionFresh] reports the session as stale — models the
 * reference app's PIN/biometric staleness re-confirm concept as UX-pattern parity only. Mileway
 * has no real secret to protect, so this is never a hard block: dismissing it just resets the
 * staleness window from "now" (see `ProfileViewModel`), rather than requiring a real credential.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconfirmIdentitySheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), DesignTokens.Shape.button),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Text(
                text = stringResource(Res.string.profile_reconfirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.profile_reconfirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Text(stringResource(Res.string.profile_reconfirm_confirm), fontWeight = FontWeight.Bold)
            }
        }
    }
}
