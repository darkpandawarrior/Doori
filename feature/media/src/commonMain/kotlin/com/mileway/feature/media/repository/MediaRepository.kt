package com.mileway.feature.media.repository

import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.OcrResult
import com.mileway.feature.media.model.UploadState

/**
 * Abstraction over the media-processing backend (OCR, watermarking, upload).
 *
 * In this demo every operation is mocked offline; see [FakeMediaRepository].
 */
interface MediaRepository {
    /** "Reads" an odometer document and returns a parsed [OcrResult]. */
    suspend fun runOcr(uri: String): OcrResult

    /** Burns [text] into the image at [uri] and returns the (mock) watermarked uri. */
    suspend fun applyWatermark(
        uri: String,
        text: String,
    ): String

    /** Uploads the attachment and returns the terminal [UploadState.Done]. */
    suspend fun upload(item: AttachmentItem): UploadState.Done
}
