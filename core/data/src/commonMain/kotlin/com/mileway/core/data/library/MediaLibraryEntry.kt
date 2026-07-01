package com.mileway.core.data.library

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_library")
data class MediaLibraryEntry(
    @PrimaryKey val id: String,
    val uri: String,
    val mimeType: String,
    val label: String,
    val source: String,
    val savedAtMs: Long,
)
