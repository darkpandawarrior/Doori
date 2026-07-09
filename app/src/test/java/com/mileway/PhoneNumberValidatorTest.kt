package com.mileway

import com.mileway.ui.auth.PhoneNumberValidator
import com.mileway.ui.auth.PhoneValidation
import org.junit.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P1.1: phone-login validation rules ported from the reference app's phone login — digit-only
 * normalization, leading-zero strip, 10-digit requirement, and the full-number assembly used as
 * the OTP target.
 */
class PhoneNumberValidatorTest {
    @Test
    fun `strips a leading zero and non-digits`() {
        assertEquals("9876543210", PhoneNumberValidator.normalize("0 98765-43210"))
        assertEquals("9876543210", PhoneNumberValidator.normalize("(987) 654 3210"))
    }

    @Test
    fun `valid ten-digit number normalizes`() {
        assertEquals(PhoneValidation.Valid("9876543210"), PhoneNumberValidator.validate("09876543210"))
    }

    @Test
    fun `empty input is Empty`() {
        assertEquals(PhoneValidation.Empty, PhoneNumberValidator.validate("   "))
        assertEquals(PhoneValidation.Empty, PhoneNumberValidator.validate("abc"))
    }

    @Test
    fun `too short or too long is WrongLength`() {
        assertEquals(PhoneValidation.WrongLength("12345"), PhoneNumberValidator.validate("12345"))
        assertEquals(PhoneValidation.WrongLength("123456789012"), PhoneNumberValidator.validate("123456789012"))
    }

    @Test
    fun `full number prefixes the dial code`() {
        assertEquals("+919876543210", PhoneNumberValidator.fullNumber("+91", "9876543210"))
    }
}
