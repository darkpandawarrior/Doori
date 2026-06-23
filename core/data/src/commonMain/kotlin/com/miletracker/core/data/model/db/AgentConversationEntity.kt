package com.miletracker.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_conversations")
data class AgentConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessageMs: Long,
    val createdAtMs: Long,
)
