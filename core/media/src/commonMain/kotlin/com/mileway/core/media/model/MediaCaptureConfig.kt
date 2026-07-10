package com.mileway.core.media.model

/** A source a [MediaCaptureConfig] may allow — camera, gallery, or a specialised capture flow. */
enum class CaptureMode {
    Camera,
    Gallery,
    Files,
    Pdf,
    Document,
    Odometer,
    QRCode,
    Barcode,
    CloudLibrary,
}

/**
 * Configuration for a single media-capture request (V25 P25.A1.1) — the commonMain contract every
 * capture surface (camera, gallery, document scanner, odometer, QR/barcode) is configured with via
 * `rememberMediaCaptureLauncher`. Mileway's own shape; mirrors the DiCE `ComposeMediaViewModel.Config`
 * only structurally — no code or assets are shared.
 */
data class MediaCaptureConfig(
    val allowedModes: Set<CaptureMode> = setOf(CaptureMode.Camera),
    val multiple: Boolean = false,
    val maxCount: Int = 1,
    val title: String? = null,
    val message: String? = null,
    // OCR
    val enableOcr: Boolean = false,
    val ocrMandatory: Boolean = false,
    val ocrFirstMandatory: Boolean = false,
    val ocrDocType: String? = null,
    // Odometer
    val isOdometer: Boolean = false,
    val odometerPurpose: String? = null,
    val selectedVehicleKey: String? = null,
    val allowManualInput: Boolean = true,
    val digitLock: Int? = null,
    // Watermarking
    val watermarkingEnabled: Boolean = false,
    val watermarkingMandatory: Boolean = false,
    val traceId: String? = null,
)
