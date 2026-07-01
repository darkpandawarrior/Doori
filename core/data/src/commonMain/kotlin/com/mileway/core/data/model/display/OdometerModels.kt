package com.mileway.core.data.model.display

enum class OdometerPurpose { START, END }

/**
 * D.3: provenance of an odometer reading — where the value actually came from. Richer than the old
 * `isManual` boolean so the UI can distinguish an OCR'd reading from a typed one from an agent-produced one.
 */
enum class OdometerReadingSource {
    /** Read by on-device OCR from a captured photo. */
    DEVICE_OCR,

    /** Typed by the user. */
    MANUAL,

    /** Produced by the (stubbed) agent / auto-capture path. */
    AGENT_STUB,
}

data class OdometerCaptureResult(
    val purpose: OdometerPurpose,
    val imageUri: String,
    val reading: Int,
    val source: OdometerReadingSource,
    val captureTimeMs: Long,
) {
    /** Back-compat: a reading is "manual" iff the user typed it. */
    val isManual: Boolean get() = source == OdometerReadingSource.MANUAL
}
