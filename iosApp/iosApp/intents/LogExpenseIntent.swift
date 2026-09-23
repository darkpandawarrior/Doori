// P7.1: iOS App Intent — Siri/Spotlight/Shortcuts entry point for logging an expense. Mirrors
// Android AppFunctions' `MileageAppFunctions.logExpense` (P7.5): category is matched
// case-insensitively against the shared `ExpenseCategory` label/name (unrecognized -> "Other"),
// merchant/note are optional free text, amount must be positive.

import AppIntents
import Mileway

struct LogExpenseIntent: AppIntent {
    static var title: LocalizedStringResource = "Log Doori Expense"
    static var description = IntentDescription("Records a new expense in Doori.")

    @Parameter(title: "Category", default: "Other")
    var category: String

    @Parameter(title: "Amount (₹)")
    var amountRupees: Double

    @Parameter(title: "Merchant", default: "")
    var merchantName: String

    @Parameter(title: "Note", default: "")
    var note: String

    static var parameterSummary: some ParameterSummary {
        Summary("Log a \(\.$category) expense of ₹\(\.$amountRupees)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ReturnsValue<String> {
        guard amountRupees > 0 else {
            throw $amountRupees.needsValueError("Enter an amount greater than zero.")
        }
        let id = try await IosIntentEntry.shared.logExpense(
            category: category,
            amountRupees: amountRupees,
            merchantName: merchantName,
            note: note
        )
        return .result(value: id)
    }
}
