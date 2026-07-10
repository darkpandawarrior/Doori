package com.mileway.core.ai

import com.mileway.core.ai.model.DocumentImageRef

/**
 * Plain OCR (ML Kit Text Recognition on Android, Vision on iOS — shared with `feature:tracking`'s
 * odometer capture). Always runs; its output feeds both [HeuristicClassifier] and the raw-regex
 * field tier in [AnalysisCombiner].
 */
interface TextRecognizer {
    /** Full recognized text, line breaks preserved; empty string when nothing was read. */
    suspend fun recognize(image: DocumentImageRef): String
}
