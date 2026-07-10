package com.mileway.feature.logging.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.stub.PolicyMockData

enum class ExpenseCategory(val label: String, val icon: ImageVector) {
    FOOD("Food", Icons.Filled.Restaurant),
    TRAVEL("Travel", Icons.Filled.DirectionsCar),
    ACCOMMODATION("Accommodation", Icons.Filled.Hotel),
    OFFICE_SUPPLIES("Office Supplies", Icons.Filled.Receipt),
    COMMUNICATION("Communication", Icons.Filled.PhoneAndroid),
    MEDICAL("Medical", Icons.Filled.LocalHospital),
    OTHER("Other", Icons.Filled.Category),
}

enum class ExpenseStatus { DRAFT, PENDING, APPROVED, REJECTED }

/**
 * Per-category field requirements, sourced from [com.mileway.feature.logging.catalog.ExpenseCategoryCatalog]
 * rather than hardcoded per-screen `when` branches. Lets a category gain a required field (receipt,
 * cost center, GST) without a new screen.
 */
data class ExpenseCategoryDef(
    val category: ExpenseCategory,
    val requiresReceipt: Boolean = false,
    val requiresCostCenter: Boolean = false,
    val requiresGst: Boolean = false,
)

/** P2.1: lifecycle of a single row inside the bulk-entry grid ([ExpenseDraftRow]). */
enum class DraftStatus { PENDING, SUBMITTING, SUCCESS, ERROR }

/**
 * P2.1: one row of the multi-item bulk expense entry grid — the local, offline equivalent of the
 * reference app's concurrent batch-submit rows. Deliberately mirrors the single-entry
 * [ExpenseFormState] field set (category, amount, merchant, note) plus a per-row [status] so a
 * batch submit (P2.3) can report a mixed outcome across rows instead of an all-or-nothing result.
 */
data class ExpenseDraftRow(
    val id: String,
    val category: ExpenseCategory? = null,
    val amountText: String = "",
    val merchantName: String = "",
    val note: String = "",
    val status: DraftStatus = DraftStatus.PENDING,
    /**
     * P2.5: local URI/path of an optional receipt photo scanned/attached for this row specifically
     * (via [com.mileway.feature.logging.ui.screens.rememberReceiptAttachmentLauncher], the same
     * per-row-scoped launcher the single-entry form's [ExpenseCategoryDef.requiresReceipt] flow
     * already uses through P1.4); null when this row has no attachment.
     */
    val receiptImagePath: String? = null,
)

data class ExpenseRecord(
    val id: String,
    val category: ExpenseCategory,
    val merchantName: String,
    val amountRupees: Double,
    val status: ExpenseStatus,
    val dateMs: Long,
    val note: String = "",
    /** Local URI/path of an optional attached receipt photo (P1.4); null when none was attached. */
    val receiptImagePath: String? = null,
    /**
     * P1.7: [com.mileway.core.network.model.Office.code] of the project/cost-center this expense
     * is tagged against, sourced from [com.mileway.stub.PolicyMockData.offices]. Only meaningful
     * (and only shown/required in the entry form) for categories where
     * [ExpenseCategoryDef.requiresCostCenter] is true; null otherwise.
     */
    val officeCode: String? = null,
    /**
     * P1.9: the manager/policy-engine's real reason for a [ExpenseStatus.REJECTED] record, shown
     * in [com.mileway.feature.logging.ui.screens.ExpenseDetailScreen]'s approval timeline instead
     * of a static placeholder string. Null for any non-rejected record.
     */
    val rejectionReason: String? = null,
    /**
     * P27.E.15: the currency [amountRupees] was originally entered in (static local conversion
     * table only — no live FX backend). [amountRupees] itself is always the rupee figure used for
     * settlement/policy checks unchanged from before this field existed; this is display metadata.
     */
    val currencyCode: String = "INR",
) {
    /**
     * P27.E.14: computed from [com.mileway.stub.PolicyMockData]'s tiered expense-amount policy
     * engine (same three violation-shaped outcomes [ExpenseSuccessScreen] already gates its
     * approval banner on) instead of a standalone hardcoded `> 5000` literal that could silently
     * drift from the real policy thresholds.
     */
    val requiresApproval: Boolean
        get() {
            val outcome = PolicyMockData.outcomeForExpenseAmount(amountRupees, category.name)
            return outcome == SubmissionStatus.POLICY_VIOLATION ||
                outcome == SubmissionStatus.NEEDS_APPROVAL ||
                outcome == SubmissionStatus.HARD_STOP
        }
}
