package com.mileway.feature.advances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_ask_subtitle
import com.mileway.core.ui.resources.advances_ask_title
import com.mileway.core.ui.resources.advances_declaration
import com.mileway.core.ui.resources.advances_error_amount_invalid
import com.mileway.core.ui.resources.advances_error_date_range_required
import com.mileway.core.ui.resources.advances_error_declaration_required
import com.mileway.core.ui.resources.advances_error_description_blank
import com.mileway.core.ui.resources.advances_error_description_too_short
import com.mileway.core.ui.resources.advances_error_title_blank
import com.mileway.core.ui.resources.advances_error_title_too_long
import com.mileway.core.ui.resources.advances_error_type_required
import com.mileway.core.ui.resources.advances_field_amount
import com.mileway.core.ui.resources.advances_field_date_range
import com.mileway.core.ui.resources.advances_field_description
import com.mileway.core.ui.resources.advances_field_title
import com.mileway.core.ui.resources.advances_field_type
import com.mileway.core.ui.resources.advances_pick_end_date
import com.mileway.core.ui.resources.advances_pick_start_date
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.ui.components.DatePickerField
import com.mileway.feature.advances.ui.components.DeclarationCheckbox
import com.mileway.feature.advances.ui.components.LabelledDropdown
import com.mileway.feature.advances.ui.components.LabelledField
import com.mileway.feature.advances.ui.components.RequestSuccessContent
import com.mileway.feature.advances.validation.PettyRequestError
import com.mileway.feature.advances.viewmodel.AskAdvanceAction
import com.mileway.feature.advances.viewmodel.AskAdvanceUiState
import com.mileway.feature.advances.viewmodel.AskAdvanceViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V35.P4: Ask-Advance (petty) request form. Validated on submit by [AskAdvanceViewModel]
 * (PettyRequestValidator); flips to [RequestSuccessContent] once the mock repository returns a
 * `permissionId > 0`.
 */
@Composable
fun AskAdvanceFormScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AskAdvanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isSuccess) {
        RequestSuccessContent(permissionId = state.submittedPermissionId ?: 0L, onDone = onDone)
    } else {
        AskAdvanceFormContent(state, viewModel::onAction, onBack)
    }
}

@Composable
internal fun AskAdvanceFormContent(
    state: AskAdvanceUiState,
    onAction: (AskAdvanceAction) -> Unit,
    onBack: () -> Unit,
) {
    FormSubmissionScaffold(
        title = stringResource(Res.string.advances_ask_title),
        subtitle = stringResource(Res.string.advances_ask_subtitle),
        titleIcon = Icons.Filled.RequestQuote,
        onBack = onBack,
        onSubmit = { onAction(AskAdvanceAction.Submit) },
        canSubmit = !state.isSubmitting,
        isSubmitting = state.isSubmitting,
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (state.types.isNotEmpty()) {
                LabelledDropdown(
                    label = stringResource(Res.string.advances_field_type),
                    selected = state.selectedType ?: "",
                    options = state.types.map { it.title },
                    onSelect = { onAction(AskAdvanceAction.SelectType(it)) },
                    isError = PettyRequestError.TYPE_REQUIRED in state.errors,
                    supportingText = errorTextOrNull(state.errors, PettyRequestError.TYPE_REQUIRED),
                )
            }
            LabelledField(
                label = stringResource(Res.string.advances_field_amount),
                value = state.amountText,
                onChange = { onAction(AskAdvanceAction.SetAmount(it)) },
                isError = PettyRequestError.AMOUNT_INVALID in state.errors,
                supportingText = errorTextOrNull(state.errors, PettyRequestError.AMOUNT_INVALID),
            )
            LabelledField(
                label = stringResource(Res.string.advances_field_title),
                value = state.title,
                onChange = { onAction(AskAdvanceAction.SetTitle(it)) },
                isError = PettyRequestError.TITLE_BLANK in state.errors || PettyRequestError.TITLE_TOO_LONG in state.errors,
                supportingText =
                    errorTextOrNull(state.errors, PettyRequestError.TITLE_BLANK)
                        ?: errorTextOrNull(state.errors, PettyRequestError.TITLE_TOO_LONG),
            )
            LabelledField(
                label = stringResource(Res.string.advances_field_description),
                value = state.description,
                onChange = { onAction(AskAdvanceAction.SetDescription(it)) },
                singleLine = false,
                isError = PettyRequestError.DESCRIPTION_BLANK in state.errors || PettyRequestError.DESCRIPTION_TOO_SHORT in state.errors,
                supportingText =
                    errorTextOrNull(state.errors, PettyRequestError.DESCRIPTION_BLANK)
                        ?: errorTextOrNull(state.errors, PettyRequestError.DESCRIPTION_TOO_SHORT),
            )
            if (state.requireDateRange) {
                DateRangeSection(state, onAction)
            }
            DeclarationCheckbox(
                checked = state.declarationAccepted,
                onCheckedChange = { onAction(AskAdvanceAction.SetDeclaration(it)) },
                label = stringResource(Res.string.advances_declaration),
            )
            errorTextOrNull(state.errors, PettyRequestError.DECLARATION_NOT_ACCEPTED)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DateRangeSection(
    state: AskAdvanceUiState,
    onAction: (AskAdvanceAction) -> Unit,
) {
    Column {
        Text(
            stringResource(Res.string.advances_field_date_range),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            DatePickerField(
                label = stringResource(Res.string.advances_pick_start_date),
                valueMs = state.dateRangeStartMs,
                onPick = { onAction(AskAdvanceAction.SetDateRange(it, state.dateRangeEndMs)) },
                modifier = Modifier.weight(1f),
            )
            DatePickerField(
                label = stringResource(Res.string.advances_pick_end_date),
                valueMs = state.dateRangeEndMs,
                onPick = { onAction(AskAdvanceAction.SetDateRange(state.dateRangeStartMs, it)) },
                modifier = Modifier.weight(1f),
            )
        }
        errorTextOrNull(state.errors, PettyRequestError.DATE_RANGE_REQUIRED)?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun errorTextOrNull(
    errors: List<PettyRequestError>,
    target: PettyRequestError,
): String? {
    if (target !in errors) return null
    return when (target) {
        PettyRequestError.TYPE_REQUIRED -> stringResource(Res.string.advances_error_type_required)
        PettyRequestError.AMOUNT_INVALID -> stringResource(Res.string.advances_error_amount_invalid)
        PettyRequestError.TITLE_BLANK -> stringResource(Res.string.advances_error_title_blank)
        PettyRequestError.TITLE_TOO_LONG -> stringResource(Res.string.advances_error_title_too_long)
        PettyRequestError.DESCRIPTION_BLANK -> stringResource(Res.string.advances_error_description_blank)
        PettyRequestError.DESCRIPTION_TOO_SHORT -> stringResource(Res.string.advances_error_description_too_short)
        PettyRequestError.DATE_RANGE_REQUIRED -> stringResource(Res.string.advances_error_date_range_required)
        PettyRequestError.DECLARATION_NOT_ACCEPTED -> stringResource(Res.string.advances_error_declaration_required)
    }
}
