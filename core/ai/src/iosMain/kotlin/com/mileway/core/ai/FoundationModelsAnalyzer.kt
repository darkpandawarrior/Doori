package com.mileway.core.ai

import com.mileway.core.ai.model.AiExtraction
import com.mileway.core.ai.model.DocPrompt
import com.mileway.core.ai.model.DocumentImageRef

// ponytail: EXPERIMENTAL — Apple FoundationModels (LanguageModelSession/@Generable/
// SystemLanguageModel, iOS 18.1+/26) is NOT reachable from Kotlin/Native cinterop on this
// toolchain (Kotlin 2.4.20-Beta1 / Kotlin-Native prebuilt 2.4.20-Beta1 & 2.3.21): there is no
// `platform.FoundationModels.*` package in either version's platform klibs (verified: no
// `org.jetbrains.kotlin.native.platform.FoundationModels` dir under either
// `~/.konan/kotlin-native-prebuilt-macos-aarch64-<ver>/klib/platform/ios_simulator_arm64/`, and
// no matching `.def` under `konan/platformDef/`), unlike e.g. `platform.Vision.*`
// ([VisionTextRecognizer]) or `platform.VisionKit.*` (`MediaCaptureLauncher.ios.kt`'s document
// scanner), which both ARE present. FoundationModels is new enough (and its Swift-macro-driven
// `@Generable` guided output has no Objective-C-compatible surface at all) that def-file
// generation hasn't caught up. A real actual needs a small Swift-side bridge framework exposing
// a plain ObjC-compatible API Kotlin/Native can cinterop against — revisit once Kotlin/Native's
// platform libs pick up FoundationModels, or once a hand-rolled Swift bridge target exists in
// this repo's iOS app shell.
//
// Until then this stub keeps reporting unavailable, so DocumentIntelligence exercises its
// degrade path (TEXT_RECOGNITION + HEURISTIC_CLASSIFIER only) on every iOS device today,
// including hardware that genuinely has Apple Intelligence.
class FoundationModelsAnalyzer : DocumentAiAnalyzer {
    override fun isAvailable(): Boolean = false

    override suspend fun extract(
        image: DocumentImageRef,
        prompt: DocPrompt,
    ): AiExtraction? = null
}
