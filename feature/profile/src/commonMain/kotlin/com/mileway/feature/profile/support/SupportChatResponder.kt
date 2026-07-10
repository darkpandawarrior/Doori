package com.mileway.feature.profile.support

/**
 * PLAN_V24 P12.2 — the deterministic support-chat matcher. The reference app's support "chat" channel
 * was a live agent bridge; Mileway has no backend, so this is a pure keyword classifier that maps a
 * user message to a canned-reply [SupportChatTopic]. The UI renders each topic's localized reply.
 *
 * ponytail: rule-based first-keyword-wins matcher — no NLP, no history/context, no persistence. It is
 * intentionally shallow: enough for a believable offline help bot. Upgrade path if this ever needs to
 * be smart: swap this classifier for an on-device model behind the same [classify] seam.
 */
enum class SupportChatTopic { GREETING, TRACKING, EXPENSE, ACCOUNT, PERMISSIONS, TICKET, THANKS, FALLBACK }

object SupportChatResponder {
    private val rules: List<Pair<SupportChatTopic, List<String>>> =
        listOf(
            SupportChatTopic.GREETING to listOf("hi", "hello", "hey", "good morning", "good evening"),
            SupportChatTopic.THANKS to listOf("thanks", "thank you", "cheers", "appreciate"),
            // PERMISSIONS before TRACKING: "location permission" is a permissions question even though
            // "location" is also a tracking keyword — the more specific permission words win.
            SupportChatTopic.PERMISSIONS to listOf("permission", "background", "battery", "notification", "allow"),
            SupportChatTopic.TRACKING to listOf("track", "gps", "location", "trip", "journey", "accuracy", "bubble"),
            SupportChatTopic.EXPENSE to listOf("expense", "claim", "reimburse", "voucher", "submit", "amount"),
            SupportChatTopic.ACCOUNT to listOf("account", "login", "sign in", "password", "profile", "switch"),
            SupportChatTopic.TICKET to listOf("ticket", "agent", "human", "call", "email", "complaint", "raise"),
        )

    /** Classifies [message] into a topic; the first rule whose keyword the message contains wins. */
    fun classify(message: String): SupportChatTopic {
        val text = message.lowercase().trim()
        if (text.isEmpty()) return SupportChatTopic.FALLBACK
        for ((topic, keywords) in rules) {
            if (keywords.any { text.contains(it) }) return topic
        }
        return SupportChatTopic.FALLBACK
    }
}
