package com.miletracker.feature.agent.repository

import com.miletracker.feature.agent.model.AgentConversation
import com.miletracker.feature.agent.model.AgentMessage
import com.miletracker.feature.agent.model.PopularQuestion
import com.miletracker.feature.agent.model.UnansweredQuestion
import com.miletracker.stub.AgentMockData

class AgentRepository {
    val conversations: List<AgentConversation> =
        AgentMockData.conversations.map { stub ->
            AgentConversation(
                id = stub.id,
                title = stub.title,
                lastMessageMs = stub.lastMessageMs,
                messages =
                    stub.messages.map { msg ->
                        AgentMessage(text = msg.text, isUser = msg.isUser, timestampMs = msg.timestampMs)
                    },
            )
        }

    val popularQuestions: List<PopularQuestion> =
        AgentMockData.popularQuestions.map { stub ->
            PopularQuestion(
                id = stub.id,
                question = stub.question,
                module = stub.module,
                askCount = stub.askCount,
                isTrending = stub.isTrending,
            )
        }

    val unansweredQuestions: List<UnansweredQuestion> =
        AgentMockData.unansweredQuestions.map { stub ->
            UnansweredQuestion(
                id = stub.id,
                question = stub.question,
                module = stub.module,
                askCount = stub.askCount,
            )
        }

    private val quickReplies: Map<String, String> =
        mapOf(
            "travel spend" to "You've spent ₹12,300 on Travel this month — flights ₹7,800, hotels ₹3,200, local transit ₹1,300.",
            "expense rejection" to "EXP-003 was rejected because the uploaded receipt was unclear. Please re-upload a legible image and resubmit.",
            "mileage this week" to "You've tracked 142 km this week across 6 trips. Estimated reimbursement: ₹1,420.",
            "policy cap" to "The daily mileage cap is ₹10/km. Your 3 flagged claims this week exceed this. Your manager can approve the overage.",
            "advance status" to "ADV-001 (₹8,000) was approved on 14 Nov. Your next advance is available after settlement of this one.",
            "pending approvals" to "You have 3 pending approvals: 2 mileage claims and 1 expense. Oldest is 4 days old.",
            "card balance" to "Corporate card **** 4821 has a balance of ₹48,000. Petty cash QR card shows ₹2,400.",
            "trip summary" to "Active trip: PNQ → BOM, IndiGo 6E-401, Gate B7, boarding 14:30. 3 upcoming trips in the next 35 days.",
        )

    fun quickReply(question: String): String {
        val lower = question.lowercase()
        return quickReplies.entries.firstOrNull { lower.contains(it.key) }?.value
            ?: "I can help with that. Please provide more context about your query."
    }
}
