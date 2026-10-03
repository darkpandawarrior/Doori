package com.mileway.core.ai

import com.mileway.core.ai.model.DocumentImageRef

/**
 * Plain OCR (ML Kit Text Recognition on Android, Vision on iOS — shared with `feature:tracking`'s
 * odometer capture). Available backends feed both [HeuristicClassifier] and the raw-regex
 * field tier in [AnalysisCombiner].
 */
interface TextRecognizer {
    /** Whether this build supplies OCR; false means callers must offer manual entry. */
    fun isAvailable(): Boolean = true

    /** Full recognized text, line breaks preserved; empty string when nothing was read. */
    suspend fun recognize(image: DocumentImageRef): String
}
