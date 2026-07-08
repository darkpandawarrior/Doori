package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.session.PasswordPolicy
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.ChangePasswordError
import com.mileway.feature.profile.viewmodel.ChangePasswordViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P1.5 — the change-password sheet (the reference app change-password bottomsheet shape): current +
 * new + confirm, a strength meter, and validation errors surfaced from [ChangePasswordViewModel].
 * Gated by the `showPasswordSettings` plugin at the call site.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDismiss()
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
                    .padding(bottom = DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                text = pw("profile_change_password_title", "Change password"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = state.current,
                onValueChange = viewModel::onCurrentChange,
                label = { Text(pw("profile_change_password_current", "Current password")) },
                isError = state.error == ChangePasswordError.WRONG_CURRENT,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
            )

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewChange,
                label = { Text(pw("profile_change_password_new", "New password")) },
                isError = state.error == ChangePasswordError.TOO_SHORT,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
            )

            if (state.newPassword.isNotEmpty()) {
                StrengthMeter(state.strength)
            }

            OutlinedTextField(
                value = state.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = { Text(pw("profile_change_password_confirm", "Confirm new password")) },
                isError = state.error == ChangePasswordError.MISMATCH,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
            )

            state.error?.let { error ->
                Text(
                    text =
                        when (error) {
                            ChangePasswordError.WRONG_CURRENT -> pw("profile_change_password_err_current", "Current password is incorrect")
                            ChangePasswordError.TOO_SHORT -> pw("profile_change_password_err_short", "Use at least 8 characters")
                            ChangePasswordError.MISMATCH -> pw("profile_change_password_err_mismatch", "Passwords don't match")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedMd,
            ) {
                Text(pw("profile_change_password_submit", "Update password"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StrengthMeter(strength: PasswordPolicy.Strength) {
    val (fraction, label, color) =
        when (strength) {
            PasswordPolicy.Strength.WEAK -> Triple(0.33f, pw("profile_change_password_strength_weak", "Weak"), DesignTokens.StatusColors.error)
            PasswordPolicy.Strength.FAIR -> Triple(0.66f, pw("profile_change_password_strength_fair", "Fair"), DesignTokens.StatusColors.warning)
            PasswordPolicy.Strength.STRONG -> Triple(1f, pw("profile_change_password_strength_strong", "Strong"), DesignTokens.StatusColors.success)
        }
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Resolve a string by resource-name key, falling back if it isn't defined yet. */
@Composable
private fun pw(
    key: String,
    fallback: String,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return stringResource(resource)
}
