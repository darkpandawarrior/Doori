package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_switch_cancel
import com.mileway.core.ui.resources.profile_switch_confirm
import com.mileway.core.ui.resources.profile_switch_locked
import com.mileway.core.ui.resources.profile_switch_subtitle
import com.mileway.core.ui.resources.profile_switch_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.SWITCH_ACCOUNT_PIN_LENGTH
import com.mileway.feature.profile.viewmodel.SwitchAccountUiState
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V22 P2.3: 4-digit PIN entry gate shown before committing a persona switch when biometric
 * guard is off (see `ProfileViewModel.SwitchAccount`). Mileway's own bottom-sheet design language
 * (dot indicators + numeric keypad on the Matrix/terminal surface), not a port of the reference
 * app's lock-screen UI — matches [AccountDetailsSheet]'s idiom (rounded icon badge, labelled
 * content, single primary action) rather than inventing a new sheet shape.
 *
 * Stateless against the ViewModel: the caller supplies [state] and raises intents so this
 * composable stays trivially previewable/testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchAccountPinSheet(
    accountLabel: String,
    state: SwitchAccountUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(state.verified) {
        if (state.verified) onDismiss()
    }

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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Text(
                text = stringResource(Res.string.profile_switch_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.profile_switch_subtitle, accountLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinDots(enteredCount = state.enteredDigits.length, isError = state.error != null)

            Spacer(Modifier.height(DesignTokens.Spacing.m))
            val statusText =
                when {
                    state.isLockedOut -> stringResource(Res.string.profile_switch_locked)
                    state.error != null -> state.error
                    else -> " "
                }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.error != null || state.isLockedOut) DesignTokens.StatusColors.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinKeypad(
                enabled = !state.isLockedOut,
                onDigit = onDigit,
                onBackspace = onBackspace,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = onConfirm,
                enabled = !state.isLockedOut && state.enteredDigits.length == SWITCH_ACCOUNT_PIN_LENGTH,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(Res.string.profile_switch_confirm), fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.profile_switch_cancel))
            }
        }
    }
}

@Composable
private fun PinDots(
    enteredCount: Int,
    isError: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        repeat(SWITCH_ACCOUNT_PIN_LENGTH) { index ->
            val filled = index < enteredCount
            val color =
                when {
                    isError -> DesignTokens.StatusColors.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .background(color, CircleShape),
            )
        }
    }
}

private val KeypadRows =
    listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        KeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
                row.forEach { digit -> KeypadKey(label = digit.toString(), enabled = enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
            Spacer(Modifier.size(56.dp))
            KeypadKey(label = "0", enabled = enabled) { onDigit('0') }
            KeypadKey(label = "⌫", enabled = enabled, onClick = onBackspace)
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                .let { base -> if (enabled) base.clickable(onClick = onClick) else base },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
