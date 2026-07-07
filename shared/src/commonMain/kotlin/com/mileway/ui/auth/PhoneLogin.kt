package com.mileway.ui.auth

/*
 * PLAN_V24 P1.1 — phone-number login validation + country list, reimplementing the reference app
 * LoginFragment's rules on Mileway's own foundation: strip non-digits, drop a leading zero, and
 * require a 10-digit national number. Pure + platform-agnostic so it unit-tests without a UI.
 *
 * ponytail: the small country list lives here as static UI data rather than in :stub — a picker
 * list isn't backend mock data and shared doesn't depend on :stub; noted in PROGRESS.
 */

/** One dialling-code option for the login country picker. */
data class CountryDialCode(
    val isoCode: String,
    val name: String,
    val dialCode: String,
)

/** Small static list; India (+91) is the default, matching the reference app' default country. */
val LOGIN_COUNTRY_CODES: List<CountryDialCode> =
    listOf(
        CountryDialCode("IN", "India", "+91"),
        CountryDialCode("US", "United States", "+1"),
        CountryDialCode("GB", "United Kingdom", "+44"),
        CountryDialCode("AE", "United Arab Emirates", "+971"),
        CountryDialCode("SG", "Singapore", "+65"),
        CountryDialCode("AU", "Australia", "+61"),
    )

val DEFAULT_COUNTRY_CODE: CountryDialCode = LOGIN_COUNTRY_CODES.first()

sealed interface PhoneValidation {
    /** The normalized 10-digit national number, ready to prefix with a dial code. */
    data class Valid(val nationalNumber: String) : PhoneValidation

    data object Empty : PhoneValidation

    data class WrongLength(val normalized: String) : PhoneValidation
}

object PhoneNumberValidator {
    private const val NATIONAL_LENGTH = 10

    /** Digits only, with a single leading zero stripped (the reference app' auto-strip behaviour). */
    fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.startsWith("0")) digits.trimStart('0') else digits
    }

    fun validate(raw: String): PhoneValidation {
        val normalized = normalize(raw)
        return when {
            normalized.isEmpty() -> PhoneValidation.Empty
            normalized.length != NATIONAL_LENGTH -> PhoneValidation.WrongLength(normalized)
            else -> PhoneValidation.Valid(normalized)
        }
    }

    /** The full E.164-ish target the OTP is sent to, e.g. "+919876543210". */
    fun fullNumber(
        dialCode: String,
        nationalNumber: String,
    ): String = dialCode + nationalNumber
}
