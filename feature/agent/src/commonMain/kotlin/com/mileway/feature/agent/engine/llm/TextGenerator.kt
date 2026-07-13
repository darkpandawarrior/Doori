package com.mileway.feature.agent.engine.llm

/**
 * Platform-agnostic on-device text generation seam, mirroring `core:ai`'s [com.mileway.core.ai.DocumentAiAnalyzer]
 * bridge shape. iOS has no `platform.FoundationModels.*` Kotlin/Native binding (see core:ai's
 * `FoundationModelsAnalyzer` doc), so the iOS [LlmGateway] delegates to a Swift class conforming to
 * this interface, injected via `FoundationModelsTextGeneratorBridge` (iosMain).
 */
interface TextGenerator {
    fun isAvailable(): Boolean

    /** Null means "declined to generate" (unavailable or a runtime failure) — never throws. */
    suspend fun generate(prompt: String): String?
}

/**
 * Generic delegate-or-degrade injection seam, same shape as core:ai's `InjectableDocumentAiAnalyzer`
 * — kept in commonMain so it's unit-testable (this repo has no iosTest source set).
 */
class InjectableTextGenerator : TextGenerator {
    var generator: TextGenerator? = null

    override fun isAvailable(): Boolean = generator?.isAvailable() ?: false

    override suspend fun generate(prompt: String): String? = generator?.takeIf { it.isAvailable() }?.generate(prompt)
}
