package com.mileway.core.ai

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
// generation hasn't caught up. Kotlin/Native still exports plain Kotlin interfaces TO Swift as
// ObjC protocols though (see `core:media`'s `DocumentAiAnalyzer` usage) — so the real actual is a
// Swift class conforming to [DocumentAiAnalyzer], injected at app-startup into
// [FoundationModelsBridge]. See `iosApp/iosApp/ai/FoundationModelsDocumentAnalyzer.swift`.
//
// Until the Swift app registers a bridge, [FoundationModelsBridge]'s seam degrades to
// unavailable, so DocumentIntelligence exercises its degrade path (TEXT_RECOGNITION +
// HEURISTIC_CLASSIFIER only) — same behavior this stub always had.
object FoundationModelsBridge {
    val seam = InjectableDocumentAiAnalyzer()
}

class FoundationModelsAnalyzer : DocumentAiAnalyzer by FoundationModelsBridge.seam
