package com.mileway.feature.profile.model

enum class AdvanceStatus { PENDING, UNDER_REVIEW, APPROVED, DISBURSED, REJECTED }

data class AdvanceRecord(
    val id: String,
    val amountRupees: Double,
    val purpose: String,
    val status: AdvanceStatus,
    val requestedDateMs: Long,
    val requiredByDate: String,
)

enum class CardType { VISA, MASTERCARD }

enum class CardStatus { ACTIVE, BLOCKED }

data class CorporateCard(
    val id: String,
    val lastFourDigits: String,
    val cardType: CardType,
    val holderName: String,
    val balanceRupees: Double,
    val status: CardStatus,
    val expiryDate: String,
    val creditLimitRupees: Double,
)

data class CardTransaction(
    val id: String,
    val cardId: String,
    val merchantName: String,
    val amountRupees: Double,
    val dateMs: Long,
    val category: String,
)
