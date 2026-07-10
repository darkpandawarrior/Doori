package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.SignatureEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P12.7: the persisted digital-signature singleton row — see [SignatureEntity]. */
@Dao
interface SignatureDao {
    @Query("SELECT * FROM signature WHERE id = :id LIMIT 1")
    fun observe(id: String = SignatureEntity.SINGLETON_ID): Flow<SignatureEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SignatureEntity)

    @Query("DELETE FROM signature WHERE id = :id")
    suspend fun clear(id: String = SignatureEntity.SINGLETON_ID)
}
