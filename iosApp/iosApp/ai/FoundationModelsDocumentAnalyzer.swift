// EXPERIMENTAL — Apple Foundation Models, xcodebuild-gated (this environment has no Xcode/iOS SDK,
// so this file is written-correct but NOT compiled or device-verified here).
//
// Kotlin/Native has no `platform.FoundationModels.*` cinterop binding (see core:ai's
// `FoundationModelsAnalyzer.kt` doc), so this is the real actual: a Swift class conforming to the
// `DocumentAiAnalyzer` protocol Kotlin/Native exports from `core:ai` (via `shared/build.gradle.kts`'s
// `export(project(":core:ai"))`), registered into `FoundationModelsBridge.shared.seam` at app
// startup (`AppDelegate.swift`). `DocumentAiAnalyzer.extract` is a Kotlin `suspend fun`, which the
// classic ObjC-based K/N interop this repo uses (see `MilewayWatchGraph.swift`'s
// `Kotlinx_coroutines_coreFlowCollector` conformance) exports as a completion-handler method, not
// `async` — implement that shape and bridge to `async/await` internally with a `Task`.
//
// Verify with (once Xcode + iOS 26.0 SDK are available):
//   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
//     -destination 'generic/platform=iOS Simulator' build

import Foundation
import FoundationModels
import Mileway

/// `@Generable` guided-output shape mirroring `core:ai`'s `AiExtraction`/`DocField` — one flat
/// struct covering every `DocField` since a `DocPrompt.schemaHint` picks which subset actually
/// matters per `DocType` (unset fields stay nil).
@available(iOS 26.0, *)
@Generable
struct GeneratedDocExtraction {
    @Guide(description: "One of: RECEIPT, INVOICE, ODOMETER, TRAVEL_TICKET, ID_DOCUMENT, OTHER")
    var docType: String
    @Guide(description: "Merchant or vendor name, if present")
    var merchant: String?
    @Guide(description: "Total amount, if present")
    var total: String?
    @Guide(description: "Tax amount, if present")
    var tax: String?
    @Guide(description: "Document date, if present")
    var date: String?
    @Guide(description: "Invoice number, if present")
    var invoiceNo: String?
    @Guide(description: "Odometer reading, if present")
    var odometer: String?
    @Guide(description: "Expense category, if present")
    var category: String?
    @Guide(description: "Currency code, if present")
    var currency: String?
}

/// EXPERIMENTAL — see file header. Availability-gates on `SystemLanguageModel.default.availability`
/// so pre-iOS-26.0 devices and iOS 26.0+ devices without Apple Intelligence enabled both degrade to
/// `DocumentIntelligence`'s TEXT_RECOGNITION + HEURISTIC_CLASSIFIER path, same as before this file
/// existed.
final class FoundationModelsDocumentAnalyzer: NSObject, DocumentAiAnalyzer {
    func isAvailable() -> Bool {
        guard #available(iOS 26.0, *) else { return false }
        return SystemLanguageModel.default.availability == .available
    }

    func extract(
        image: String,
        prompt: DocPrompt,
        completionHandler: @escaping (AiExtraction?, Error?) -> Void
    ) {
        guard #available(iOS 26.0, *), isAvailable() else {
            completionHandler(nil, nil)
            return
        }
        Task {
            let result = await Self.runExtraction(imagePath: image, prompt: prompt)
            completionHandler(result, nil)
        }
    }

    @available(iOS 26.0, *)
    private static func runExtraction(imagePath: String, prompt: DocPrompt) async -> AiExtraction? {
        // `extract` never throws (per DocumentAiAnalyzer's contract) — every failure path below
        // degrades to nil instead of propagating.
        do {
            let session = LanguageModelSession()
            let response = try await session.respond(
                to: "\(prompt.instruction)\n\n\(prompt.schemaHint)",
                generating: GeneratedDocExtraction.self
            )
            return toAiExtraction(response.content, prompt: prompt)
        } catch {
            return nil
        }
    }

    @available(iOS 26.0, *)
    private static func toAiExtraction(_ generated: GeneratedDocExtraction, prompt: DocPrompt) -> AiExtraction {
        var fields: [DocField: ExtractedValue] = [:]
        func put(_ field: DocField, _ value: String?) {
            guard let value, !value.isEmpty else { return }
            fields[field] = ExtractedValue(value: value, confidence: kResponseConfidence, source: .onDeviceAi)
        }
        put(.merchant, generated.merchant)
        put(.total, generated.total)
        put(.tax, generated.tax)
        put(.date, generated.date)
        put(.invoiceNo, generated.invoiceNo)
        put(.odometer, generated.odometer)
        put(.category, generated.category)
        put(.currency, generated.currency)

        let docType = docType(from: generated.docType) ?? prompt.docType
        return AiExtraction(
            docType: docType,
            fields: fields,
            rawText: generated.docType,
            confidence: kResponseConfidence
        )
    }
}

// Matches core:ai's MlKitGenAiAnalyzer.RESPONSE_CONFIDENCE — above AnalysisCombiner's
// AI_CONFIDENT_THRESHOLD (0.6) so a confident FM call wins the merge.
private let kResponseConfidence: Float = 0.7

// Kotlin enums exported via classic ObjC K/N interop swiftify SCREAMING_CASE cases to
// lowerCamelCase (e.g. `DocType.RECEIPT` -> `.receipt`, `DocType.TRAVEL_TICKET` -> `.travelTicket`)
// — mirrored manually here rather than via `DocType.entries` since that companion accessor isn't
// guaranteed present on every K/N enum ObjC export.
private func docType(from raw: String) -> DocType? {
    switch raw.uppercased() {
    case "RECEIPT": return .receipt
    case "INVOICE": return .invoice
    case "ODOMETER": return .odometer
    case "TRAVEL_TICKET": return .travelTicket
    case "ID_DOCUMENT": return .idDocument
    case "OTHER": return .other
    default: return nil
    }
}
