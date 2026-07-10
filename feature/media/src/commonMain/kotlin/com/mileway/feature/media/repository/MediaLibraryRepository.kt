package com.mileway.feature.media.repository

import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.media.model.UploadState
import com.mileway.feature.media.model.AttachmentItem
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class MediaLibraryRepository(private val dao: MediaLibraryDao) {
    fun observeLibrary(): Flow<List<MediaLibraryEntry>> = dao.observeAll()

    suspend fun save(item: AttachmentItem) {
        val url = (item.uploadState as? UploadState.Done)?.remoteUrl ?: item.uri
        dao.insert(
            MediaLibraryEntry(
                id = item.id,
                uri = url,
                mimeType = item.mimeType,
                label = "Attachment ${item.source.name.lowercase()}",
                source = item.source.name,
                savedAtMs = item.capturedAtMillis,
                hasOcr = item.ocr != null,
            ),
        )
    }

    /** Recoverable — sets [MediaLibraryEntry.isDeleted]/[MediaLibraryEntry.deletedAt] rather than a hard delete. */
    suspend fun softDelete(entry: MediaLibraryEntry) = dao.softDelete(entry.id, Clock.System.now().toEpochMilliseconds())

    suspend fun restore(entry: MediaLibraryEntry) = dao.restore(entry.id)

    suspend fun toggleFavorite(entry: MediaLibraryEntry) = dao.toggleFavorite(entry.id)

    /** Call when an entry is opened full-screen — backs the "recently viewed" sort option. */
    suspend fun touchLastAccessed(entry: MediaLibraryEntry) = dao.touchLastAccessed(entry.id, Clock.System.now().toEpochMilliseconds())
}
