package com.miletracker

import com.miletracker.core.data.dao.AgentDao
import com.miletracker.core.data.model.db.AgentConversationEntity
import com.miletracker.core.data.model.db.AgentMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeAgentDao : AgentDao {
    private val conversations = LinkedHashMap<String, AgentConversationEntity>()
    private val messages = LinkedHashMap<String, AgentMessageEntity>()
    private val _convFlow = MutableStateFlow<List<AgentConversationEntity>>(emptyList())
    private val _msgFlow = MutableStateFlow<List<AgentMessageEntity>>(emptyList())

    private fun flush() {
        _convFlow.value = conversations.values.sortedByDescending { it.lastMessageMs }
        _msgFlow.value = messages.values.toList()
    }

    override suspend fun insertConversation(conversation: AgentConversationEntity) {
        conversations[conversation.id] = conversation
        flush()
    }

    override fun observeConversations(): Flow<List<AgentConversationEntity>> = _convFlow.asStateFlow()

    override suspend fun updateConversationMeta(id: String, title: String, lastMessageMs: Long) {
        conversations[id]?.let { conversations[id] = it.copy(title = title, lastMessageMs = lastMessageMs) }
        flush()
    }

    override suspend fun deleteConversation(id: String) {
        conversations.remove(id)
        flush()
    }

    override suspend fun countConversations(): Int = conversations.size

    override suspend fun insertMessage(message: AgentMessageEntity) {
        messages[message.messageId] = message
        flush()
    }

    override fun observeMessages(conversationId: String): Flow<List<AgentMessageEntity>> =
        _msgFlow.map { list ->
            list.filter { it.conversationId == conversationId }.sortedBy { it.timestampMs }
        }

    override suspend fun updateMessage(message: AgentMessageEntity) {
        messages[message.messageId] = message
        flush()
    }

    override suspend fun updateFeedback(messageId: String, rating: Int, comment: String?) {
        messages[messageId]?.let { messages[messageId] = it.copy(feedbackRating = rating, feedbackComment = comment) }
        flush()
    }

    override suspend fun updateLastMessageTime(id: String, lastMessageMs: Long) {
        conversations[id]?.let { conversations[id] = it.copy(lastMessageMs = lastMessageMs) }
        flush()
    }
}
