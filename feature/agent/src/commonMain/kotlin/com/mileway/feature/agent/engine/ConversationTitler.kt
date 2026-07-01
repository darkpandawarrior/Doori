package com.mileway.feature.agent.engine

internal object ConversationTitler {
    fun title(firstUserMessage: String): String {
        val cleaned = firstUserMessage.trim().trimEnd('?', '!')
        return if (cleaned.length <= 50) cleaned else cleaned.take(47) + "…"
    }
}
