package com.mileway.feature.profile.support

import kotlin.test.Test
import kotlin.test.assertEquals

/** PLAN_V24 P12.2 — the deterministic support-chat classifier. */
class SupportChatResponderTest {
    @Test
    fun `greetings are recognised`() {
        assertEquals(SupportChatTopic.GREETING, SupportChatResponder.classify("Hi there"))
        assertEquals(SupportChatTopic.GREETING, SupportChatResponder.classify("hello"))
    }

    @Test
    fun `tracking questions classify to tracking`() {
        assertEquals(SupportChatTopic.TRACKING, SupportChatResponder.classify("Why is my GPS accuracy low?"))
        assertEquals(SupportChatTopic.TRACKING, SupportChatResponder.classify("how do I start a trip"))
    }

    @Test
    fun `expense questions classify to expense`() {
        assertEquals(SupportChatTopic.EXPENSE, SupportChatResponder.classify("How do I submit a claim?"))
    }

    @Test
    fun `permission questions classify to permissions`() {
        assertEquals(SupportChatTopic.PERMISSIONS, SupportChatResponder.classify("background location permission"))
    }

    @Test
    fun `escalation words classify to ticket`() {
        assertEquals(SupportChatTopic.TICKET, SupportChatResponder.classify("I need to talk to a human agent"))
    }

    @Test
    fun `empty and unknown messages fall back`() {
        assertEquals(SupportChatTopic.FALLBACK, SupportChatResponder.classify(""))
        assertEquals(SupportChatTopic.FALLBACK, SupportChatResponder.classify("xyzzy plugh"))
    }

    @Test
    fun `classification is deterministic across repeated calls`() {
        val m = "gps trip tracking"
        assertEquals(SupportChatResponder.classify(m), SupportChatResponder.classify(m))
    }
}
