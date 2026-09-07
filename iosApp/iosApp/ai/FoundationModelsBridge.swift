// Real actual: Apple Foundation Models. Conforms to kmp-toolkit's `NativeLlm`
// (com.siddharth.kmp.ai.NativeLlm, exported via shared/build.gradle.kts's
// `export("com.siddharth.kmp:ai:1.0.0")`) and is registered into the toolkit's own
// `FoundationModelsBridge.shared.seam` (com.siddharth.kmp.ai.FoundationModelsBridge) at app
// startup — see AppDelegate.swift. Adapted from kmp-toolkit's `ai/ios-bridge/FoundationModelsBridge.swift`
// template (`import YourApp` → `import Mileway`).
//
// core:ai's FoundationModelsAnalyzer (document extraction) and feature:agent's
// FoundationModelsLlmGateway (assistant chat) both sit on top of this ONE bridge now — previously
// each carried its own separate Swift class (FoundationModelsDocumentAnalyzer.swift,
// FoundationModelsTextGenerator.swift) and its own injection seam; the toolkit's NativeLlm is a
// generic text-in/text-out surface both consumers share.
//
// Named MilewayFoundationModelsBridge (not `FoundationModelsBridge`) to avoid colliding with the
// Kotlin/Native-exported `com.siddharth.kmp.ai.FoundationModelsBridge` object, which this same app
// target imports from `Mileway` under that exact name.
//
// `generate`/`generateStream` are Kotlin `suspend fun`/plain callback shapes — classic ObjC-based
// Kotlin/Native interop exports the suspend one as a completion-handler method, same shape this
// repo's other bridges (see MilewayWatchGraph.swift's Kotlinx_coroutines_coreFlowCollector
// conformance) already use.
//
// `LanguageModelSession.streamResponse(to:)` returns an AsyncSequence of CUMULATIVE partial
// responses (each element is the whole reply so far, not just the new suffix) — diffed against the
// last-seen text and threaded through NativeLlmStreamCallback.onPartial as new suffixes, so a UI
// renders tokens as Apple's on-device model produces them instead of waiting for the whole reply.
//
// Verify with (once Xcode + iOS 26.0 SDK are available):
//   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
//     -destination 'generic/platform=iOS Simulator' build

import Foundation
import FoundationModels
import Mileway

final class MilewayFoundationModelsBridge: NSObject, NativeLlm {
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
                // through this bridge yet, same simplification this repo's own prior Foundation
                // Models bridges made. Upgrade to one retained LanguageModelSession (carrying call
                // history) if multi-turn on-device context turns out to matter.
                let session = LanguageModelSession()
                let response = try await session.respond(to: prompt)
                completionHandler(response.content, nil)
            } catch {
                // NativeLlm.generate never throws (see its KDoc) — degrade to nil rather than
                // propagating a Swift error across the interop boundary; the Kotlin side
                // (FoundationModelsOnDeviceLlm) turns nil into a typed AiFailure.EmptyReply.
                completionHandler(nil, nil)
            }
        }
    }

    func generateStream(prompt: String, callback: NativeLlmStreamCallback) -> NativeLlmCancelHandle {
        guard #available(iOS 26.0, *), isAvailable() else {
            callback.onComplete()
            return CancelToken(task: nil)
        }
        let task = Task {
            do {
                let session = LanguageModelSession()
                var lastText = ""
                for try await partial in session.streamResponse(to: prompt) {
                    // Checked on every element rather than relying solely on `for try await`'s
                    // cooperative cancellation propagating through the AsyncSequence, since
                    // streamResponse isn't documented to check cancellation itself.
                    if Task.isCancelled { break }
                    let full = partial.content
                    if full.count > lastText.count {
                        callback.onPartial(String(full.dropFirst(lastText.count)))
                        lastText = full
                    }
                }
                if !Task.isCancelled {
                    callback.onComplete()
                }
            } catch {
                callback.onError()
            }
        }
        return CancelToken(task: task)
    }
}

/// Cancelling the underlying `Task` is what actually stops `streamResponse`'s AsyncSequence
/// iteration (via the `Task.isCancelled` check in the loop above) — the model itself stops being
/// asked for more output, not just this bridge stopping listening.
private final class CancelToken: NativeLlmCancelHandle {
    private let task: Task<Void, Never>?

    init(task: Task<Void, Never>?) {
        self.task = task
    }

    func cancel() {
        task?.cancel()
    }
}
