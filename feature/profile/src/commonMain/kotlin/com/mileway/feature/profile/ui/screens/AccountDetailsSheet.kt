package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.network.model.DemoAccount
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_accounts_close
import com.mileway.core.ui.resources.profile_accounts_created
import com.mileway.core.ui.resources.profile_accounts_employee_code
import com.mileway.core.ui.resources.profile_accounts_last_login
import com.mileway.core.ui.resources.profile_accounts_organization
import com.mileway.core.ui.resources.profile_sessions_status_active
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatTimestamp(ms: Long): String =
    if (ms <= 0L) {
        "—"
    } else {
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
            "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
        }
    }

/**
 * P1.3: read-only persona details, opened from [ProfileAction.ViewAccountDetails][com.mileway.feature.profile.viewmodel.ProfileAction.ViewAccountDetails].
 * Mileway's own bottom-sheet design language (rounded avatar, labelled data rows, single close
 * button) — not a port of the reference app's `AccountCard` gradient-ring UI, matching the
 * [ActionConfirmationBottomSheet][com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet]
 * idiom already used by [DelegationScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    account: DemoAccount,
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
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (account.isActive) {
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                Surface(
                    shape = DesignTokens.Shape.chip,
                    color = DesignTokens.StatusColors.success.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = stringResource(Res.string.profile_sessions_status_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = DesignTokens.StatusColors.success,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Surface(
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    DetailRow(label = stringResource(Res.string.profile_accounts_employee_code), value = account.employeeCode)
                    DetailRow(label = stringResource(Res.string.profile_accounts_organization), value = account.organization.ifBlank { "—" })
                    DetailRow(label = stringResource(Res.string.profile_accounts_last_login), value = formatTimestamp(account.lastLoginAtMs))
                    DetailRow(label = stringResource(Res.string.profile_accounts_created), value = formatTimestamp(account.createdAtMs))
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(Res.string.profile_accounts_close), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
