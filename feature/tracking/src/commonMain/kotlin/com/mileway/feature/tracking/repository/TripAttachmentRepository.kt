package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.model.OdometerAnalysisSnapshot
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.display.OdometerReadingSource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class TripAttachmentRepository(private val dao: TripAttachmentDao) {
    /** Observe all attachments for a trip, ordered by capture time. */
    fun attachmentsForTrack(trackToken: String): Flow<List<TripAttachmentEntity>> = dao.observeForTrack(trackToken)

    /** Observe only receipt photos for a trip. */
    fun receiptsForTrack(trackToken: String): Flow<List<TripAttachmentEntity>> = dao.observeByType(trackToken, AttachmentType.RECEIPT)

    /** Save a receipt photo for the given trip. Returns the new row id. */
    suspend fun addReceipt(
        trackToken: String,
        uri: String,
    ): Long =
        dao.insert(
            TripAttachmentEntity(
                trackToken = trackToken,
                type = AttachmentType.RECEIPT,
                uri = uri,
                fileName = fileNameFromUri(uri),
                createdAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )

    /** Save (or overwrite) an odometer start proof. Returns the new row id. */
    suspend fun setOdometerStart(
        trackToken: String,
        uri: String,
        ocrText: String?,
        ocrConfidence: Float = 0f,
        ocrVerified: Boolean = false,
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.insert(
            TripAttachmentEntity(
                trackToken = trackToken,
                type = AttachmentType.ODOMETER_START,
                uri = uri,
                ocrText = ocrText,
                fileName = fileNameFromUri(uri),
                ocrConfidence = ocrConfidence,
                ocrVerified = ocrVerified,
                createdAt = now,
                odometerAnalysisJson = analysisJsonFor(ocrText, now),
            ),
        )
    }

    /** Save (or overwrite) an odometer end proof. Returns the new row id. */
    suspend fun setOdometerEnd(
        trackToken: String,
        uri: String,
        ocrText: String?,
        ocrConfidence: Float = 0f,
        ocrVerified: Boolean = false,
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.insert(
            TripAttachmentEntity(
                trackToken = trackToken,
                type = AttachmentType.ODOMETER_END,
                uri = uri,
                ocrText = ocrText,
                fileName = fileNameFromUri(uri),
                ocrConfidence = ocrConfidence,
                ocrVerified = ocrVerified,
                createdAt = now,
                odometerAnalysisJson = analysisJsonFor(ocrText, now),
            ),
        )
    }

    /**
     * §2.4: builds the typed [OdometerAnalysisSnapshot] JSON for an odometer capture from what
     * this single-reading insert has available — [ocrText] parsed to an [Int] reading, source is
     * always [OdometerReadingSource.DEVICE_OCR] since a parseable [ocrText] means OCR produced it.
     * Null when there's no parseable reading (nothing to analyze).
     * ponytail: this is a per-attachment snapshot, not the paired start→end trip validation — that
     * still happens where both readings are compared (LogMilesUiState.odometerValidationError).
     */
    private fun analysisJsonFor(
        ocrText: String?,
        analyzedAtMs: Long,
    ): String? {
        val reading = ocrText?.toIntOrNull() ?: return null
        val snapshot = OdometerAnalysisSnapshot.fromReading(reading, OdometerReadingSource.DEVICE_OCR, analyzedAtMs)
        return OdometerAnalysisSnapshot.encode(snapshot)
    }

    /** Latest odometer-start attachment, or null if none captured. */
    suspend fun getOdometerStart(trackToken: String): TripAttachmentEntity? = dao.getLatestOfType(trackToken, AttachmentType.ODOMETER_START)

    /** Latest odometer-end attachment, or null if none captured. */
    suspend fun getOdometerEnd(trackToken: String): TripAttachmentEntity? = dao.getLatestOfType(trackToken, AttachmentType.ODOMETER_END)

    /** All attachments for the track as a one-shot snapshot. */
    suspend fun getAll(trackToken: String): List<TripAttachmentEntity> = dao.getForTrack(trackToken)

    /** Remove a single attachment by its row id. */
    suspend fun remove(id: Long) = dao.delete(id)

    /** Remove all attachments for a trip (e.g. on discard). */
    suspend fun removeAll(trackToken: String) = dao.deleteForTrack(trackToken)

    /** D.5: last path segment of the URI as a display file name (null when the URI has none). */
    private fun fileNameFromUri(uri: String): String? = uri.substringAfterLast('/').takeIf { it.isNotBlank() }
}
