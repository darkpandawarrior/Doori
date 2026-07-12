package com.mileway

import com.mileway.core.data.dao.ClarificationDao
import com.mileway.core.data.model.db.ClarificationMessageEntity
import com.mileway.core.data.model.db.ClarificationRoomEntity
import com.mileway.core.data.model.db.ClarificationRoomMetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [ClarificationDao] (PLAN_V28 P28.2) — lets JVM/Robolectric Koin-graph and
 * screenshot tests construct `ApprovalsViewModel`/`ClarificationHistoryViewModel` without a Room
 * instance. A relaxed mockk would hand back a null-backed Flow and crash the eager `combine`/
 * `collect` in `ApprovalsViewModel.openDetail`/`ClarificationHistoryViewModel.init` (memory:
 * screenshot Koin needs deterministic fakes), so a real MutableStateFlow is used instead.
 */
class FakeClarificationDao : ClarificationDao {
    private val rooms = MutableStateFlow<Map<String, ClarificationRoomEntity>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, ClarificationMessageEntity>>(emptyMap())
    private val meta = MutableStateFlow<Map<String, ClarificationRoomMetaEntity>>(emptyMap())

    override fun observeRoomByApproval(approvalId: String): Flow<ClarificationRoomEntity?> =
        rooms.map { it.values.firstOrNull { r -> r.approvalId == approvalId } }

    override suspend fun getRoomByApproval(approvalId: String): ClarificationRoomEntity? = rooms.value.values.firstOrNull { it.approvalId == approvalId }

    override fun observeAllRooms(): Flow<List<ClarificationRoomEntity>> = rooms.map { it.values.sortedByDescending { r -> r.updatedAtMs } }

    override suspend fun upsertRoom(room: ClarificationRoomEntity) {
        rooms.value = rooms.value + (room.roomId to room)
    }

    override suspend fun updateStatus(
        roomId: String,
        status: String,
        updatedAtMs: Long,
    ) {
        rooms.value[roomId]?.let { rooms.value = rooms.value + (roomId to it.copy(status = status, updatedAtMs = updatedAtMs)) }
    }

    override suspend fun touchUpdatedAt(
        roomId: String,
        updatedAtMs: Long,
    ) {
        rooms.value[roomId]?.let { rooms.value = rooms.value + (roomId to it.copy(updatedAtMs = updatedAtMs)) }
    }

    override fun observeMessages(roomId: String): Flow<List<ClarificationMessageEntity>> =
        messages.map { it.values.filter { m -> m.roomId == roomId }.sortedBy { m -> m.timestampMs } }

    override suspend fun insertMessage(message: ClarificationMessageEntity) {
        messages.value = messages.value + (message.id to message)
    }

    override fun observeMeta(roomId: String): Flow<ClarificationRoomMetaEntity?> = meta.map { it[roomId] }

    override fun observeAllMeta(): Flow<List<ClarificationRoomMetaEntity>> = meta.map { it.values.toList() }

    override suspend fun upsertMeta(meta: ClarificationRoomMetaEntity) {
        this.meta.value = this.meta.value + (meta.roomId to meta)
    }

    override fun observeActiveRoomCount(): Flow<Int> = rooms.map { it.values.count { r -> r.status == "ACTIVE" } }

    override fun observeUnreadRoomCount(): Flow<Int> =
        kotlinx.coroutines.flow.combine(rooms, messages) { roomMap, messageMap ->
            roomMap.values.count { r ->
                r.status == "ACTIVE" &&
                    messageMap.values.filter { m -> m.roomId == r.roomId }.maxByOrNull { m -> m.timestampMs }?.isFromRequester == true
            }
        }
}
