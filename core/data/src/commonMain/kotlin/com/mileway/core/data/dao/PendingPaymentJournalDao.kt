package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PendingPaymentJournalEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [PendingPaymentJournalEntity] store — payout journal entries queued for export. */
@Dao
interface PendingPaymentJournalDao {
    @Query("SELECT * FROM pending_payment_journals WHERE status = :status")
    fun observeByStatus(status: String): Flow<List<PendingPaymentJournalEntity>>

    @Query("SELECT * FROM pending_payment_journals WHERE reportId = :reportId")
    suspend fun getByReport(reportId: String): List<PendingPaymentJournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingPaymentJournalEntity)
}
