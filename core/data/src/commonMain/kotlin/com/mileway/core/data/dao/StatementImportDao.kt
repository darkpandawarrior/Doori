package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.StatementImportEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [StatementImportEntity] store — one row per imported card-statement batch. */
@Dao
interface StatementImportDao {
    @Query("SELECT * FROM statement_imports ORDER BY importedAtMs DESC")
    fun observeAll(): Flow<List<StatementImportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StatementImportEntity)
}
