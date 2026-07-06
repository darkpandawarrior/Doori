package com.mileway.core.data.model

import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.model.validator.OdometerValidation
import com.mileway.core.data.model.validator.OdometerValidator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * §2.4: a structured, typed snapshot of an odometer reading's validation outcome — stored
 * alongside the raw OCR text on [TripAttachmentEntity.odometerAnalysisJson] instead of leaving
 * that analysis implicit/recomputed. Mirrors the shape of [OdometerValidation] (built by
 * [OdometerValidator]), flattened into one JSON-encodable record.
 */
@Serializable
data class OdometerAnalysisSnapshot(
    val reading: Int,
    val source: OdometerReadingSource,
    val computedDistance: Int?,
    val rolledOver: Boolean,
    val synthetic: Boolean,
    val validationError: String?,
    val analyzedAtMs: Long,
) {
    companion object {
        /** Encodes a snapshot into [TripAttachmentEntity.odometerAnalysisJson]'s stored form. */
        fun encode(snapshot: OdometerAnalysisSnapshot): String = Json.encodeToString(serializer(), snapshot)

        /** Decodes [TripAttachmentEntity.odometerAnalysisJson] back into a snapshot, or null if absent/corrupt. */
        fun decode(json: String?): OdometerAnalysisSnapshot? = json?.let { runCatching { Json.decodeFromString(serializer(), it) }.getOrNull() }

        /**
         * Builds a snapshot for a single [reading] from [source] — run through [OdometerValidator]
         * as a zero-length (start == end) trip, since a single capture has no pairing yet.
         * [computedDistance]/[rolledOver] therefore reflect that single-reading check, not a real
         * start→end trip distance (that's computed separately once both readings exist).
         */
        fun fromReading(
            reading: Int,
            source: OdometerReadingSource,
            analyzedAtMs: Long,
        ): OdometerAnalysisSnapshot =
            when (val result = OdometerValidator.validate(reading, reading, source)) {
                is OdometerValidation.Valid ->
                    OdometerAnalysisSnapshot(
                        reading = reading,
                        source = source,
                        computedDistance = result.distance,
                        rolledOver = result.rolledOver,
                        synthetic = result.synthetic,
                        validationError = null,
                        analyzedAtMs = analyzedAtMs,
                    )
                is OdometerValidation.Invalid ->
                    OdometerAnalysisSnapshot(
                        reading = reading,
                        source = source,
                        computedDistance = null,
                        rolledOver = false,
                        synthetic = source == OdometerReadingSource.AGENT_STUB,
                        validationError = result.reason.name,
                        analyzedAtMs = analyzedAtMs,
                    )
            }
    }
}
