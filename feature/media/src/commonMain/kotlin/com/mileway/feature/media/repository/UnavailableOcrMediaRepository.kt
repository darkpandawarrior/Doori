package com.mileway.feature.media.repository

import com.mileway.core.media.model.UploadState
import com.mileway.feature.media.model.AttachmentItem
import com.mileway.feature.media.model.OcrResult

/** Retains attachments and reports manual entry when this build has no OCR backend. */
class UnavailableOcrMediaRepository : MediaRepository {
    override suspend fun runOcr(uri: String): OcrResult = OcrResult("OCR is not available in this build. Enter the reading manually.", null, 0f, false)

    override suspend fun upload(item: AttachmentItem): UploadState.Done = UploadState.Done("local://${item.id}")
}
