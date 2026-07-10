package com.mileway.feature.media.repository

import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.media.model.UploadState
import com.mileway.feature.media.model.AttachmentItem
import kotlinx.coroutines.flow.Flow

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
            ),
        )
    }

    suspend fun delete(entry: MediaLibraryEntry) = dao.delete(entry)
}
