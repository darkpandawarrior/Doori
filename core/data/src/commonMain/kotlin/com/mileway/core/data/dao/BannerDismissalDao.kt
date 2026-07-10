package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.BannerDismissedEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P13.1: the per-account persisted banner-dismissal set — see [BannerDismissedEntity]. */
@Dao
interface BannerDismissalDao {
    @Query("SELECT bannerId FROM banners_dismissed WHERE accountId = :accountId")
    fun observeDismissedIds(accountId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BannerDismissedEntity)
}
