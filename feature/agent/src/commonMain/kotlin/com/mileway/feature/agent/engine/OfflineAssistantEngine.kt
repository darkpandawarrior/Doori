package com.mileway.feature.agent.engine

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.cards.data.CardsMockDataProvider
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.siddharth.kmp.ai.KeywordClassifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

private const val THINKING_DELAY_MS = 800L
private const val WORD_DELAY_MS = 35L
private const val KM_RATE = 10.0
private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

enum class Intent {
    MILEAGE_WEEK,
    MILEAGE_RATE,
    EXPENSE_REJECTION,
    POLICY_CAP,
    ADVANCE_STATUS,
    CARD_BALANCE,
    PENDING_APPROVALS,
    TRIP_SUMMARY,
    GENERIC,
}

/**
 * Buckets a chat message into an [Intent] via kmp-toolkit's shared [KeywordClassifier], replacing
 * the hand-rolled local matcher this file used to carry (same keyword lists and declaration
 * order, so the common no-tie cases classify identically; [KeywordClassifier] scores every
 * category by keyword-hit count instead of stopping at the first match, so a message that hits
 * more keywords in a later category now wins on merit rather than declaration order).
 */
private val intentClassifier =
    KeywordClassifier(
        mapOf(
            Intent.MILEAGE_WEEK to
                listOf(
                    "km this week", "km last week", "mileage this week", "distance this week",
                    "trips this week", "how many km", "km did i", "tracked",
                ),
            Intent.MILEAGE_RATE to listOf("reimbursement rate", "per km", "rate per km", "mileage rate"),
            Intent.EXPENSE_REJECTION to listOf("expense", "rejected", "rejection", "exp-"),
            Intent.POLICY_CAP to listOf("policy cap", "policy limit", "policy alert", "₹10/km", "mileage cap"),
            Intent.ADVANCE_STATUS to listOf("advance", "adv-"),
            Intent.CARD_BALANCE to listOf("card balance", "card", "petty cash"),
            Intent.PENDING_APPROVALS to listOf("pending approval", "approvals", "approve"),
            Intent.TRIP_SUMMARY to listOf("trip summary", "trip", "travel", "flight", "booking"),
        ),
    )

/**
 * The degrade path every user without Gemini-Nano-class hardware (or on iOS, where the Foundation
 * Models bridge is unregistered) actually gets. Answers from the same repositories the
 * approvals/cards/advances/expense screens read — see PLAN mileway-assistant-real-data — so this
 * never states a number those screens wouldn't also show. Only [mileageThisWeek] was ever real
 * before this change; the other three data-backed intents below now match it.
 */
class OfflineAssistantEngine(
    private val savedTrackDao: SavedTrackDao,
    private val advancesRepository: AdvancesRepository,
    private val expenseRepository: ExpenseRepository,
    private val cardsProvider: CardsMockDataProvider = CardsMockDataProviderFactory.provider(),
) : AssistantEngine {
    override fun respond(
        conversationId: String,
        userMessage: String,
        historySize: Int,
    ): Flow<AssistantChunk> =
        flow {
            val intent = intentClassifier.classify(userMessage) ?: Intent.GENERIC
            emit(AssistantChunk.Thinking(ThinkingPhrases.forIntent(intent)))
            delay(THINKING_DELAY_MS)

            val replyText = buildReply(intent)
            replyText.split(" ").forEach { word ->
                emit(AssistantChunk.Token("$word "))
                delay(WORD_DELAY_MS)
            }

            val titleSuggestion = if (historySize == 0) ConversationTitler.title(userMessage) else null
            emit(AssistantChunk.Done(replyText, titleSuggestion))
        }

    private suspend fun buildReply(intent: Intent): String =
        when (intent) {
            Intent.MILEAGE_WEEK -> mileageThisWeek()
            Intent.MILEAGE_RATE ->
                "The standard reimbursement rate is **₹10 per km** for four-wheelers and **₹5/km** for two-wheelers. GPS-tracked trips qualify automatically."
            Intent.EXPENSE_REJECTION -> latestRejectedExpense()
            Intent.POLICY_CAP ->
                "The daily mileage cap is **₹10/km**. Claims above this threshold are flagged for review. Your manager can approve the overage."
            Intent.ADVANCE_STATUS -> latestAdvanceStatus()
            Intent.CARD_BALANCE -> cardBalances()
            Intent.PENDING_APPROVALS -> pendingApprovalsSummary()
            // ponytail: travel itinerary has no real per-user data source wired here — the brief
            // for this lane named approvals/cards/advances/expense only. Upgrade the same way:
            // inject feature:travel's real itinerary repository and query it here.
            Intent.TRIP_SUMMARY ->
                "Active trip: PNQ → BOM, IndiGo 6E-401, Gate B7, boarding 14:30. 3 upcoming trips in the next 35 days."
            Intent.GENERIC ->
                "I can help with mileage, expenses, approvals, corporate cards, and travel. What would you like to know?"
        }

    private suspend fun mileageThisWeek(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val weekAgo = now - WEEK_MS
        val allTracks = savedTrackDao.getCompletedTracks().first()
        val recent = allTracks.filter { it.endTime >= weekAgo }
        val totalKm = recent.sumOf { it.distance }
        val tripCount = recent.size
        return if (tripCount == 0) {
            "You haven't tracked any trips in the last 7 days. Head to the **Track Miles** screen to start recording."
        } else {
            val est = (totalKm * KM_RATE).toLong()
            "You've tracked **${totalKm.toInt()} km** across " +
                "**$tripCount trip${if (tripCount == 1) "" else "s"}** in the last 7 days. " +
                "Estimated reimbursement: **₹$est**."
        }
    }

    private fun latestRejectedExpense(): String {
        val rejected = expenseRepository.filterByStatus(ExpenseStatus.REJECTED).maxByOrNull { it.dateMs }
        return if (rejected == null) {
            "You don't have any rejected expenses right now."
        } else {
            "**${rejected.id}** (${rejected.merchantName}, ₹${rejected.amountRupees.toInt()}) was rejected: " +
                (rejected.rejectionReason ?: "no reason was recorded — check the Expenses screen for details.")
        }
    }

    private suspend fun latestAdvanceStatus(): String {
        val latest =
            (advancesRepository.openRequests().first() + advancesRepository.closedRequests().first())
                .maxByOrNull { it.createdAtMs }
        return if (latest == null) {
            "You have no advance requests on file."
        } else {
            "Your most recent advance request — **${latest.title}** (₹${latest.amount.toInt()}) — is **${latest.status.displayStatus()}**."
        }
    }

    private fun cardBalances(): String {
        val cards = cardsProvider.virtualCards()
        return if (cards.isEmpty()) {
            "You don't have any cards yet."
        } else {
            cards.joinToString(separator = "; ") { card ->
                val statusSuffix = if (card.status.name == "ACTIVE") "" else ", ${card.status.name.lowercase().replace('_', ' ')}"
                "**** ${card.cardNumber} (${card.cardType}$statusSuffix) ₹${card.balance.toInt()}"
            }
        }
    }

    private fun pendingApprovalsSummary(): String {
        val pending = ApprovalsRepository.all.filter { it.status == ApprovalStatus.PENDING }
        if (pending.isEmpty()) return "You have no pending approvals."
        val breakdown =
            pending
                .groupingBy { it.type }
                .eachCount()
                .entries
                .joinToString(", ") { (type, count) -> "$count ${type.name.lowercase()}" }
        return "You have **${pending.size} pending approval${if (pending.size == 1) "" else "s"}**: $breakdown."
    }
}
