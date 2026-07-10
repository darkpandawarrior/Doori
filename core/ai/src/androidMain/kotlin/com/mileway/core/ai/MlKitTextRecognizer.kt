package com.mileway.core.ai

import com.mileway.core.ai.model.DocumentImageRef

// ponytail: EXPERIMENTAL stub — real ML Kit Text Recognition wiring (shared with feature:tracking's
// odometer capture) is V26 P-AI. No-op today so DocumentIntelligence still compiles/runs on Android.
class MlKitTextRecognizer : TextRecognizer {
    override suspend fun recognize(image: DocumentImageRef): String = ""
}
