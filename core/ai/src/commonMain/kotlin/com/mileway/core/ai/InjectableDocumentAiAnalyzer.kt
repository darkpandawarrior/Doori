package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef

/**
 * Generic delegate-or-degrade injection seam: once a platform bridge sets [analyzer], calls are
 * forwarded to it; until then (or if it reports unavailable) this degrades to "unavailable" —
 * never throws, matching [DocumentAiAnalyzer]'s contract. commonMain (not iosMain) so the
 * delegation logic is unit-testable — this repo has no iosTest source set, so any logic living
 * only in iosMain can't run in the gradle gate at all. The iOS-specific holder object using this
 * (e.g. `FoundationModelsBridge`) is a thin iosMain wrapper with nothing left to unit test.
 */
class InjectableDocumentAiAnalyzer : DocumentAiAnalyzer {
    var analyzer: DocumentAiAnalyzer? = null

    override fun isAvailable(): Boolean = analyzer?.isAvailable() ?: false

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? = analyzer?.takeIf { it.isAvailable() }?.extract(image, prompt)
}
