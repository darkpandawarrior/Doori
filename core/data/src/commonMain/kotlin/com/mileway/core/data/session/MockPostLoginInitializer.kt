package com.mileway.core.data.session

import com.mileway.core.data.dao.MockAccountDao
import kotlinx.coroutines.flow.first

/**
 * PLAN_V22 P7.1: today `SessionRepository.signInWithCredentials(email)` writes exactly two
 * DataStore keys (kind, email) — no profile fetch, no tenant/office/theme bootstrap, nothing
 * shaped like a real post-login response. This is the local, no-network stand-in for that:
 * shaped like the reference app's `profile/me` bootstrap response, but synthesized entirely from
 * static demo constants keyed off [MockAccountDao]'s seeded personas (P1.1) — never a network
 * call, per the project's "no backend, ever" rule.
 *
 * If [email] matches a seeded [com.mileway.core.data.model.db.MockAccountEntity] (by
 * [com.mileway.core.data.model.db.MockAccountEntity.employeeCode] derived the same deterministic
 * way [deriveEmployeeCode] does), that persona's `displayName`/`employeeCode`/`organization` seed
 * the profile so the synthesized identity lines up with whichever account is about to become
 * active; otherwise a deterministic fallback derived purely from [email] is used so sign-in never
 * fails just because the email wasn't one of the seeded demo personas.
 */
class MockPostLoginInitializer(
    private val mockAccountDao: MockAccountDao,
) {
    /** The synthesized bootstrap block a real `profile/me` response would have supplied. */
    data class SynthesizedProfile(
        val displayName: String,
        val employeeCode: String,
        val officeName: String,
        val themeColorHex: String,
        val currencySymbol: String,
    )

    /**
     * Synthesizes a [SynthesizedProfile] for a fresh credentials sign-in with [email]. Never
     * throws and never touches the network — worst case (no seeded accounts yet, e.g. a clean
     * install before P1.1's `seedIfEmpty()` has run) it falls back to deriving everything from
     * [email] alone.
     */
    suspend fun synthesizeProfile(email: String): SynthesizedProfile {
        val employeeCode = deriveEmployeeCode(email)
        val matchedAccount = mockAccountDao.observeAll().first().firstOrNull { it.employeeCode == employeeCode }

        return if (matchedAccount != null) {
            SynthesizedProfile(
                displayName = matchedAccount.displayName,
                employeeCode = matchedAccount.employeeCode,
                officeName = defaultOfficeNameFor(matchedAccount.organization),
                themeColorHex = DEFAULT_THEME_COLOR_HEX,
                currencySymbol = DEFAULT_CURRENCY_SYMBOL,
            )
        } else {
            SynthesizedProfile(
                displayName = email.substringBefore('@').ifBlank { "Demo User" },
                employeeCode = employeeCode,
                officeName = DEFAULT_OFFICE_NAME,
                themeColorHex = DEFAULT_THEME_COLOR_HEX,
                currencySymbol = DEFAULT_CURRENCY_SYMBOL,
            )
        }
    }

    private companion object {
        /** Static demo default — no per-tenant theming backend exists to fetch a real one from. */
        const val DEFAULT_THEME_COLOR_HEX = "#F5A623"

        /** Ember-amber default; matches `DesignTokens`' primary accent so an unmatched sign-in isn't jarring. */
        const val DEFAULT_CURRENCY_SYMBOL = "₹"

        const val DEFAULT_OFFICE_NAME = "Demo HQ"

        fun defaultOfficeNameFor(organization: String): String = organization.ifBlank { DEFAULT_OFFICE_NAME }
    }
}
