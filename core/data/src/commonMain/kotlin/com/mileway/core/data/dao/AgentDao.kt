package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mileway.core.data.model.db.AgentConversationEntity
import com.mileway.core.data.model.db.AgentMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    // Conversations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AgentConversationEntity)

    @Query("SELECT * FROM agent_conversations ORDER BY lastMessageMs DESC")
    fun observeConversations(): Flow<List<AgentConversationEntity>>

    @Query("UPDATE agent_conversations SET title = :title, lastMessageMs = :lastMessageMs WHERE id = :id")
    suspend fun updateConversationMeta(id: String, title: String, lastMessageMs: Long)

    @Query("DELETE FROM agent_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT COUNT(*) FROM agent_conversations")
    suspend fun countConversations(): Int

    // Messages

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity)

    @Query("SELECT * FROM agent_messages WHERE conversationId = :conversationId ORDER BY timestampMs ASC")
    fun observeMessages(conversationId: String): Flow<List<AgentMessageEntity>>

    @Update
    suspend fun updateMessage(message: AgentMessageEntity)

    @Query("UPDATE agent_messages SET feedbackRating = :rating, feedbackComment = :comment WHERE messageId = :messageId")
    suspend fun updateFeedback(messageId: String, rating: Int, comment: String?)

    @Query("UPDATE agent_conversations SET lastMessageMs = :lastMessageMs WHERE id = :id")
    suspend fun updateLastMessageTime(id: String, lastMessageMs: Long)
}
