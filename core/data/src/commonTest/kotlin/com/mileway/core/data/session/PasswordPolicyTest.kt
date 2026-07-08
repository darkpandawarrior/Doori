package com.mileway.core.data.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PLAN_V24 P1.5 — password validity + strength scoring for the change/forgot flows. */
class PasswordPolicyTest {
    @Test
    fun `min length gate`() {
        assertFalse(PasswordPolicy.isValid("short"))
        assertFalse(PasswordPolicy.isValid("1234567"))
        assertTrue(PasswordPolicy.isValid("12345678"))
    }

    @Test
    fun `strength escalates with length and variety`() {
        // "abc": letters only, short → WEAK. "abcdefgh": 8 letters, no digit/symbol → still WEAK (score 2).
        assertEquals(PasswordPolicy.Strength.WEAK, PasswordPolicy.strength("abc"))
        assertEquals(PasswordPolicy.Strength.WEAK, PasswordPolicy.strength("abcdefgh"))
        // "abcdefg1": >=8 + letter + digit → score 3 → FAIR.
        assertEquals(PasswordPolicy.Strength.FAIR, PasswordPolicy.strength("abcdefg1"))
        // "Abcdefg1!xyz": >=8, >=12, digit, letter, symbol → score 5 → STRONG.
        assertEquals(PasswordPolicy.Strength.STRONG, PasswordPolicy.strength("Abcdefg1!xyz"))
    }

    @Test
    fun `hash is salted by account id and deterministic`() {
        val a = hashPassword("ACC-1", "demo")
        assertEquals(a, hashPassword("ACC-1", "demo"))
        assertTrue(a != hashPassword("ACC-2", "demo"), "different account salt yields a different hash")
        assertTrue(a != hashPassword("ACC-1", "other"))
    }
}
