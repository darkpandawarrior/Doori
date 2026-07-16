package com.mileway.feature.advances.validation

/** Every rule the QR-request form checks, so callers can highlight fields precisely. */
enum class QrRequestError {
    AMOUNT_INVALID,
    TITLE_BLANK,
    DESCRIPTION_BLANK,
    DESCRIPTION_TOO_LONG,
    TYPE_REQUIRED,
    CARD_SELECTION_REQUIRED,
    DECLARATION_NOT_ACCEPTED,
}

private const val DESCRIPTION_MAX_LEN = 300

/** PLAN_V35.P3/P4: pure validation for QrRequestFormScreen — no I/O, fully unit-testable. */
object QrRequestValidator {
    fun validate(
        amount: Double,
        title: String,
        description: String,
        type: String?,
        typeEnabled: Boolean,
        cardSelected: Boolean,
        mandatoryCardSelection: Boolean,
        cardsExist: Boolean,
        declarationAccepted: Boolean,
    ): List<QrRequestError> =
        buildList {
            if (amount <= 0.0) add(QrRequestError.AMOUNT_INVALID)
            if (title.isBlank()) add(QrRequestError.TITLE_BLANK)
            if (description.isBlank()) {
                add(QrRequestError.DESCRIPTION_BLANK)
            } else if (description.length > DESCRIPTION_MAX_LEN) {
                add(QrRequestError.DESCRIPTION_TOO_LONG)
            }
            if (typeEnabled && type.isNullOrBlank()) add(QrRequestError.TYPE_REQUIRED)
            if (mandatoryCardSelection && cardsExist && !cardSelected) add(QrRequestError.CARD_SELECTION_REQUIRED)
            if (!declarationAccepted) add(QrRequestError.DECLARATION_NOT_ACCEPTED)
        }
}
