package com.mileway.feature.agent.engine

internal object ConversationTitler {
    /** A conversation title is never longer than this; shorter messages are used verbatim. */
    internal const val MaxTitleLength = 50

    /** Where a longer message is cut, before the ellipsis is appended. */
    private const val TruncatedTitleLength = 47

    fun title(firstUserMessage: String): String {
        val cleaned = firstUserMessage.trim().trimEnd('?', '!')
        return if (cleaned.length <= MaxTitleLength) cleaned else cleaned.take(TruncatedTitleLength) + "…"
    }
}
