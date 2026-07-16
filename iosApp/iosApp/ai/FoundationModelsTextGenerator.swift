// EXPERIMENTAL — Apple Foundation Models, xcodebuild-gated (see FoundationModelsDocumentAnalyzer.swift
// for the full rationale; this is the same bridge mechanism applied to free-text assistant replies
// instead of structured document extraction). Written-correct, NOT compiled/device-verified here.
//
// Conforms to `TextGenerator` (exported from `feature:agent` via `shared/build.gradle.kts`'s
// existing `export(project(":feature:agent"))`), registered into
// `FoundationModelsTextGeneratorBridge.shared.seam` at app startup (AppDelegate.swift). `generate`
// is a Kotlin `suspend fun` → completion-handler shape, same as FoundationModelsDocumentAnalyzer.
//
// Verify with:
//   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
//     -destination 'generic/platform=iOS Simulator' build

import Foundation
import FoundationModels
import Mileway

/// Whole-response generation only — `LanguageModelSession.respond(to:)` isn't wired through this
/// bridge incrementally, so `FoundationModelsLlmGateway` (Kotlin iosMain) surfaces the full reply as
/// a single token rather than true per-word streaming. A future revision could switch to
/// `LanguageModelSession.streamResponse(to:)` and thread partial strings through repeated
/// completionHandler-adjacent callbacks, if perceived latency on-device makes that worth the extra
/// plumbing.
final class FoundationModelsTextGenerator: NSObject, TextGenerator {
    func isAvailable() -> Bool {
        guard #available(iOS 26.0, *) else { return false }
        return SystemLanguageModel.default.availability == .available
    }

    func generate(prompt: String, completionHandler: @escaping (String?, Error?) -> Void) {
        guard #available(iOS 26.0, *), isAvailable() else {
            completionHandler(nil, nil)
            return
        }
        Task {
            do {
                // ponytail: a fresh session per call — no cross-turn context/history threaded
                // through this seam yet. Upgrade to one retained LanguageModelSession() (with
                // .respond(to:) call history) if multi-turn on-device context turns out to matter.
                let session = LanguageModelSession()
                let response = try await session.respond(to: prompt)
                completionHandler(response.content, nil)
            } catch {
                // TextGenerator.generate never throws — degrade to nil, same as
                // FoundationModelsDocumentAnalyzer.extract.
                completionHandler(nil, nil)
            }
        }
    }
}
