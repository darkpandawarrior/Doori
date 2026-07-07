@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.media.ui.previews

import androidx.compose.runtime.Composable
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.feature.media.model.OcrResult
import com.mileway.feature.media.ui.sheets.OcrResultBottomSheet

@PreviewLightDark
@Composable
internal fun PreviewOcrResultConfident() {
    PreviewSurface {
        OcrResultBottomSheet(
            result =
                OcrResult(
                    rawText = "87452",
                    detectedOdometer = "87452",
                    confidence = 0.97f,
                    watermarkApplied = false,
                ),
            onConfirm = {},
            onEdit = {},
            onDismiss = {},
        )
    }
}

@PreviewMatrix
@Composable
internal fun PreviewOcrResultLowConfidence() {
    PreviewSurface {
        OcrResultBottomSheet(
            result =
                OcrResult(
                    rawText = "8?452",
                    detectedOdometer = null,
                    confidence = 0.42f,
                    watermarkApplied = false,
                ),
            onConfirm = {},
            onEdit = {},
            onDismiss = {},
        )
    }
}
