package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.CorporateError
import com.mileway.feature.profile.viewmodel.CorporateStep
import com.mileway.feature.profile.viewmodel.CorporateVerificationViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P4.4 — the corporate-email verification sheet: enter a company email (domain-checked
 * against the demo allow-list) → OTP to that email → verified. Dismisses once the session flips to
 * corporate-verified. Gated by corporateVerificationEnabled at the call site.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorporateVerificationSheet(
    onDismiss: () -> Unit,
    viewModel: CorporateVerificationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Only auto-dismiss on a verification that happened while the sheet was open.
    var openedUnverified by remember { mutableStateOf(!state.isVerified) }
    LaunchedEffect(state.isVerified) {
        if (state.isVerified && openedUnverified) onDismiss()
        if (!state.isVerified) openedUnverified = true
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
                text = cv("corporate_verify_title", "Verify corporate email"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            when (state.step) {
                CorporateStep.ENTER_EMAIL -> {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text(cv("corporate_verify_email", "Company email")) },
                        isError = state.error == CorporateError.UNRECOGNISED_DOMAIN,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                    )
                    if (state.error == CorporateError.UNRECOGNISED_DOMAIN) {
                        Text(
                            cv("corporate_verify_err_domain", "Use a recognised company email domain."),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = viewModel::requestOtp,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.roundedMd,
                    ) { Text(cv("corporate_verify_send", "Send code"), fontWeight = FontWeight.SemiBold) }
                }

                CorporateStep.VERIFY -> {
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        label = { Text(cv("corporate_verify_code", "Enter the 6-digit code")) },
                        isError = state.error == CorporateError.WRONG_CODE,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                    )
                    if (state.error == CorporateError.WRONG_CODE) {
                        Text(
                            cv("corporate_verify_err_wrong", "Incorrect code. Please try again."),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.demoCode?.let { code ->
                        TextButton(onClick = { viewModel.onCodeChange(code) }) {
                            Text(cv("corporate_verify_demo", "Demo code: ") + code)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        TextButton(onClick = viewModel::requestOtp) { Text(cv("corporate_verify_resend", "Resend")) }
                        TextButton(onClick = viewModel::reset) { Text(cv("corporate_verify_cancel", "Cancel")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun cv(
    key: String,
    fallback: String,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return stringResource(resource)
}
