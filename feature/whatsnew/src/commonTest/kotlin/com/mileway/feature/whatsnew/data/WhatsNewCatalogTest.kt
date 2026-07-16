package com.mileway.feature.whatsnew.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PLAN_V36 P1 — catalog invariants: a typo'd/duplicated entry fails CI, not a device. */
class WhatsNewCatalogTest {
    private val entries = WhatsNewCatalog.entries

    @Test
    fun `ids are unique`() {
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
    }

    @Test
    fun `versions are unique and monotonic from 1`() {
        val versions = entries.map { it.version }.sorted()
        assertEquals((1..entries.size).toList(), versions)
    }

    @Test
    fun `title and description are non-blank for every entry`() {
        entries.forEach { entry ->
            assertTrue(entry.title.isNotBlank(), "blank title for ${entry.id}")
            assertTrue(entry.description.isNotBlank(), "blank description for ${entry.id}")
        }
    }

    @Test
    fun `currentVersion is the max entry version`() {
        val repository = BundledWhatsNewRepository(entries)
        assertEquals(entries.maxOf { it.version }, repository.currentVersion)
    }

    @Test
    fun `repository entries are sorted by releasedOn descending`() {
        val repository = BundledWhatsNewRepository(entries)
        val result = repository.entries()
        assertEquals(result.sortedByDescending { it.releasedOn }, result)
        assertEquals(entries.size, result.size)
    }
}
