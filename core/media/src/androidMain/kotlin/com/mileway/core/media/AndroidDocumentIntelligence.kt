package com.mileway.core.media

import com.mileway.core.ai.DocumentAiAnalyzer
import com.mileway.core.ai.DocumentIntelligence
import com.mileway.core.ai.KeywordHeuristicClassifier
import com.mileway.core.ai.NoDocumentAiAnalyzer
import com.mileway.core.ai.NoTextRecognizer
import com.mileway.core.ai.TextRecognizer
import org.koin.mp.KoinPlatform

/** Uses the flavor's OCR backend; absent bindings safely retain manual capture. */
fun androidDocumentIntelligence(): DocumentIntelligence {
    val koin = KoinPlatform.getKoin()
    return DocumentIntelligence(
        aiAnalyzer = koin.getOrNull<DocumentAiAnalyzer>() ?: NoDocumentAiAnalyzer,
        textRecognizer = koin.getOrNull<TextRecognizer>() ?: NoTextRecognizer,
        classifier = KeywordHeuristicClassifier,
    )
}
