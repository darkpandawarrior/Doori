package com.mileway.core.ai

import com.mileway.core.ai.model.DocumentImageRef

// ponytail: EXPERIMENTAL stub — real Vision framework wiring (reusing feature:tracking's
// VisionFrameTextRecognizer) is V26 P-AI. No-op today so DocumentIntelligence still compiles/runs
// on iOS.
class VisionTextRecognizer : TextRecognizer {
    override suspend fun recognize(image: DocumentImageRef): String = ""
}
