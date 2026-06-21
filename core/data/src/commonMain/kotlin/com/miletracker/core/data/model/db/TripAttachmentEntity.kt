package com.miletracker.core.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists a single photo attachment for a trip identified by [trackToken] (= routeId).
 *
 * One row per photo. Receipts can have many rows; odometer start/end are identified by
 * [type] == [AttachmentType.ODOMETER_START] / [AttachmentType.ODOMETER_END] respectively.
 *
 * Stored locally only, no upload or backend URL.
 */
@Entity(
    tableName = "trip_attachments",
    indices = [
        Index(value = ["track_token"]),
        Index(value = ["track_token", "type"]),
    ],
)
data class TripAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Matches [SavedTrack.routeId], the logical foreign key. */
    @ColumnInfo(name = "track_token")
    val trackToken: String,
    /** Classification of this attachment within the trip. */
    val type: AttachmentType,
    /** File URI produced by the camera (content:// or file://). */
    val uri: String,
    /** OCR-detected odometer reading, populated for ODOMETER_* types. */
    @ColumnInfo(name = "ocr_text")
    val ocrText: String? = null,
    /** Epoch milliseconds when the photo was captured. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
)

enum class AttachmentType {
    RECEIPT,
    ODOMETER_START,
    ODOMETER_END,
}
