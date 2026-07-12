package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** PLAN_V28 P28.2: one message in a [ClarificationRoomEntity]'s thread. Mirrors [AgentMessageEntity]'s shape. */
@Entity(
    tableName = "clarification_messages",
    foreignKeys = [
        ForeignKey(
            entity = ClarificationRoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("roomId")],
)
data class ClarificationMessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val isFromRequester: Boolean,
    val text: String,
    val timestampMs: Long,
    // PLAN_V28 P28.6: display name/role for the chat bubble header + a picked attachment's local
    // URI (core:media's AttachmentItem.uri — no new attachment model). All nullable/additive.
    val senderName: String? = null,
    val senderRole: String? = null,
    val attachmentUrl: String? = null,
)
