package com.mileway.feature.media.model

import com.mileway.core.data.library.MediaLibraryEntry

/** Cloud Library grid filter (V26 P26.LIB.1) — applied client-side over the single Room [Flow]. */
enum class MediaLibraryFilter { All, Images, Pdfs, WithOcr, Favorites }

/** Cloud Library grid sort order (V26 P26.LIB.1). */
enum class MediaLibrarySort { NewestFirst, OldestFirst, RecentlyAccessed }

/**
 * Applies [filter] then [sort] to [entries] — pure, unit-tested logic kept out of the ViewModel's
 * `combine()` lambda so it's directly testable. [MediaLibraryDao.observeAll] already excludes
 * soft-deleted rows; this only narrows/orders what's left.
 */
internal fun applyLibraryFilterAndSort(
    entries: List<MediaLibraryEntry>,
    filter: MediaLibraryFilter,
    sort: MediaLibrarySort,
): List<MediaLibraryEntry> {
    val filtered =
        when (filter) {
            MediaLibraryFilter.All -> entries
            MediaLibraryFilter.Images -> entries.filter { it.mimeType.startsWith("image/") }
            MediaLibraryFilter.Pdfs -> entries.filter { it.mimeType == "application/pdf" }
            MediaLibraryFilter.WithOcr -> entries.filter { it.hasOcr }
            MediaLibraryFilter.Favorites -> entries.filter { it.isFavorite }
        }
    return when (sort) {
        MediaLibrarySort.NewestFirst -> filtered.sortedByDescending { it.savedAtMs }
        MediaLibrarySort.OldestFirst -> filtered.sortedBy { it.savedAtMs }
        // Never-viewed entries (null lastAccessedAt) sort after any viewed entry.
        MediaLibrarySort.RecentlyAccessed -> filtered.sortedByDescending { it.lastAccessedAt ?: -1L }
    }
}

/**
 * Picker-mode constraints for [CloudLibraryScreen] (V26 P26.LIB.4) — set by a caller that wants
 * the library as a multi-select source (e.g. an expense attachment picker) rather than the default
 * "tap to preview" browse mode. `null` `allowedMimeTypes` allows every mime type.
 *
 * ponytail: this is a feature:media-local config, not `core:media`'s `MediaCaptureConfig` —
 * wiring `CaptureMode.CloudLibrary` through the generic `rememberMediaCaptureLauncher` would need
 * `core:media` to reach into `feature:media`'s nav graph, which the module boundaries (CLAUDE.md
 * "feature modules never depend on each other") don't allow. That's a P-SITE-shaped nav-integration
 * task, not this one; `MediaGraph.kt`'s CLOUD_LIBRARY route is the real integration point today.
 */
data class SelectionConfig(
    val maxCount: Int = 1,
    val allowedMimeTypes: Set<String>? = null,
) {
    fun accepts(entry: MediaLibraryEntry): Boolean = allowedMimeTypes == null || entry.mimeType in allowedMimeTypes
}
