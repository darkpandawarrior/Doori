package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * PLAN_V28 P28.4: local-only per-room metadata layered on top of a [ClarificationRoomEntity] —
 * save/pin for triage, freeform tags + a note, and an optional reminder timestamp. One row per
 * room, created lazily on first toggle/edit (no row = all-defaults, same "doesn't exist yet"
 * convention as [com.mileway.core.data.model.db.BannerDismissedEntity]). `tagsCsv` is a plain
 * comma-joined list — no json lib needed for a handful of short tags.
 */
@Entity(
    tableName = "clarification_room_meta",
    foreignKeys = [
        ForeignKey(
            entity = ClarificationRoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ClarificationRoomMetaEntity(
    @PrimaryKey val roomId: String,
    val isSaved: Boolean = false,
    val isPinned: Boolean = false,
    val tagsCsv: String = "",
    val note: String = "",
    val reminderAtMs: Long? = null,
)
