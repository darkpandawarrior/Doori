package com.mileway.feature.media.repository

import com.mileway.core.media.model.UploadState
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.OcrResult

/**
 * Abstraction over the media-processing backend (OCR, upload).
 *
 * In this demo every operation is mocked offline; see [FakeMediaRepository]. Watermarking used to
 * live here as a `"$uri#watermarked"` stub — V26 P26.WM moved real watermark burn-in to
 * `core:media`'s `burnWatermark`; V26 P26.SITE.5 deleted the dead stub (no callers left).
 */
interface MediaRepository {
    /** "Reads" an odometer document and returns a parsed [OcrResult]. */
    suspend fun runOcr(uri: String): OcrResult

    /** Uploads the attachment and returns the terminal [UploadState.Done]. */
    suspend fun upload(item: AttachmentItem): UploadState.Done
}
