package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PluginOverrideEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P0.1: per-account plugin overrides (the USER layer) — see [PluginOverrideEntity]. */
@Dao
interface PluginOverrideDao {
    @Query("SELECT * FROM plugin_overrides WHERE accountId = :accountId")
    fun observeForAccount(accountId: String): Flow<List<PluginOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PluginOverrideEntity)

    @Query("DELETE FROM plugin_overrides WHERE accountId = :accountId AND pluginId = :pluginId")
    suspend fun delete(
        accountId: String,
        pluginId: String,
    )

    @Query("DELETE FROM plugin_overrides WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
