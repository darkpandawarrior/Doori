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

    @Query("SELECT * FROM statement_imports WHERE id = :id")
    suspend fun get(id: String): StatementImportEntity?

    @Query("SELECT cardMatchId FROM claim_lines WHERE cardMatchId IS NOT NULL")
    suspend fun matchedTransactionIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StatementImportEntity)
}
