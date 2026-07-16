package com.mileway.feature.advances.validation

import com.mileway.feature.advances.model.AdvanceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PettyRequestValidatorTest {
    private val types = listOf(AdvanceType(1L, "Travel Petty Cash", "blue"))

    private fun validate(
        type: String? = "Travel Petty Cash",
        types: List<AdvanceType> = this.types,
        amount: Double = 100.0,
        title: String = "Client visit",
        description: String = "Advance for the Pune client visit trip.",
        requireDateRange: Boolean = false,
        dateRangeStartMs: Long? = null,
        dateRangeEndMs: Long? = null,
        declarationAccepted: Boolean = true,
    ) = PettyRequestValidator.validate(
        type, types, amount, title, description, requireDateRange, dateRangeStartMs, dateRangeEndMs, declarationAccepted,
    )

    @Test
    fun `a fully valid request has no errors`() {
        assertEquals(emptyList(), validate())
    }

    @Test
    fun `missing type is flagged only when types exist`() {
        assertTrue(PettyRequestError.TYPE_REQUIRED in validate(type = null))
        assertTrue(PettyRequestError.TYPE_REQUIRED !in validate(type = null, types = emptyList()))
    }

    @Test
    fun `non-positive amount is invalid`() {
        assertTrue(PettyRequestError.AMOUNT_INVALID in validate(amount = 0.0))
        assertTrue(PettyRequestError.AMOUNT_INVALID in validate(amount = -5.0))
        assertTrue(PettyRequestError.AMOUNT_INVALID !in validate(amount = 0.01))
    }

    @Test
    fun `blank title is flagged, over-length title is a separate error`() {
        assertTrue(PettyRequestError.TITLE_BLANK in validate(title = "  "))
        assertTrue(PettyRequestError.TITLE_TOO_LONG in validate(title = "x".repeat(51)))
        assertTrue(validate(title = "x".repeat(50)).none { it == PettyRequestError.TITLE_TOO_LONG })
    }

    @Test
    fun `blank description is flagged, too-short description is a separate error`() {
        assertTrue(PettyRequestError.DESCRIPTION_BLANK in validate(description = ""))
        assertTrue(PettyRequestError.DESCRIPTION_TOO_SHORT in validate(description = "short"))
        assertTrue(validate(description = "exactly10!").none { it == PettyRequestError.DESCRIPTION_TOO_SHORT })
    }

    @Test
    fun `date range required only when requireDateRange is set, and start must not be after end`() {
        assertTrue(PettyRequestError.DATE_RANGE_REQUIRED !in validate(requireDateRange = false))
        assertTrue(PettyRequestError.DATE_RANGE_REQUIRED in validate(requireDateRange = true, dateRangeStartMs = null, dateRangeEndMs = null))
        assertTrue(
            PettyRequestError.DATE_RANGE_REQUIRED in
                validate(requireDateRange = true, dateRangeStartMs = 200L, dateRangeEndMs = 100L),
        )
        assertTrue(
            PettyRequestError.DATE_RANGE_REQUIRED !in
                validate(requireDateRange = true, dateRangeStartMs = 100L, dateRangeEndMs = 200L),
        )
    }

    @Test
    fun `declaration must be accepted`() {
        assertTrue(PettyRequestError.DECLARATION_NOT_ACCEPTED in validate(declarationAccepted = false))
        assertTrue(PettyRequestError.DECLARATION_NOT_ACCEPTED !in validate(declarationAccepted = true))
    }
}
