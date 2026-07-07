package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import com.mileway.core.platform.PermissionTier
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_perm_allow
import com.mileway.core.ui.resources.tracking_perm_if_skipped
import com.mileway.core.ui.resources.tracking_perm_optional
import com.mileway.core.ui.resources.tracking_perm_required
import com.mileway.core.ui.resources.tracking_perm_skip
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * Wave-3 tiered permission-onboarding sheet: shows one [PermissionTier]'s rationale + skip-impact copy at a
 * time (driven by `PermissionOnboardingFlow` in the caller), plus an optional OEM battery hint after the
 * background-location tier. Mirrors [SessionRestoreBottomSheet]'s modal-sheet idiom so tracking's dialog
 * language stays consistent — a full-bleed sheet with a lead icon, title, body copy, and stacked actions.
 *
 * @param oemHint device-manufacturer battery guidance from `OemBatteryHints`/`currentDeviceManufacturer`,
 *   shown only when non-null (Android-only signal; always null on iOS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingSheet(
    tier: PermissionTier,
    oemHint: String?,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!tier.required) onSkip() },
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
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            Text(
                text = if (tier.required) stringResource(Res.string.tracking_perm_required) else stringResource(Res.string.tracking_perm_optional),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = tier.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.tracking_perm_if_skipped, tier.skipImpact),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            if (oemHint != null) {
                Spacer(Modifier.height(DesignTokens.Spacing.m))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = DesignTokens.Spacing.xs),
                    )
                    Text(
                        text = oemHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.tracking_perm_allow), fontWeight = FontWeight.Bold)
            }
            if (!tier.required) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = DesignTokens.Shape.button,
                ) {
                    Text(stringResource(Res.string.tracking_perm_skip), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
