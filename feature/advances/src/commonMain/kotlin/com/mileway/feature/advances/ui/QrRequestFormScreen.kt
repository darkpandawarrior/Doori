package com.mileway.feature.advances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_declaration
import com.mileway.core.ui.resources.advances_error_amount_invalid
import com.mileway.core.ui.resources.advances_error_card_required
import com.mileway.core.ui.resources.advances_error_declaration_required
import com.mileway.core.ui.resources.advances_error_description_blank
import com.mileway.core.ui.resources.advances_error_description_too_long
import com.mileway.core.ui.resources.advances_error_title_blank
import com.mileway.core.ui.resources.advances_error_type_required
import com.mileway.core.ui.resources.advances_field_amount
import com.mileway.core.ui.resources.advances_field_card
import com.mileway.core.ui.resources.advances_field_description
import com.mileway.core.ui.resources.advances_field_title
import com.mileway.core.ui.resources.advances_field_type
import com.mileway.core.ui.resources.advances_qr_request_subtitle
import com.mileway.core.ui.resources.advances_qr_request_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.ui.components.DeclarationCheckbox
import com.mileway.feature.advances.ui.components.LabelledDropdown
import com.mileway.feature.advances.ui.components.LabelledField
import com.mileway.feature.advances.ui.components.RequestSuccessContent
import com.mileway.feature.advances.validation.QrRequestError
import com.mileway.feature.advances.viewmodel.QrRequestAction
import com.mileway.feature.advances.viewmodel.QrRequestUiState
import com.mileway.feature.advances.viewmodel.QrRequestViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V35.P4: QR-card request form. Validated on submit by [QrRequestViewModel]
 * (QrRequestValidator); flips to [RequestSuccessContent] once the mock repository returns a
 * `permissionId > 0`.
 */
@Composable
fun QrRequestFormScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: QrRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isSuccess) {
        RequestSuccessContent(permissionId = state.submittedPermissionId ?: 0L, onDone = onDone)
    } else {
        QrRequestFormContent(state, viewModel::onAction, onBack)
    }
}

@Composable
internal fun QrRequestFormContent(
    state: QrRequestUiState,
    onAction: (QrRequestAction) -> Unit,
    onBack: () -> Unit,
) {
    FormSubmissionScaffold(
        title = stringResource(Res.string.advances_qr_request_title),
        subtitle = stringResource(Res.string.advances_qr_request_subtitle),
        titleIcon = Icons.Filled.QrCode2,
        onBack = onBack,
        onSubmit = { onAction(QrRequestAction.Submit) },
        canSubmit = !state.isSubmitting,
        isSubmitting = state.isSubmitting,
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (state.typeEnabled && state.types.isNotEmpty()) {
                LabelledDropdown(
                    label = stringResource(Res.string.advances_field_type),
                    selected = state.selectedType ?: "",
                    options = state.types.map { it.title },
                    onSelect = { onAction(QrRequestAction.SelectType(it)) },
                    isError = QrRequestError.TYPE_REQUIRED in state.errors,
                    supportingText = errorTextOrNull(state.errors, QrRequestError.TYPE_REQUIRED),
                )
            }
            LabelledField(
                label = stringResource(Res.string.advances_field_amount),
                value = state.amountText,
                onChange = { onAction(QrRequestAction.SetAmount(it)) },
                isError = QrRequestError.AMOUNT_INVALID in state.errors,
                supportingText = errorTextOrNull(state.errors, QrRequestError.AMOUNT_INVALID),
            )
            LabelledField(
                label = stringResource(Res.string.advances_field_title),
                value = state.title,
                onChange = { onAction(QrRequestAction.SetTitle(it)) },
                isError = QrRequestError.TITLE_BLANK in state.errors,
                supportingText = errorTextOrNull(state.errors, QrRequestError.TITLE_BLANK),
            )
            LabelledField(
                label = stringResource(Res.string.advances_field_description),
                value = state.description,
                onChange = { onAction(QrRequestAction.SetDescription(it)) },
                singleLine = false,
                isError = QrRequestError.DESCRIPTION_BLANK in state.errors || QrRequestError.DESCRIPTION_TOO_LONG in state.errors,
                supportingText =
                    errorTextOrNull(state.errors, QrRequestError.DESCRIPTION_BLANK)
                        ?: errorTextOrNull(state.errors, QrRequestError.DESCRIPTION_TOO_LONG),
            )
            if (state.mandatoryCardSelection && state.cards.isNotEmpty()) {
                val selectedCard = state.cards.firstOrNull { it.id == state.selectedCardId }
                LabelledDropdown(
                    label = stringResource(Res.string.advances_field_card),
                    selected = selectedCard?.title ?: "",
                    options = state.cards.map { it.title },
                    onSelect = { title -> state.cards.firstOrNull { it.title == title }?.let { onAction(QrRequestAction.SelectCard(it.id)) } },
                    isError = QrRequestError.CARD_SELECTION_REQUIRED in state.errors,
                    supportingText = errorTextOrNull(state.errors, QrRequestError.CARD_SELECTION_REQUIRED),
                )
            }
            DeclarationCheckbox(
                checked = state.declarationAccepted,
                onCheckedChange = { onAction(QrRequestAction.SetDeclaration(it)) },
                label = stringResource(Res.string.advances_declaration),
            )
            errorTextOrNull(state.errors, QrRequestError.DECLARATION_NOT_ACCEPTED)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun errorTextOrNull(
    errors: List<QrRequestError>,
    target: QrRequestError,
): String? {
    if (target !in errors) return null
    return when (target) {
        QrRequestError.AMOUNT_INVALID -> stringResource(Res.string.advances_error_amount_invalid)
        QrRequestError.TITLE_BLANK -> stringResource(Res.string.advances_error_title_blank)
        QrRequestError.DESCRIPTION_BLANK -> stringResource(Res.string.advances_error_description_blank)
        QrRequestError.DESCRIPTION_TOO_LONG -> stringResource(Res.string.advances_error_description_too_long)
        QrRequestError.TYPE_REQUIRED -> stringResource(Res.string.advances_error_type_required)
        QrRequestError.CARD_SELECTION_REQUIRED -> stringResource(Res.string.advances_error_card_required)
        QrRequestError.DECLARATION_NOT_ACCEPTED -> stringResource(Res.string.advances_error_declaration_required)
    }
}
