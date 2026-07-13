package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.BugReportEntity
import kotlinx.coroutines.flow.Flow

/** P31.MISC.1: the persisted shake-to-report store — see [BugReportEntity]. */
@Dao
interface BugReportDao {
    @Query("SELECT * FROM bug_reports ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<BugReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: BugReportEntity)
}
