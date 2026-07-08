package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P4.1: the persisted verification-documents store — see [DocumentEntity]. */
@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY category ASC, docType ASC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int

    @Query("SELECT * FROM documents WHERE docType = :docType LIMIT 1")
    suspend fun get(docType: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentEntity)
}
