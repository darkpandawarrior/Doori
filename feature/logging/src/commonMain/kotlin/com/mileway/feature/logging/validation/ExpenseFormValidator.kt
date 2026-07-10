package com.mileway.feature.logging.validation

import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.feature.logging.model.ExpenseCategoryDef
import com.mileway.feature.logging.viewmodel.ExpenseFormState

/**
 * Pure, offline field-level validator for the expense entry form. Replaces the previous
 * button-gate-only check (`amountText.isNotBlank() && merchantName.isNotBlank()`) with a real
 * field-keyed error map so the screen can render an inline [androidx.compose.material3
 * .OutlinedTextField] error/supportingText per field instead of just disabling Submit.
 *
 * No Android/Compose imports here — stays commonMain-only so it can also back a Wear OS or iOS
 * entry flow later without change.
 */
object ExpenseFormValidator {
    /** Stable keys into the returned error map, also usable by the UI to look up a field's error. */
    const val FIELD_MERCHANT_NAME = "merchantName"
    const val FIELD_AMOUNT = "amount"
    const val FIELD_CATEGORY = "category"
    const val FIELD_OFFICE_CODE = "officeCode"

    /**
     * Validates [form] against [catalogDef] (the category's required-field set from
     * [com.mileway.feature.logging.catalog.ExpenseCategoryCatalog], or null when no category is
     * selected yet). Returns an empty map when the form is valid.
     *
     * Category-conditional receipt/GST checks are wired here as soon as those fields land on
     * [ExpenseFormState] (P1.4 receipt attachment, P1.11 GST) — until then, [catalogDef] is
     * accepted so callers don't need to change their call-site again once those fields exist.
     */
    fun validate(
        form: ExpenseFormState,
        catalogDef: ExpenseCategoryDef?,
    ): Map<String, UiText> {
        val errors = mutableMapOf<String, UiText>()

        if (form.category == null) {
            errors[FIELD_CATEGORY] = UiText.of("Select a category")
        }

        if (form.merchantName.isBlank()) {
            errors[FIELD_MERCHANT_NAME] = UiText.of("Enter a merchant or vendor name")
        }

        val amount = form.amountText.toDoubleOrNull()
        val cardCeiling = (form.sourceContext as? ExpenseSourceContext.Card)?.transactionAmountRupees
        if (amount == null || amount <= 0.0) {
            errors[FIELD_AMOUNT] = UiText.of("Enter an amount greater than 0")
        } else if (cardCeiling != null && amount > cardCeiling) {
            // P27.E.4: DiCE's card-transaction ceiling — a claim can't exceed what the card actually
            // charged. Capped via validation (rejected on submit) rather than blocking keystrokes,
            // so the field stays a normal editable text input.
            errors[FIELD_AMOUNT] = UiText.of("Amount can't exceed the card transaction amount")
        }

        // P1.7: cost-center-gated categories (TRAVEL, ACCOMMODATION, OFFICE_SUPPLIES) require a
        // project/office selection before submit; other categories never show or require it.
        if (catalogDef?.requiresCostCenter == true && form.officeCode.isNullOrBlank()) {
            errors[FIELD_OFFICE_CODE] = UiText.of("Select a project/cost center")
        }

        return errors
    }

    /**
     * P27.E.4: DiCE's `editableTypesForCard` — when an expense is opened against a card transaction,
     * merchant and category are pinned to the actual transaction and rendered read-only once
     * E-STRUCT wires this into the screen; amount stays a normal editable field, capped (not
     * blocked) by [validate] instead.
     */
    fun lockedFieldKeys(sourceContext: ExpenseSourceContext): Set<String> =
        if (sourceContext is ExpenseSourceContext.Card) {
            setOf(FIELD_MERCHANT_NAME, FIELD_CATEGORY)
        } else {
            emptySet()
        }
}
