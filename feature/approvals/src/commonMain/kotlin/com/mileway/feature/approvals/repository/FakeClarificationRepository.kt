package com.mileway.feature.approvals.repository

import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomMeta
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.model.ClarificationRoomSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val meta = MutableStateFlow<Map<String, ClarificationRoomMeta>>(emptyMap())

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
        senderName: String?,
        senderRole: String?,
        attachmentUrl: String?,
    ) {
        val message =
            ClarificationMessage(
                id = Uuid.random().toString(),
                roomId = roomId,
                senderId = senderId,
                text = text,
                isFromRequester = isFromRequester,
                timestampMs = 0L,
                senderName = senderName,
                senderRole = senderRole,
                attachmentUrl = attachmentUrl,
            )
        val next = (messages.value[roomId].orEmpty()) + message
        messages.value = messages.value + (roomId to next)
    }

    override suspend fun closeRoom(roomId: String) {
        val room = rooms.value[roomId]?.copy(status = ClarificationRoomStatus.CLOSED) ?: return
        rooms.value = rooms.value + (roomId to room)
    }

    override fun observeMeta(roomId: String): Flow<ClarificationRoomMeta> = meta.map { it[roomId] ?: ClarificationRoomMeta(roomId) }

    override fun observeSavedApprovalIds(): Flow<Set<String>> =
        combine(rooms, meta) { roomMap, metaMap ->
            roomMap.values.filter { metaMap[it.roomId]?.isSaved == true }.map { it.approvalId }.toSet()
        }

    override fun observeRoomSummary(): Flow<ClarificationRoomSummary> =
        combine(rooms, messages) { roomMap, messageMap ->
            val active = roomMap.values.count { it.status == ClarificationRoomStatus.ACTIVE }
            val unread =
                roomMap.values.count { room ->
                    room.status == ClarificationRoomStatus.ACTIVE && messageMap[room.roomId]?.lastOrNull()?.isFromRequester == true
                }
            ClarificationRoomSummary(activeRooms = active, totalUnread = unread)
        }

    override suspend fun setSaved(
        roomId: String,
        saved: Boolean,
    ) = updateMeta(roomId) { copy(isSaved = saved) }

    override suspend fun setPinned(
        roomId: String,
        pinned: Boolean,
    ) = updateMeta(roomId) { copy(isPinned = pinned) }

    override suspend fun setTags(
        roomId: String,
        tags: List<String>,
    ) = updateMeta(roomId) { copy(tags = tags) }

    override suspend fun setNote(
        roomId: String,
        note: String,
    ) = updateMeta(roomId) { copy(note = note) }

    private fun updateMeta(
        roomId: String,
        reducer: ClarificationRoomMeta.() -> ClarificationRoomMeta,
    ) {
        val existing = meta.value[roomId] ?: ClarificationRoomMeta(roomId)
        meta.value = meta.value + (roomId to existing.reducer())
    }
}
