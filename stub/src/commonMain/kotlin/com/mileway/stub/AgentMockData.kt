@file:Suppress("MaxLineLength")

package com.mileway.stub

private const val AGENT_BASE_MS = 1_781_654_400_000L
private const val AGENT_DAY_MS = 86_400_000L
private const val AGENT_HR_MS = 3_600_000L

data class AgentMessageStub(val text: String, val isUser: Boolean, val timestampMs: Long)

data class AgentConversationStub(val id: String, val title: String, val lastMessageMs: Long, val messages: List<AgentMessageStub>)

data class PopularQuestionStub(val id: String, val question: String, val module: String, val askCount: Int, val isTrending: Boolean)

data class UnansweredQuestionStub(val id: String, val question: String, val module: String, val askCount: Int)

object AgentMockData {
    val conversations: List<AgentConversationStub> =
        listOf(
            AgentConversationStub(
                id = "CONV-001",
                title = "Mileage reimbursement rate",
                lastMessageMs = AGENT_BASE_MS - AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "What is the per-km reimbursement rate?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "The standard rate is ₹10 per km for personal four-wheelers and ₹5/km for two-wheelers. GPS-tracked trips qualify automatically.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - AGENT_DAY_MS - AGENT_HR_MS + 3000L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-002",
                title = "Expense rejection: EXP-003",
                lastMessageMs = AGENT_BASE_MS - 2 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "Why was EXP-003 rejected?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 2 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "EXP-003 was rejected because the uploaded receipt was unclear: the amount and merchant name could not be verified. Please re-upload a legible image.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 2 * AGENT_DAY_MS - AGENT_HR_MS + 4000L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-003",
                title = "Travel advance limit",
                lastMessageMs = AGENT_BASE_MS - 3 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "What is the travel advance limit?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 3 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "The travel advance limit is ₹25,000 per trip for domestic travel. International advances require Finance approval and are capped at ₹80,000.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 3 * AGENT_DAY_MS - AGENT_HR_MS + 3500L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-004",
                title = "How to approve requests",
                lastMessageMs = AGENT_BASE_MS - 4 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "How do I approve a pending request?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 4 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "Go to the Approvals tab, find the pending request, and tap 'Approve' or 'Reject'. You can also add a comment before submitting your decision.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 4 * AGENT_DAY_MS - AGENT_HR_MS + 2800L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-005",
                title = "Card blocking",
                lastMessageMs = AGENT_BASE_MS - 5 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "How do I block my corporate card?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 5 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "Navigate to Account → Cards, tap the card you want to block, and hit 'Block Card'. The block takes effect immediately.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 5 * AGENT_DAY_MS - AGENT_HR_MS + 2000L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-006",
                title = "GPS tracking issue",
                lastMessageMs = AGENT_BASE_MS - 6 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "My trip distance is showing zero, what's wrong?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 6 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "This usually means location permission is set to 'Only while using app'. Please grant 'Always allow' permission in your phone settings so trips record in the background.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 6 * AGENT_DAY_MS - AGENT_HR_MS + 4200L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-007",
                title = "Policy cap alert",
                lastMessageMs = AGENT_BASE_MS - 7 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "I got a policy alert about the ₹10/km cap. What does it mean?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 7 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "The daily mileage cap is ₹10/km. Claims above this threshold are flagged for review. You can still submit them, but your manager will need to manually approve the overage.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 7 * AGENT_DAY_MS - AGENT_HR_MS + 3800L,
                        ),
                    ),
            ),
            AgentConversationStub(
                id = "CONV-008",
                title = "Purchase request status",
                lastMessageMs = AGENT_BASE_MS - 8 * AGENT_DAY_MS,
                messages =
                    listOf(
                        AgentMessageStub(
                            "What is the status of my purchase request PO-2024-002?",
                            isUser = true,
                            timestampMs = AGENT_BASE_MS - 8 * AGENT_DAY_MS - AGENT_HR_MS,
                        ),
                        AgentMessageStub(
                            "PO-2024-002 has been approved by Finance and is pending vendor dispatch. Expected delivery in 3–5 business days.",
                            isUser = false,
                            timestampMs = AGENT_BASE_MS - 8 * AGENT_DAY_MS - AGENT_HR_MS + 5000L,
                        ),
                    ),
            ),
        )

    val popularQuestions: List<PopularQuestionStub> =
        listOf(
            PopularQuestionStub("PQ-001", "What is the mileage reimbursement rate?", "Mileage", askCount = 248, isTrending = true),
            PopularQuestionStub("PQ-002", "How do I submit a GPS-tracked trip?", "Mileage", askCount = 187, isTrending = true),
            PopularQuestionStub("PQ-003", "Why is my trip distance showing zero?", "Mileage", askCount = 143, isTrending = false),
            PopularQuestionStub("PQ-004", "Can I log a manual mileage entry?", "Mileage", askCount = 102, isTrending = false),
            PopularQuestionStub("PQ-005", "What receipts are required for expenses?", "Expense", askCount = 215, isTrending = true),
            PopularQuestionStub("PQ-006", "How long does expense approval take?", "Expense", askCount = 178, isTrending = false),
            PopularQuestionStub("PQ-007", "Why was my expense rejected?", "Expense", askCount = 134, isTrending = false),
            PopularQuestionStub("PQ-008", "How do I resubmit a rejected expense?", "Expense", askCount = 98, isTrending = false),
            PopularQuestionStub("PQ-009", "How do I book a flight via the app?", "Travel", askCount = 162, isTrending = true),
            PopularQuestionStub("PQ-010", "What is the travel advance limit?", "Travel", askCount = 119, isTrending = false),
            PopularQuestionStub("PQ-011", "How do I approve a pending request?", "Approvals", askCount = 204, isTrending = true),
            PopularQuestionStub("PQ-012", "Can I delegate approvals while on leave?", "Approvals", askCount = 88, isTrending = false),
        )

    val unansweredQuestions: List<UnansweredQuestionStub> =
        listOf(
            UnansweredQuestionStub("UQ-001", "Does the app support electric vehicle reimbursement?", "Mileage", askCount = 6),
            UnansweredQuestionStub("UQ-002", "Can I attach multiple receipts to a single expense?", "Expense", askCount = 4),
            UnansweredQuestionStub("UQ-003", "Is international travel booking available?", "Travel", askCount = 5),
            UnansweredQuestionStub("UQ-004", "Can approvals be done in bulk from the mobile app?", "Approvals", askCount = 3),
        )
}
