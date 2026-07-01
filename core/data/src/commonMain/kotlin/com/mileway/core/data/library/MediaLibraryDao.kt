package com.mileway.core.data.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaLibraryDao {
    @Query("SELECT * FROM media_library ORDER BY savedAtMs DESC")
    fun observeAll(): Flow<List<MediaLibraryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MediaLibraryEntry)

    @Delete
    suspend fun delete(entry: MediaLibraryEntry)
}
