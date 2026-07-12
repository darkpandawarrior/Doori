package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ClarificationMessageEntity
import com.mileway.core.data.model.db.ClarificationRoomEntity
import com.mileway.core.data.model.db.ClarificationRoomMetaEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V28 P28.2: the persisted clarification-room store — see [ClarificationRoomEntity]. */
@Dao
interface ClarificationDao {
    @Query("SELECT * FROM clarification_rooms WHERE approvalId = :approvalId LIMIT 1")
    fun observeRoomByApproval(approvalId: String): Flow<ClarificationRoomEntity?>

    @Query("SELECT * FROM clarification_rooms WHERE approvalId = :approvalId LIMIT 1")
    suspend fun getRoomByApproval(approvalId: String): ClarificationRoomEntity?

    @Query("SELECT * FROM clarification_rooms ORDER BY updatedAtMs DESC")
    fun observeAllRooms(): Flow<List<ClarificationRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoom(room: ClarificationRoomEntity)

    @Query("UPDATE clarification_rooms SET status = :status, updatedAtMs = :updatedAtMs WHERE roomId = :roomId")
    suspend fun updateStatus(
        roomId: String,
        status: String,
        updatedAtMs: Long,
    )

    @Query("UPDATE clarification_rooms SET updatedAtMs = :updatedAtMs WHERE roomId = :roomId")
    suspend fun touchUpdatedAt(
        roomId: String,
        updatedAtMs: Long,
    )

    @Query("SELECT * FROM clarification_messages WHERE roomId = :roomId ORDER BY timestampMs ASC")
    fun observeMessages(roomId: String): Flow<List<ClarificationMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ClarificationMessageEntity)

    // PLAN_V28 P28.4: per-room local metadata (isSaved/isPinned/tags/note/reminder).
    @Query("SELECT * FROM clarification_room_meta WHERE roomId = :roomId")
    fun observeMeta(roomId: String): Flow<ClarificationRoomMetaEntity?>

    @Query("SELECT * FROM clarification_room_meta")
    fun observeAllMeta(): Flow<List<ClarificationRoomMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: ClarificationRoomMetaEntity)

    // PLAN_V28 P28.4: room-summary aggregation for the nav-hub badge. "Unread" has no dedicated
    // read-receipt column (out of scope here) — an ACTIVE room counts as unread-for-the-approver
    // when its most recent message came from the requester, i.e. it's awaiting an approver reply.
    @Query("SELECT COUNT(*) FROM clarification_rooms WHERE status = 'ACTIVE'")
    fun observeActiveRoomCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM clarification_rooms r
        WHERE r.status = 'ACTIVE' AND (
            SELECT m.isFromRequester FROM clarification_messages m
            WHERE m.roomId = r.roomId ORDER BY m.timestampMs DESC LIMIT 1
        ) = 1
        """,
    )
    fun observeUnreadRoomCount(): Flow<Int>
}
