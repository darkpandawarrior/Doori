package com.mileway.feature.approvals.repository

import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Fully offline, deterministic in-memory [ClarificationRepository] — same tone as feature:media's
 * `FakeMediaRepository`. Used by feature:approvals' own ViewModel tests and by `:app`'s
 * screenshot/Koin-graph test doubles (no seed messages, unlike the real repo — tests seed their
 * own fixtures explicitly).
 */
class FakeClarificationRepository : ClarificationRepository {
    private val rooms = MutableStateFlow<Map<String, ClarificationRoom>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, List<ClarificationMessage>>>(emptyMap())

    override fun observeRoom(approvalId: String): Flow<ClarificationRoom?> = rooms.map { it.values.firstOrNull { r -> r.approvalId == approvalId } }

    override fun observeMessages(roomId: String): Flow<List<ClarificationMessage>> = messages.map { it[roomId].orEmpty() }

    override fun observeAllRooms(): Flow<List<ClarificationRoom>> = rooms.map { it.values.sortedByDescending { r -> r.updatedAtMs } }

    override suspend fun getOrCreateRoom(
        approvalId: String,
        participants: List<String>,
    ): ClarificationRoom {
        rooms.value.values.firstOrNull { it.approvalId == approvalId }?.let { return it }
        val room =
            ClarificationRoom(
                roomId = "room_$approvalId",
                approvalId = approvalId,
                status = ClarificationRoomStatus.ACTIVE,
                participants = participants,
                createdAtMs = 0L,
                updatedAtMs = 0L,
            )
        rooms.value = rooms.value + (room.roomId to room)
        return room
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        roomId: String,
        senderId: String,
        isFromRequester: Boolean,
        text: String,
    ) {
        val next = (messages.value[roomId].orEmpty()) + ClarificationMessage(Uuid.random().toString(), roomId, senderId, text, isFromRequester, 0L)
        messages.value = messages.value + (roomId to next)
    }

    override suspend fun closeRoom(roomId: String) {
        val room = rooms.value[roomId]?.copy(status = ClarificationRoomStatus.CLOSED) ?: return
        rooms.value = rooms.value + (roomId to room)
    }
}
