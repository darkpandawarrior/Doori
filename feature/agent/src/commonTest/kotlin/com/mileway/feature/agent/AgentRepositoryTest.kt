package com.mileway.feature.agent

import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.model.db.AgentConversationEntity
import com.mileway.core.data.model.db.AgentMessageEntity
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.feature.agent.repository.AgentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeAgentDao : AgentDao {
    val conversations = LinkedHashMap<String, AgentConversationEntity>()
    val messages = LinkedHashMap<String, AgentMessageEntity>()
    private val _convFlow = MutableStateFlow<List<AgentConversationEntity>>(emptyList())
    private val _msgFlow = MutableStateFlow<List<AgentMessageEntity>>(emptyList())

    private fun flush() {
        _convFlow.value = conversations.values.sortedByDescending { it.lastMessageMs }
        _msgFlow.value = messages.values.toList()
    }

    override suspend fun insertConversation(conversation: AgentConversationEntity) {
        conversations[conversation.id] = conversation; flush()
    }
    override fun observeConversations(): Flow<List<AgentConversationEntity>> = _convFlow.asStateFlow()
    override suspend fun updateConversationMeta(id: String, title: String, lastMessageMs: Long) {
        conversations[id]?.let { conversations[id] = it.copy(title = title, lastMessageMs = lastMessageMs) }; flush()
    }
    override suspend fun deleteConversation(id: String) { conversations.remove(id); flush() }
    override suspend fun countConversations(): Int = conversations.size
    override suspend fun insertMessage(message: AgentMessageEntity) { messages[message.messageId] = message; flush() }
    override fun observeMessages(conversationId: String): Flow<List<AgentMessageEntity>> =
        _msgFlow.map { list -> list.filter { it.conversationId == conversationId }.sortedBy { it.timestampMs } }
    override suspend fun updateMessage(message: AgentMessageEntity) { messages[message.messageId] = message; flush() }
    override suspend fun updateFeedback(messageId: String, rating: Int, comment: String?) {
        messages[messageId]?.let { messages[messageId] = it.copy(feedbackRating = rating, feedbackComment = comment) }; flush()
    }
    override suspend fun updateLastMessageTime(id: String, lastMessageMs: Long) {
        conversations[id]?.let { conversations[id] = it.copy(lastMessageMs = lastMessageMs) }; flush()
    }
}

private class FakeAgentSessionStore : AgentSessionStore {
    private var stored: Pair<String, Long>? = null
    override suspend fun getActiveThread(): Pair<String, Long>? = stored
    override suspend fun setActiveThread(threadId: String, nowMs: Long) { stored = threadId to nowMs }
    override suspend fun clearActiveThread() { stored = null }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

class AgentRepositoryTest {

    private fun buildRepo(): Pair<AgentRepository, FakeAgentDao> {
        val dao = FakeAgentDao()
        return AgentRepository(dao, FakeAgentSessionStore()) to dao
    }

    @Test
    fun `createThread inserts conversation in DAO`() = runTest {
        val (repo, dao) = buildRepo()
        repo.createThread("t1", "Test thread", 1_000L)
        assertNotNull(dao.conversations["t1"])
        assertEquals("Test thread", dao.conversations["t1"]!!.title)
    }

    @Test
    fun `appendMessage inserts message under correct conversation`() = runTest {
        val (repo, dao) = buildRepo()
        repo.createThread("t1", "T", 1_000L)
        repo.appendMessage("t1", "m1", "Hello", isUser = true, 2_000L)
        assertNotNull(dao.messages["m1"])
        assertEquals("t1", dao.messages["m1"]!!.conversationId)
        assertTrue(dao.messages["m1"]!!.isUser)
    }

    @Test
    fun `messagesFor returns only messages for requested conversation`() = runTest {
        val (repo, dao) = buildRepo()
        repo.createThread("t1", "T", 1_000L)
        repo.createThread("t2", "U", 2_000L)
        repo.appendMessage("t1", "m1", "A", true, 3_000L)
        repo.appendMessage("t2", "m2", "B", true, 4_000L)
        val msgs = repo.messagesFor("t1").first()
        assertEquals(1, msgs.size)
        assertEquals("A", msgs[0].text)
    }

    @Test
    fun `conversationsFlow emits after createThread`() = runTest {
        val (repo) = buildRepo()
        repo.createThread("t1", "Title", 1_000L)
        val conversations = repo.conversationsFlow.first()
        assertEquals(1, conversations.size)
        assertEquals("t1", conversations[0].id)
    }

    @Test
    fun `getResumableThread returns null when no active thread`() = runTest {
        val (repo) = buildRepo()
        val result = repo.getResumableThread(1_700_000_000_000L)
        assertNull(result)
    }

    @Test
    fun `getResumableThread returns thread id within resume window`() = runTest {
        val (repo) = buildRepo()
        val now = 1_700_000_000_000L
        repo.createThread("t1", "T", now - 1000L)
        repo.setActiveThread("t1", now - 1000L)
        val result = repo.getResumableThread(now)
        assertEquals("t1", result)
    }

    @Test
    fun `persistFeedback updates message rating in DAO`() = runTest {
        val (repo, dao) = buildRepo()
        repo.createThread("t1", "T", 1_000L)
        repo.appendMessage("t1", "m1", "Good answer", isUser = false, 2_000L)
        repo.persistFeedback("m1", rating = 1, comment = null)
        assertEquals(1, dao.messages["m1"]!!.feedbackRating)
    }

    @Test
    fun `seedIfEmpty does not crash when no conversations exist`() = runTest {
        val (repo) = buildRepo()
        repo.seedIfEmpty()
    }
}
