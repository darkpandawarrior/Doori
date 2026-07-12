package com.mileway.feature.approvals.repository

import com.mileway.core.data.dao.ClarificationDao
import com.mileway.core.data.model.db.ClarificationMessageEntity
import com.mileway.core.data.model.db.ClarificationRoomEntity
import com.mileway.core.data.model.db.ClarificationRoomMetaEntity
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomMeta
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.model.ClarificationRoomSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * PLAN_V28 P28.2/P28.3: the clarification-room store — one room per approval, created lazily on
 * first open, persisted from then on (fixes the previous "resets to a hardcoded seed every time
 * the sheet reopens" gap). Interface + [RoomClarificationRepository] (real, Room-backed) +
 * [FakeClarificationRepository] (in-memory, for tests) — same shape as `MediaRepository`/
 * `FakeMediaRepository` in feature:media.
 */
interface ClarificationRepository {
    fun observeRoom(approvalId: String): Flow<ClarificationRoom?>

    fun observeMessages(roomId: String): Flow<List<ClarificationMessage>>

    fun observeAllRooms(): Flow<List<ClarificationRoom>>

    /** Returns the existing room for [approvalId], or creates + seeds one if this is the first open. */
    suspend fun getOrCreateRoom(
        approvalId: String,
        participants: List<String>,
    ): ClarificationRoom

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        isFromRequester: Boolean,
        text: String,
    )

    /** ACTIVE → CLOSED (P28.3). Idempotent; closing an already-closed room is a no-op. */
    suspend fun closeRoom(roomId: String)

    // PLAN_V28 P28.4: local per-room metadata (isSaved/isPinned/tags/note/reminder).
    fun observeMeta(roomId: String): Flow<ClarificationRoomMeta>

    /** approvalId → true for every room currently marked saved — backs the approvals-list SAVED filter chip. */
    fun observeSavedApprovalIds(): Flow<Set<String>>

    /** activeRooms/totalUnread — backs the approvals nav-hub badge. */
    fun observeRoomSummary(): Flow<ClarificationRoomSummary>

    suspend fun setSaved(
        roomId: String,
        saved: Boolean,
    )

    suspend fun setPinned(
        roomId: String,
        pinned: Boolean,
    )

    suspend fun setTags(
        roomId: String,
        tags: List<String>,
    )

    suspend fun setNote(
        roomId: String,
        note: String,
    )
}

class RoomClarificationRepository(
    private val dao: ClarificationDao,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : ClarificationRepository {
    override fun observeRoom(approvalId: String): Flow<ClarificationRoom?> = dao.observeRoomByApproval(approvalId).map { it?.toDomain() }

    override fun observeMessages(roomId: String): Flow<List<ClarificationMessage>> =
        dao.observeMessages(roomId).map { messages -> messages.map { it.toDomain() } }

    override fun observeAllRooms(): Flow<List<ClarificationRoom>> = dao.observeAllRooms().map { rooms -> rooms.map { it.toDomain() } }

    override suspend fun getOrCreateRoom(
        approvalId: String,
        participants: List<String>,
    ): ClarificationRoom {
        dao.getRoomByApproval(approvalId)?.let { return it.toDomain() }
        val now = nowMs()
        val entity =
            ClarificationRoomEntity(
                roomId = "room_$approvalId",
                approvalId = approvalId,
                status = ClarificationRoomStatus.ACTIVE.name,
                participantsCsv = participants.joinToString(","),
                createdAtMs = now,
                updatedAtMs = now,
            )
        dao.upsertRoom(entity)
        seedMessages(entity.roomId, now).forEach { dao.insertMessage(it) }
        return entity.toDomain()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        roomId: String,
        senderId: String,
        isFromRequester: Boolean,
        text: String,
    ) {
        val now = nowMs()
        dao.insertMessage(
            ClarificationMessageEntity(
                id = "msg_${Uuid.random()}",
                roomId = roomId,
                senderId = senderId,
                isFromRequester = isFromRequester,
                text = text,
                timestampMs = now,
            ),
        )
        // ponytail: no status check here — sending into a CLOSED room shouldn't silently reopen
        // it (P28.3's whole point is a real terminal state); the ViewModel/UI already disables
        // the input once closed, so this is a defence-in-depth no-op, not the primary gate.
        dao.touchUpdatedAt(roomId, now)
    }

    override suspend fun closeRoom(roomId: String) {
        dao.updateStatus(roomId, ClarificationRoomStatus.CLOSED.name, nowMs())
    }

    override fun observeMeta(roomId: String): Flow<ClarificationRoomMeta> =
        dao.observeMeta(roomId).map { it?.toDomain(roomId) ?: ClarificationRoomMeta(roomId) }

    override fun observeSavedApprovalIds(): Flow<Set<String>> =
        combine(dao.observeAllRooms(), dao.observeAllMeta()) { rooms, meta ->
            val savedRoomIds = meta.filter { it.isSaved }.map { it.roomId }.toSet()
            rooms.filter { it.roomId in savedRoomIds }.map { it.approvalId }.toSet()
        }

    override fun observeRoomSummary(): Flow<ClarificationRoomSummary> =
        combine(dao.observeActiveRoomCount(), dao.observeUnreadRoomCount()) { active, unread ->
            ClarificationRoomSummary(activeRooms = active, totalUnread = unread)
        }

    override suspend fun setSaved(
        roomId: String,
        saved: Boolean,
    ) = upsertMeta(roomId) { copy(isSaved = saved) }

    override suspend fun setPinned(
        roomId: String,
        pinned: Boolean,
    ) = upsertMeta(roomId) { copy(isPinned = pinned) }

    override suspend fun setTags(
        roomId: String,
        tags: List<String>,
    ) = upsertMeta(roomId) { copy(tags = tags) }

    override suspend fun setNote(
        roomId: String,
        note: String,
    ) = upsertMeta(roomId) { copy(note = note) }

    private suspend fun upsertMeta(
        roomId: String,
        reducer: ClarificationRoomMeta.() -> ClarificationRoomMeta,
    ) {
        // ponytail: no suspend "get" query on the dao — one-shot read of the observe-Flow's first
        // value instead of adding a parallel @Query just for this read-modify-write.
        val existing = dao.observeMeta(roomId).first()?.toDomain(roomId) ?: ClarificationRoomMeta(roomId)
        dao.upsertMeta(existing.reducer().toEntity())
    }

    /** Same seed content the old hardcoded thread always showed — kept identical for the first open only. */
    private fun seedMessages(
        roomId: String,
        now: Long,
    ): List<ClarificationMessageEntity> =
        listOf(
            ClarificationMessageEntity(
                id = "${roomId}_seed_1",
                roomId = roomId,
                senderId = APPROVER_SENDER_ID,
                isFromRequester = false,
                text = "Hi, could you clarify the purpose of this claim?",
                timestampMs = now - 2 * 3_600_000L,
            ),
            ClarificationMessageEntity(
                id = "${roomId}_seed_2",
                roomId = roomId,
                senderId = REQUESTER_SENDER_ID,
                isFromRequester = true,
                text = "Sure! This was for a client visit to Whitefield office. I have the meeting invite if needed.",
                timestampMs = now - 3_600_000L,
            ),
        )

    private fun ClarificationRoomEntity.toDomain() =
        ClarificationRoom(
            roomId = roomId,
            approvalId = approvalId,
            status = ClarificationRoomStatus.valueOf(status),
            participants = participantsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
        )

    private fun ClarificationMessageEntity.toDomain() =
        ClarificationMessage(
            id = id,
            roomId = roomId,
            senderId = senderId,
            text = text,
            isFromRequester = isFromRequester,
            timestampMs = timestampMs,
        )

    private fun ClarificationRoomMetaEntity.toDomain(roomId: String) =
        ClarificationRoomMeta(
            roomId = roomId,
            isSaved = isSaved,
            isPinned = isPinned,
            tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            note = note,
            reminderAtMs = reminderAtMs,
        )

    private fun ClarificationRoomMeta.toEntity() =
        ClarificationRoomMetaEntity(
            roomId = roomId,
            isSaved = isSaved,
            isPinned = isPinned,
            tagsCsv = tags.joinToString(","),
            note = note,
            reminderAtMs = reminderAtMs,
        )
}

/** Sender ids for the two roles a room ever has — display names are P28.6 (deferred). */
const val APPROVER_SENDER_ID = "approver"
const val REQUESTER_SENDER_ID = "requester"
