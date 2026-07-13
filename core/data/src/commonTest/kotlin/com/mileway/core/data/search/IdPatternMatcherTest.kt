package com.mileway.core.data.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** F0.5 / PLAN_V29 P29.S.2: the id-prefix → SearchEntityType detection table. */
class IdPatternMatcherTest {
    @Test
    fun `blank query detects nothing`() {
        assertTrue(IdPatternMatcher.detect("").isEmpty())
        assertTrue(IdPatternMatcher.detect("   ").isEmpty())
    }

    @Test
    fun `free-text query detects nothing`() {
        assertTrue(IdPatternMatcher.detect("client dinner").isEmpty())
    }

    @Test
    fun `known id prefixes detect their type, case-insensitively, mid-type`() {
        assertEquals(setOf(SearchEntityType.TRANSACTION), IdPatternMatcher.detect("exp-12"))
        assertEquals(setOf(SearchEntityType.ADVANCE), IdPatternMatcher.detect("ADV-"))
        assertEquals(setOf(SearchEntityType.INVOICE), IdPatternMatcher.detect("INV-2024"))
        assertEquals(setOf(SearchEntityType.PURCHASE_REQUEST), IdPatternMatcher.detect("PO-2024-001"))
        assertEquals(setOf(SearchEntityType.CARD_TXN), IdPatternMatcher.detect("TXN-005"))
    }

    @Test
    fun `approval ids don't collide with the ADV or ASN prefixes`() {
        assertEquals(setOf(SearchEntityType.APPROVAL), IdPatternMatcher.detect("A001"))
        assertEquals(setOf(SearchEntityType.ADVANCE), IdPatternMatcher.detect("ADV-001"))
        assertEquals(setOf(SearchEntityType.ASN), IdPatternMatcher.detect("ASN-7741"))
    }
}
