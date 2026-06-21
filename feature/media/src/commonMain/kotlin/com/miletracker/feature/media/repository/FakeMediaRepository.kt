package com.miletracker.feature.media.repository

import com.miletracker.feature.media.model.AttachmentItem
import com.miletracker.feature.media.model.OcrResult
import com.miletracker.feature.media.model.UploadState
import kotlinx.coroutines.delay

/**
 * Fully offline, deterministic [MediaRepository]. Mirrors the tone of
 * `FakeTrackingNetworkApi` in the :stub module: no network, canned values,
 * small artificial delays so the UI can show progress affordances.
 */
class FakeMediaRepository : MediaRepository {
    override suspend fun runOcr(uri: String): OcrResult {
        delay(1100)
        return OcrResult(
            rawText =
                buildString {
                    appendLine("FUEL RECEIPT")
                    appendLine("Station: Demo Petroleum #4821")
                    appendLine("Odometer: 048213 km")
                    appendLine("Litres: 41.20   Total: $73.18")
                }.trim(),
            detectedOdometer = "048213",
            confidence = 0.93f,
            watermarkApplied = true,
        )
    }

    override suspend fun applyWatermark(
        uri: String,
        text: String,
    ): String {
        delay(700)
        // Mock: pretend we wrote a watermarked copy alongside the original.
        return "$uri#watermarked"
    }

    override suspend fun upload(item: AttachmentItem): UploadState.Done {
        delay(900)
        // URI is the canonical URL in this offline demo, no server upload needed.
        return UploadState.Done(remoteUrl = item.uri)
    }
}
