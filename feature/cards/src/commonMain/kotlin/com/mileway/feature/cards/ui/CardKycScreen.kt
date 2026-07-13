package com.mileway.feature.cards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.cards_done
import com.mileway.core.ui.resources.cards_kyc_attach_document
import com.mileway.core.ui.resources.cards_kyc_attached
import com.mileway.core.ui.resources.cards_kyc_capture_selfie
import com.mileway.core.ui.resources.cards_kyc_doc_body
import com.mileway.core.ui.resources.cards_kyc_doc_title
import com.mileway.core.ui.resources.cards_kyc_full_name
import com.mileway.core.ui.resources.cards_kyc_id_number
import com.mileway.core.ui.resources.cards_kyc_intro_body
import com.mileway.core.ui.resources.cards_kyc_intro_title
import com.mileway.core.ui.resources.cards_kyc_otp_demo
import com.mileway.core.ui.resources.cards_kyc_otp_error
import com.mileway.core.ui.resources.cards_kyc_otp_label
import com.mileway.core.ui.resources.cards_kyc_otp_sent
import com.mileway.core.ui.resources.cards_kyc_phone
import com.mileway.core.ui.resources.cards_kyc_selfie_body
import com.mileway.core.ui.resources.cards_kyc_selfie_title
import com.mileway.core.ui.resources.cards_kyc_step
import com.mileway.core.ui.resources.cards_kyc_success_body
import com.mileway.core.ui.resources.cards_kyc_success_title
import com.mileway.core.ui.resources.cards_kyc_title
import com.mileway.core.ui.resources.cards_next
import com.mileway.core.ui.resources.cards_submit
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.cards.viewmodel.CardKycAction
import com.mileway.feature.cards.viewmodel.CardKycUiState
import com.mileway.feature.cards.viewmodel.CardKycViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P4.3: the Card-KYC 5-step wizard, rendered on top of [FormSubmissionScaffold] (the
 * project's form scaffold — there is no generic stepper). The bottom Submit button doubles as
 * "Next" until the last step; the top-bar back steps within the wizard until step 0, then exits.
 */
@Composable
fun CardKycScreen(
    // P29.C.1: `completed` distinguishes "finished the wizard" (Success screen's Done button)
    // from "backed out early" — only the former should flip the card's isKycPending back in
    // CardsNavigation/CardDetailViewModel.
    onDone: (completed: Boolean) -> Unit,
    viewModel: CardKycViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.done) {
        KycSuccess(onDone = { onDone(true) })
        return
    }

    FormSubmissionScaffold(
        title = stringResource(Res.string.cards_kyc_title),
        subtitle = stringResource(Res.string.cards_kyc_step, state.step + 1, state.totalSteps),
        onBack = { if (state.step > 0) viewModel.onAction(CardKycAction.Back) else onDone(false) },
        onSubmit = { viewModel.onAction(CardKycAction.Next) },
        submitLabel = stringResource(if (state.isLastStep) Res.string.cards_submit else Res.string.cards_next),
        canSubmit = state.isCurrentStepValid && !state.isProcessing,
        isSubmitting = state.isProcessing,
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            KycStepBody(state, viewModel::onAction)
        }
    }
}

@Composable
private fun KycStepBody(
    state: CardKycUiState,
    onAction: (CardKycAction) -> Unit,
) {
    when (state.step) {
        0 -> {
            StepHeader(stringResource(Res.string.cards_kyc_intro_title), stringResource(Res.string.cards_kyc_intro_body))
        }
        1 -> {
            OutlinedTextField(
                value = state.fullName,
                onValueChange = { onAction(CardKycAction.SetFullName(it)) },
                label = { Text(stringResource(Res.string.cards_kyc_full_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.idNumber,
                onValueChange = { onAction(CardKycAction.SetIdNumber(it)) },
                label = { Text(stringResource(Res.string.cards_kyc_id_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = { onAction(CardKycAction.SetPhone(it)) },
                label = { Text(stringResource(Res.string.cards_kyc_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        2 -> {
            state.otpSentTo?.let { Text(stringResource(Res.string.cards_kyc_otp_sent, it), style = MaterialTheme.typography.bodyMedium) }
            OutlinedTextField(
                value = state.otpCode,
                onValueChange = { onAction(CardKycAction.SetOtp(it)) },
                label = { Text(stringResource(Res.string.cards_kyc_otp_label)) },
                singleLine = true,
                isError = state.otpError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            state.demoCode?.let {
                Text(
                    stringResource(Res.string.cards_kyc_otp_demo, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.otpError) {
                Text(stringResource(Res.string.cards_kyc_otp_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        3 -> {
            StepHeader(stringResource(Res.string.cards_kyc_doc_title), stringResource(Res.string.cards_kyc_doc_body))
            // V26 P26.SITE.4: a real picker for the first time (was a tap-only state flip).
            // CaptureMode.Gallery — this screen is commonMain (Android + iOS both render it), and
            // Gallery is the one mode core:media's launcher actually implements on both platforms
            // today; Camera/Document each error on one platform or the other (see
            // MediaCaptureLauncher.android.kt / .ios.kt).
            val launchDocumentPicker =
                rememberMediaCaptureLauncher(
                    config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Gallery)),
                    onResult = { result ->
                        if (result is MediaCaptureResult.Attachments) {
                            result.items.firstOrNull()?.let { onAction(CardKycAction.AttachDocument(it.uri)) }
                        }
                    },
                )
            AttachRow(
                attached = state.documentAttached,
                label = stringResource(Res.string.cards_kyc_attach_document),
                onAttach = launchDocumentPicker,
            )
        }
        4 -> {
            StepHeader(stringResource(Res.string.cards_kyc_selfie_title), stringResource(Res.string.cards_kyc_selfie_body))
            AttachRow(
                attached = state.selfieAttached,
                label = stringResource(Res.string.cards_kyc_capture_selfie),
                onAttach = { onAction(CardKycAction.AttachSelfie) },
            )
        }
    }
}

@Composable
private fun StepHeader(
    title: String,
    body: String,
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AttachRow(
    attached: Boolean,
    label: String,
    onAttach: () -> Unit,
) {
    if (attached) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
            Text(
                stringResource(Res.string.cards_kyc_attached),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = DesignTokens.Spacing.s),
            )
        }
    } else {
        OutlinedButton(onClick = onAttach, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) { Text(label) }
    }
}

@Composable
private fun KycSuccess(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.padding(bottom = DesignTokens.Spacing.l))
        Text(
            stringResource(Res.string.cards_kyc_success_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.cards_kyc_success_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DesignTokens.Spacing.s, bottom = DesignTokens.Spacing.xl),
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) { Text(stringResource(Res.string.cards_done)) }
    }
}
