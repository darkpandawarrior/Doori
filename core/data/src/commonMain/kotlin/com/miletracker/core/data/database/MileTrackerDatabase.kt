package com.miletracker.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.miletracker.core.data.dao.HardwareEventDao
import com.miletracker.core.data.dao.LocationDao
import com.miletracker.core.data.dao.LogMilesDraftDao
import com.miletracker.core.data.dao.LogMilesFrequentRouteDao
import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.dao.SubmitDraftDao
import com.miletracker.core.data.dao.TripAttachmentDao
import com.miletracker.core.data.library.MediaLibraryDao
import com.miletracker.core.data.library.MediaLibraryEntry
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.LogMilesDraftEntity
import com.miletracker.core.data.model.db.LogMilesFrequentRouteEntity
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.model.db.SubmitDraftEntity
import com.miletracker.core.data.model.db.TripAttachmentEntity

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
    ],
    version = 5,
    exportSchema = false,
)
@ConstructedBy(MileTrackerDatabaseConstructor::class)
abstract class MileTrackerDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    abstract fun savedTrackDao(): SavedTrackDao

    abstract fun hardwareEventDao(): HardwareEventDao

    abstract fun logMilesDraftDao(): LogMilesDraftDao

    abstract fun logMilesFrequentRouteDao(): LogMilesFrequentRouteDao

    abstract fun tripAttachmentDao(): TripAttachmentDao

    abstract fun mediaLibraryDao(): MediaLibraryDao

    abstract fun submitDraftDao(): SubmitDraftDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MileTrackerDatabaseConstructor : RoomDatabaseConstructor<MileTrackerDatabase>
