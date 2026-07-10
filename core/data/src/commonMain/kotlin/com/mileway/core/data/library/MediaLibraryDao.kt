package com.mileway.core.data.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaLibraryDao {
    /** Excludes soft-deleted entries — the library grid's source of truth. */
    @Query("SELECT * FROM media_library WHERE isDeleted = 0 ORDER BY savedAtMs DESC")
    fun observeAll(): Flow<List<MediaLibraryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MediaLibraryEntry)

    /** Hard delete — not wired to any UI yet; kept for a future permanent-purge action. */
    @Delete
    suspend fun delete(entry: MediaLibraryEntry)

    @Query("UPDATE media_library SET isDeleted = 1, deletedAt = :deletedAtMs WHERE id = :id")
    suspend fun softDelete(
        id: String,
        deletedAtMs: Long,
    )

    @Query("UPDATE media_library SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("UPDATE media_library SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE media_library SET lastAccessedAt = :accessedAtMs WHERE id = :id")
    suspend fun touchLastAccessed(
        id: String,
        accessedAtMs: Long,
    )
}
