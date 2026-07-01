package com.mileway.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.SubmitDraftDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.data.model.db.AgentConversationEntity
import com.mileway.core.data.model.db.AgentMessageEntity
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.LogMilesDraftEntity
import com.mileway.core.data.model.db.LogMilesFrequentRouteEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.SubmitDraftEntity
import com.mileway.core.data.model.db.TripAttachmentEntity

@Database(
    entities = [
        LocationData::class,
        SavedTrack::class,
        HardwareEvent::class,
        LogMilesDraftEntity::class,
        LogMilesFrequentRouteEntity::class,
        TripAttachmentEntity::class,
        MediaLibraryEntry::class,
        SubmitDraftEntity::class,
        AgentConversationEntity::class,
        AgentMessageEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@ConstructedBy(MilewayDatabaseConstructor::class)
abstract class MilewayDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    abstract fun savedTrackDao(): SavedTrackDao

    abstract fun hardwareEventDao(): HardwareEventDao

    abstract fun logMilesDraftDao(): LogMilesDraftDao

    abstract fun logMilesFrequentRouteDao(): LogMilesFrequentRouteDao

    abstract fun tripAttachmentDao(): TripAttachmentDao

    abstract fun mediaLibraryDao(): MediaLibraryDao

    abstract fun submitDraftDao(): SubmitDraftDao

    abstract fun agentDao(): AgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MilewayDatabaseConstructor : RoomDatabaseConstructor<MilewayDatabase>
