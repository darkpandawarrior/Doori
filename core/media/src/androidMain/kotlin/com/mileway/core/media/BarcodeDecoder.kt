package com.mileway.core.media

/**
 * Decodes the first QR/barcode found in a captured still image.
 *
 * Flavor-specific by design: a Play Services barcode-scanning dependency here (in core:media's
 * shared androidMain) would leak into the noGms/F-Droid release classpath and fail
 * `:app:verifyNoGmsDependencyPrefixes` (FLFD.2) — the exact bug this interface exists to avoid.
 * Bound per flavor in `PlatformServicesKoinEntry.kt`: gms → ML Kit `BarcodeScanning`
 * (`app/src/gms`), noGms → ZXing (`app/src/noGms`, FOSS/Apache-2.0). Resolved via
 * [org.koin.mp.KoinPlatform] in [scanBarcode] — same "no Compose scope to inject from" pattern as
 * `Watermark.android.kt`.
 */
fun interface BarcodeDecoder {
    suspend fun decode(imageBytes: ByteArray): String?
}
