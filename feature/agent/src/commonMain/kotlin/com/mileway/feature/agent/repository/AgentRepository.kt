package com.mileway.feature.agent.repository

import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.model.db.AgentConversationEntity
import com.mileway.core.data.model.db.AgentMessageEntity
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.feature.agent.model.AgentConversation
import com.mileway.feature.agent.model.AgentMessage
import com.mileway.feature.agent.model.PopularQuestion
import com.mileway.feature.agent.model.UnansweredQuestion
import com.mileway.stub.AgentMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SESSION_RESUME_WINDOW_MS = 5 * 60 * 1000L

class AgentRepository(private val agentDao: AgentDao, private val sessionStore: AgentSessionStore) {

    val conversationsFlow: Flow<List<AgentConversation>> =
        agentDao.observeConversations().map { entities ->
            entities.map { entity ->
                AgentConversation(
                    id = entity.id,
                    title = entity.title,
                    lastMessageMs = entity.lastMessageMs,
                    messages = emptyList(),
                )
            }
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

    fun messagesFor(conversationId: String): Flow<List<AgentMessage>> =
        agentDao.observeMessages(conversationId).map { entities ->
            entities.map { entity ->
                AgentMessage(text = entity.text, isUser = entity.isUser, timestampMs = entity.timestampMs)
            }
        }

    suspend fun createThread(id: String, title: String, nowMs: Long) {
        agentDao.insertConversation(
            AgentConversationEntity(id = id, title = title, lastMessageMs = nowMs, createdAtMs = nowMs),
        )
    }

    suspend fun appendMessage(
        conversationId: String,
        messageId: String,
        text: String,
        isUser: Boolean,
        timestampMs: Long,
    ) {
        agentDao.insertMessage(
            AgentMessageEntity(
                messageId = messageId,
                conversationId = conversationId,
                text = text,
                isUser = isUser,
                timestampMs = timestampMs,
            ),
        )
        agentDao.updateLastMessageTime(conversationId, timestampMs)
    }

    suspend fun updateTitle(id: String, title: String, lastMessageMs: Long) {
        agentDao.updateConversationMeta(id, title, lastMessageMs)
    }

    suspend fun setActiveThread(threadId: String, nowMs: Long) {
        sessionStore.setActiveThread(threadId, nowMs)
    }

    suspend fun getResumableThread(nowMs: Long): String? {
        val (id, lastMs) = sessionStore.getActiveThread() ?: return null
        return if (nowMs - lastMs <= SESSION_RESUME_WINDOW_MS) id else null
    }

    suspend fun persistFeedback(messageId: String, rating: Int, comment: String?) {
        agentDao.updateFeedback(messageId, rating, comment)
    }

    suspend fun clearSession() {
        sessionStore.clearActiveThread()
    }

    suspend fun seedIfEmpty() {
        if (agentDao.countConversations() > 0) return
        AgentMockData.conversations.forEach { stub ->
            agentDao.insertConversation(
                AgentConversationEntity(
                    id = stub.id,
                    title = stub.title,
                    lastMessageMs = stub.lastMessageMs,
                    createdAtMs = stub.lastMessageMs,
                ),
            )
            stub.messages.forEach { msg ->
                agentDao.insertMessage(
                    AgentMessageEntity(
                        messageId = "${stub.id}_${msg.timestampMs}",
                        conversationId = stub.id,
                        text = msg.text,
                        isUser = msg.isUser,
                        timestampMs = msg.timestampMs,
                    ),
                )
            }
        }
    }
}
