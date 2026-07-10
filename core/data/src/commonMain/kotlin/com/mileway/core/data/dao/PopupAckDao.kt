package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PopupAckEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P13.3: the per-account persisted forced-popup acknowledgements — see [PopupAckEntity]. */
@Dao
interface PopupAckDao {
    @Query("SELECT popupId FROM popup_acks WHERE accountId = :accountId")
    fun observeAcknowledgedIds(accountId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PopupAckEntity)
}
