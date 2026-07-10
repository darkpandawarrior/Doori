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
    // V26 P26.LIB.2 (migration 39->40): favorites + soft-delete + last-viewed tracking.
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val lastAccessedAt: Long? = null,
    // ponytail: one column beyond the task's literal 4 — the WithOcr filter chip (P26.LIB.1) has
    // nothing to filter on without it; adding it here (same migration, same owner) beats shipping
    // a filter that always shows zero results.
    val hasOcr: Boolean = false,
)
