package com.mileway.feature.advances.validation

import com.mileway.feature.advances.model.AdvanceType

/** Every rule the Ask-Advance (petty) form checks, so callers can highlight fields precisely. */
enum class PettyRequestError {
    TYPE_REQUIRED,
    AMOUNT_INVALID,
    TITLE_BLANK,
    TITLE_TOO_LONG,
    DESCRIPTION_BLANK,
    DESCRIPTION_TOO_SHORT,
    DATE_RANGE_REQUIRED,
    DECLARATION_NOT_ACCEPTED,
}

private const val TITLE_MAX_LEN = 50
private const val DESCRIPTION_MIN_LEN = 10

/** PLAN_V35.P3/P4: pure validation for AskAdvanceFormScreen — no I/O, fully unit-testable. */
object PettyRequestValidator {
    fun validate(
        type: String?,
        types: List<AdvanceType>,
        amount: Double,
        title: String,
        description: String,
        requireDateRange: Boolean,
        dateRangeStartMs: Long?,
        dateRangeEndMs: Long?,
        declarationAccepted: Boolean,
    ): List<PettyRequestError> =
        buildList {
            if (types.isNotEmpty() && type.isNullOrBlank()) add(PettyRequestError.TYPE_REQUIRED)
            if (amount <= 0.0) add(PettyRequestError.AMOUNT_INVALID)
            if (title.isBlank()) {
                add(PettyRequestError.TITLE_BLANK)
            } else if (title.length > TITLE_MAX_LEN) {
                add(PettyRequestError.TITLE_TOO_LONG)
            }
            if (description.isBlank()) {
                add(PettyRequestError.DESCRIPTION_BLANK)
            } else if (description.length < DESCRIPTION_MIN_LEN) {
                add(PettyRequestError.DESCRIPTION_TOO_SHORT)
            }
            if (requireDateRange && (dateRangeStartMs == null || dateRangeEndMs == null || dateRangeStartMs > dateRangeEndMs)) {
                add(PettyRequestError.DATE_RANGE_REQUIRED)
            }
            if (!declarationAccepted) add(PettyRequestError.DECLARATION_NOT_ACCEPTED)
        }
}
