package com.mileway.feature.advances.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QrRequestValidatorTest {
    private fun validate(
        amount: Double = 100.0,
        title: String = "Fuel top-up",
        description: String = "Weekly fleet fuel top-up.",
        type: String? = "Fuel QR",
        typeEnabled: Boolean = true,
        cardSelected: Boolean = true,
        mandatoryCardSelection: Boolean = true,
        cardsExist: Boolean = true,
        declarationAccepted: Boolean = true,
    ) = QrRequestValidator.validate(
        amount, title, description, type, typeEnabled, cardSelected, mandatoryCardSelection, cardsExist, declarationAccepted,
    )

    @Test
    fun `a fully valid request has no errors`() {
        assertEquals(emptyList(), validate())
    }

    @Test
    fun `non-positive amount is invalid`() {
        assertTrue(QrRequestError.AMOUNT_INVALID in validate(amount = 0.0))
        assertTrue(QrRequestError.AMOUNT_INVALID !in validate(amount = 0.01))
    }

    @Test
    fun `blank title is invalid`() {
        assertTrue(QrRequestError.TITLE_BLANK in validate(title = " "))
        assertTrue(QrRequestError.TITLE_BLANK !in validate(title = "ok"))
    }

    @Test
    fun `blank description is flagged, over-length description is a separate error`() {
        assertTrue(QrRequestError.DESCRIPTION_BLANK in validate(description = ""))
        assertTrue(QrRequestError.DESCRIPTION_TOO_LONG in validate(description = "x".repeat(301)))
        assertTrue(validate(description = "x".repeat(300)).none { it == QrRequestError.DESCRIPTION_TOO_LONG })
    }

    @Test
    fun `type required only when typeEnabled`() {
        assertTrue(QrRequestError.TYPE_REQUIRED in validate(type = null, typeEnabled = true))
        assertTrue(QrRequestError.TYPE_REQUIRED !in validate(type = null, typeEnabled = false))
    }

    @Test
    fun `card selection required only when mandatory and cards exist`() {
        assertTrue(
            QrRequestError.CARD_SELECTION_REQUIRED in
                validate(cardSelected = false, mandatoryCardSelection = true, cardsExist = true),
        )
        assertTrue(
            QrRequestError.CARD_SELECTION_REQUIRED !in
                validate(cardSelected = false, mandatoryCardSelection = false, cardsExist = true),
        )
        assertTrue(
            QrRequestError.CARD_SELECTION_REQUIRED !in
                validate(cardSelected = false, mandatoryCardSelection = true, cardsExist = false),
        )
    }

    @Test
    fun `declaration must be accepted`() {
        assertTrue(QrRequestError.DECLARATION_NOT_ACCEPTED in validate(declarationAccepted = false))
        assertTrue(QrRequestError.DECLARATION_NOT_ACCEPTED !in validate(declarationAccepted = true))
    }
}
