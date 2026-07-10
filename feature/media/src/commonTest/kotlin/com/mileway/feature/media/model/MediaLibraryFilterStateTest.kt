package com.mileway.feature.media.model

import com.mileway.core.data.library.MediaLibraryEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/** V26 P26.LIB.1: [applyLibraryFilterAndSort] is the pure logic the ViewModel's `combine()` calls. */
class MediaLibraryFilterStateTest {
    private fun entry(
        id: String,
        mimeType: String = "image/jpeg",
        savedAtMs: Long = 0L,
        isFavorite: Boolean = false,
        hasOcr: Boolean = false,
        lastAccessedAt: Long? = null,
    ) = MediaLibraryEntry(
        id = id,
        uri = "file:///$id",
        mimeType = mimeType,
        label = id,
        source = "CAMERA",
        savedAtMs = savedAtMs,
        isFavorite = isFavorite,
        hasOcr = hasOcr,
        lastAccessedAt = lastAccessedAt,
    )

    private val library =
        listOf(
            entry("image", mimeType = "image/jpeg", savedAtMs = 300L, hasOcr = true, lastAccessedAt = 10L),
            entry("pdf", mimeType = "application/pdf", savedAtMs = 200L, isFavorite = true, lastAccessedAt = 30L),
            entry("plain", mimeType = "image/png", savedAtMs = 100L),
        )

    @Test
    fun `All filter keeps every entry`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.All, MediaLibrarySort.NewestFirst)
        assertEquals(listOf("image", "pdf", "plain"), result.map { it.id })
    }

    @Test
    fun `Images filter keeps only image mime types`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.Images, MediaLibrarySort.NewestFirst)
        assertEquals(listOf("image", "plain"), result.map { it.id })
    }

    @Test
    fun `Pdfs filter keeps only application-pdf`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.Pdfs, MediaLibrarySort.NewestFirst)
        assertEquals(listOf("pdf"), result.map { it.id })
    }

    @Test
    fun `WithOcr filter keeps only hasOcr entries`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.WithOcr, MediaLibrarySort.NewestFirst)
        assertEquals(listOf("image"), result.map { it.id })
    }

    @Test
    fun `Favorites filter keeps only isFavorite entries`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.Favorites, MediaLibrarySort.NewestFirst)
        assertEquals(listOf("pdf"), result.map { it.id })
    }

    @Test
    fun `OldestFirst sorts ascending by savedAtMs`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.All, MediaLibrarySort.OldestFirst)
        assertEquals(listOf("plain", "pdf", "image"), result.map { it.id })
    }

    @Test
    fun `RecentlyAccessed sorts never-viewed entries last`() {
        val result = applyLibraryFilterAndSort(library, MediaLibraryFilter.All, MediaLibrarySort.RecentlyAccessed)
        assertEquals(listOf("pdf", "image", "plain"), result.map { it.id })
    }
}
