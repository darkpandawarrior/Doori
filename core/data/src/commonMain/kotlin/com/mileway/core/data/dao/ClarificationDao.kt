package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ClarificationMessageEntity
import com.mileway.core.data.model.db.ClarificationRoomEntity
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
}
