package com.mileway.feature.agent.engine

import com.mileway.core.data.dao.SavedTrackDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

private const val THINKING_DELAY_MS = 800L
private const val WORD_DELAY_MS = 35L
private const val KM_RATE = 10.0
private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

class OfflineAssistantEngine(private val savedTrackDao: SavedTrackDao) : AssistantEngine {

    override fun respond(conversationId: String, userMessage: String, historySize: Int): Flow<AssistantChunk> = flow {
        val intent = IntentClassifier.classify(userMessage)
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

    private suspend fun buildReply(intent: Intent): String = when (intent) {
        Intent.MILEAGE_WEEK -> mileageThisWeek()
        Intent.MILEAGE_RATE ->
            "The standard reimbursement rate is **₹10 per km** for four-wheelers and **₹5/km** for two-wheelers. GPS-tracked trips qualify automatically."
        Intent.EXPENSE_REJECTION ->
            "EXP-003 was rejected because the uploaded receipt was unclear. Please re-upload a legible image and resubmit."
        Intent.POLICY_CAP ->
            "The daily mileage cap is **₹10/km**. Claims above this threshold are flagged for review. Your manager can approve the overage."
        Intent.ADVANCE_STATUS ->
            "ADV-001 (₹8,000) was approved on 14 Nov. Your next advance is available after settling this one."
        Intent.CARD_BALANCE ->
            "Corporate card **** 4821 has a balance of **₹48,000**. Petty cash QR card shows **₹2,400**."
        Intent.PENDING_APPROVALS ->
            "You have **3 pending approvals**: 2 mileage claims and 1 expense. The oldest is 4 days old."
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
            "You've tracked **${totalKm.toInt()} km** across **$tripCount trip${if (tripCount == 1) "" else "s"}** in the last 7 days. Estimated reimbursement: **₹$est**."
        }
    }
}
