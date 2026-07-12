package com.mileway.feature.approvals.repository

import com.mileway.core.data.dao.ClarificationDao
import com.mileway.core.data.model.db.ClarificationMessageEntity
import com.mileway.core.data.model.db.ClarificationRoomEntity
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import kotlinx.coroutines.flow.Flow
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
}

/** Sender ids for the two roles a room ever has — display names are P28.6 (deferred). */
const val APPROVER_SENDER_ID = "approver"
const val REQUESTER_SENDER_ID = "requester"
