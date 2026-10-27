package com.mileway.core.data.model.validator

import com.mileway.core.data.model.display.OdometerReadingSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OdometerValidatorTest {
    @Test
    fun `valid ascending reading`() {
        val result = OdometerValidator.validate(start = 1000, end = 1050, source = OdometerReadingSource.MANUAL)

        val valid = assertIs<OdometerValidation.Valid>(result)
        assertEquals(50, valid.distance)
        assertEquals(false, valid.rolledOver)
    }

    @Test
    fun `equal readings is an implausible jump of zero distance not a decrement`() {
        // OdometerValidator treats end == start as a (valid, zero-distance) trip;
        // LogMilesUiState layers the "must be greater than start" business rule on top.
        val result = OdometerValidator.validate(start = 1000, end = 1000, source = OdometerReadingSource.MANUAL)

        val valid = assertIs<OdometerValidation.Valid>(result)
        assertEquals(0, valid.distance)
    }

    @Test
    fun `simple decrement is invalid`() {
        val result = OdometerValidator.validate(start = 5000, end = 4000, source = OdometerReadingSource.MANUAL)

        val invalid = assertIs<OdometerValidation.Invalid>(result)
        assertEquals(OdometerError.DECREMENT, invalid.reason)
    }

    @Test
    fun `genuine rollover near max wraps to a small valid distance`() {
        val result = OdometerValidator.validate(start = 999_990, end = 20, source = OdometerReadingSource.MANUAL)

        val valid = assertIs<OdometerValidation.Valid>(result)
        assertEquals(30, valid.distance)
        assertEquals(true, valid.rolledOver)
    }

    @Test
    fun `below bounds reading is invalid`() {
        val result = OdometerValidator.validate(start = -5, end = 100, source = OdometerReadingSource.MANUAL)

        val invalid = assertIs<OdometerValidation.Invalid>(result)
        assertEquals(OdometerError.BELOW_BOUNDS, invalid.reason)
    }

    @Test
    fun `above bounds reading is invalid`() {
        val result =
            OdometerValidator.validate(start = 1000, end = OdometerValidator.MAX_ODOMETER + 1, source = OdometerReadingSource.MANUAL)

        val invalid = assertIs<OdometerValidation.Invalid>(result)
        assertEquals(OdometerError.ABOVE_BOUNDS, invalid.reason)
    }

    @Test
    fun `implausible huge jump is invalid`() {
        val result = OdometerValidator.validate(start = 1000, end = 900_000, source = OdometerReadingSource.MANUAL)

        val invalid = assertIs<OdometerValidation.Invalid>(result)
        assertEquals(OdometerError.IMPLAUSIBLE_JUMP, invalid.reason)
    }

    @Test
    fun `synthetic source is carried through on a valid reading`() {
        val result = OdometerValidator.validate(start = 1000, end = 1050, source = OdometerReadingSource.AGENT_STUB)

        val valid = assertIs<OdometerValidation.Valid>(result)
        assertEquals(true, valid.synthetic)
    }
}
