package com.mileway.feature.agent.model

data class AgentMessage(
    val text: String,
    val isUser: Boolean,
    val timestampMs: Long,
    val id: String = "${if (isUser) "u" else "a"}_$timestampMs",
)

data class AgentConversation(
    val id: String,
    val title: String,
    val lastMessageMs: Long,
    val messages: List<AgentMessage>,
)

data class PopularQuestion(
    val id: String,
    val question: String,
    val module: String,
    val askCount: Int,
    val isTrending: Boolean,
)

data class UnansweredQuestion(
    val id: String,
    val question: String,
    val module: String,
    val askCount: Int,
)
