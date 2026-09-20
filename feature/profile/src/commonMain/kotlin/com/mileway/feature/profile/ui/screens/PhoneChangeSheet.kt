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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.PhoneChangeError
import com.mileway.feature.profile.viewmodel.PhoneChangeStep
import com.mileway.feature.profile.viewmodel.PhoneChangeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P3.1 — the phone-change sheet: enter a new number → OTP re-verify → commit. The current
 * phone is shown; the OTP step reuses the offline demo-code autofill. Gated by phoneChangeEnabled
 * at the call site.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneChangeSheet(
    onDismiss: () -> Unit,
    viewModel: PhoneChangeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) { if (state.done) onDismiss() }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancel()
            onDismiss()
        },
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
                text = pc("phone_change_title", "Change phone number"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (state.currentPhone.isNotBlank()) {
                Text(
                    text = pc("phone_change_current", "Current: ") + state.currentPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (state.step) {
                PhoneChangeStep.ENTER_PHONE -> {
                    OutlinedTextField(
                        value = state.newPhone,
                        onValueChange = viewModel::onNewPhoneChange,
                        label = { Text(pc("phone_change_new", "New phone number")) },
                        isError = state.error == PhoneChangeError.INVALID_PHONE,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                    )
                    if (state.error == PhoneChangeError.INVALID_PHONE) {
                        Text(
                            pc("phone_change_err_invalid", "Enter a valid 10-digit number"),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = viewModel::requestOtp,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = DesignTokens.Shape.roundedMd,
                    ) { Text(pc("phone_change_send_otp", "Send OTP"), fontWeight = FontWeight.SemiBold) }
                }

                PhoneChangeStep.VERIFY -> {
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        label = { Text(pc("phone_change_code", "Enter the 6-digit code")) },
                        isError = state.error != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                    )
                    state.error?.let { error ->
                        Text(
                            text =
                                when (error) {
                                    PhoneChangeError.WRONG_CODE -> pc("phone_change_err_wrong", "Incorrect code")
                                    PhoneChangeError.EXPIRED -> pc("phone_change_err_expired", "Code expired — resend")
                                    PhoneChangeError.INVALID_PHONE -> ""
                                },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.delivery?.let { d ->
                        TextButton(onClick = viewModel::autofillDemoCode) {
                            Text(pc("phone_change_demo_code", "Demo code: ") + d.code)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        TextButton(onClick = viewModel::requestOtp) { Text(pc("phone_change_resend", "Resend")) }
                        TextButton(onClick = viewModel::cancel) { Text(pc("phone_change_cancel", "Cancel")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun pc(
    key: String,
    fallback: String,
): String {
    val resource = Res.allStringResources[key] ?: return fallback
    return stringResource(resource)
}
