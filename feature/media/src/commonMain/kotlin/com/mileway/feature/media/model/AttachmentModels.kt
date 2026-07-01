package com.mileway.feature.media.model

/** Where an attachment originated. */
enum class AttachmentSource { CAMERA, GALLERY, FILES }

/**
 * Camera flash mode, cycled by the in-camera flash toggle.
 *
 * The order [AUTO] -> [ON] -> [OFF] mirrors the cycle of the on-screen toggle and the
 * underlying CameraX `ImageCapture.FLASH_MODE_*` constants the controller is configured with.
 */
enum class FlashMode { AUTO, ON, OFF }

/**
 * Result of a (mocked) OCR pass over an attachment image.
 *
 * @param rawText            full text the OCR engine "read" from the document
 * @param detectedOdometer   the odometer reading parsed out of [rawText], if any
 * @param confidence         OCR confidence in the 0f..1f range
 * @param watermarkApplied   whether a watermark was burned into the image
 */
data class OcrResult(
    val rawText: String,
    val detectedOdometer: String?,
    val confidence: Float,
    val watermarkApplied: Boolean,
    /** D.2: how many enhancement passes ran (1 = legacy single-pass). */
    val passCount: Int = 1,
    /** D.2: true when >=2 multi-pass enhancement variants agreed on [detectedOdometer]. */
    val isVerified: Boolean = false,
)

/** Lifecycle of a (mocked) upload for a single attachment. */
sealed interface UploadState {
    data object Idle : UploadState

    data object Uploading : UploadState

    data class Done(val remoteUrl: String) : UploadState

    data class Failed(val reason: String) : UploadState
}

/**
 * A captured / picked media attachment plus any OCR and upload state attached to it.
 */
data class AttachmentItem(
    val id: String,
    val uri: String,
    val source: AttachmentSource,
    val mimeType: String = "image/jpeg",
    val capturedAtMillis: Long = 0L,
    val ocr: OcrResult? = null,
    val uploadState: UploadState = UploadState.Idle,
)
