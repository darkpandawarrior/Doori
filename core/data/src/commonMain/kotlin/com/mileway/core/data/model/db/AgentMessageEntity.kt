package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_messages",
    foreignKeys = [
        ForeignKey(
            entity = AgentConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId")],
)
data class AgentMessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val text: String,
    val isUser: Boolean,
    val timestampMs: Long,
    val feedbackRating: Int? = null,
    val feedbackComment: String? = null,
    val module: String? = null,
    val isContextReset: Boolean = false,
)
