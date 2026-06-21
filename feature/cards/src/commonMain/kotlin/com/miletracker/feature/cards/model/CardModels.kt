package com.miletracker.feature.cards.model

/*
 * Q.2 — corporate-cards data layer (commonMain, pure Kotlin). No android.* / java.* imports.
 */

enum class CardStatus { ACTIVE, BLOCKED, FROZEN, KYC_PENDING, PHYSICAL_ISSUED, EXPIRED, PENDING }

enum class CardFormat { VIRTUAL, PHYSICAL }

/** Q+.1 — claim lifecycle (the web's transaction tabs). */
enum class CardTxnClaimStatus { UNCLAIMED, PERSONAL, CLAIMED, RECOVERED, REJECTED }

enum class CardRequestStatus { IN_PROGRESS, APPROVED, REJECTED }

enum class ApprovalStepStatus { PENDING, APPROVED, REJECTED }

data class MccGroupModel(
    val id: Long,
    val name: String,
    val description: String,
    val mccCodes: List<String> = emptyList(),
    val icon: String? = null,
)

data class ThresholdRuleModel(
    val id: Long,
    val lowerBound: Double,
    val upperBound: Double? = null,
    val employeeGrades: List<String> = emptyList(),
    val approvalSteps: List<String> = listOf("Manager", "Finance"),
)

data class ApprovalMatrixModel(
    val id: Long,
    val name: String,
    val description: String = "",
    val thresholdRules: List<ThresholdRuleModel> = emptyList(),
)

data class CardTypeModel(
    val id: Long,
    val name: String,
    val description: String,
    val scheme: String,
    val allowedMccGroups: List<MccGroupModel> = emptyList(),
    val blockedMccGroups: List<MccGroupModel> = emptyList(),
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val defaultSingleTransactionLimit: Double? = null,
    val defaultDailyLimit: Double? = null,
    val defaultMonthlyLimit: Double? = null,
    val isAiSuggested: Boolean = false,
    val approvalMatrix: ApprovalMatrixModel? = null,
)

data class CardModel(
    val id: Long,
    val cardNumber: String,
    val cardHolderName: String,
    val cardType: String,
    val cardTypeId: Long,
    val issuer: String,
    val validThru: String,
    val scheme: String,
    val balance: Double,
    val limit: Double,
    val status: CardStatus,
    val createdAt: Long,
    val lastUpdatedAt: Long,
    val isVirtual: Boolean = true,
    val isFrozen: Boolean = false,
    val isKycPending: Boolean = false,
    val cardFormat: CardFormat = CardFormat.VIRTUAL,
    val currency: String = "AED",
    // Q+.2 — balance header (web shows holder name + email + Available Balance).
    val employeeEmail: String? = null,
    // Q+.3 — card controls.
    val monthlyLimit: Double? = null,
    val singleTransactionLimit: Double? = null,
    val dailyTransactionLimit: Double? = null,
    val allowedMccGroups: List<String> = emptyList(),
)

data class CardTransactionModel(
    val id: Long,
    val cardId: Long,
    val merchantName: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val claimStatus: CardTxnClaimStatus,
    val txnNumber: String,
    val currency: String = "AED",
    val mcc: String? = null,
    val mccGroup: String? = null,
    val description: String? = null,
    val location: String? = null,
    val requiresApproval: Boolean = false,
)

data class ApprovalStepModel(
    val id: Long,
    val title: String,
    val status: ApprovalStepStatus,
    val description: String = "",
    val approverName: String? = null,
    val approvalDate: Long? = null,
    val comment: String? = null,
    val order: Int = 0,
)

data class CardRequestModel(
    val id: Long,
    val cardTypeId: Long,
    val status: CardRequestStatus,
    val requestDate: Long,
    val cardType: String = "",
    val requesterName: String? = null,
    val requesterGrade: String? = null,
    val reason: String? = null,
    val requestedLimit: Double? = null,
    val approvalSteps: List<ApprovalStepModel> = emptyList(),
)

/** Q+.3 — physical-card shipping address (web "Issue Physical Card" form). */
data class CardShippingAddress(
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val state: String,
    val pincode: String,
    val country: String = "India",
)
