package com.miletracker.core.data.di

import com.miletracker.core.data.database.MileTrackerDatabase
import com.miletracker.core.data.database.buildMileTrackerDatabase
import com.miletracker.core.data.model.display.InMemorySnapshotPublisher
import com.miletracker.core.data.model.display.SnapshotPublisher
import com.miletracker.core.data.outbox.RoomSubmitOutbox
import com.miletracker.core.data.outbox.SubmitOutbox
import com.miletracker.core.data.outbox.TripDraft
import com.miletracker.core.data.session.CurrentTrackDataStore
import com.miletracker.core.data.session.SessionRepository
import com.miletracker.core.data.settings.DemoSettingsRepository
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val coreDataModule =
    module {
        single<MileTrackerDatabase> { buildMileTrackerDatabase() }
        single { get<MileTrackerDatabase>().locationDao() }
        single { get<MileTrackerDatabase>().savedTrackDao() }
        single { get<MileTrackerDatabase>().hardwareEventDao() }
        single { get<MileTrackerDatabase>().logMilesDraftDao() }
        single { get<MileTrackerDatabase>().logMilesFrequentRouteDao() }
        single { get<MileTrackerDatabase>().tripAttachmentDao() }
        single { get<MileTrackerDatabase>().mediaLibraryDao() }
        single { get<MileTrackerDatabase>().submitDraftDao() }
        single { CurrentTrackDataStore() }
        single { SessionRepository() }
        single { DemoSettingsRepository() }
        single { Json { ignoreUnknownKeys = true } }
        single<SubmitOutbox<TripDraft>> { RoomSubmitOutbox(get(), get()) }
        // L.1: in-process snapshot channel for glanceable surfaces (publish + observe).
        single { InMemorySnapshotPublisher() }
        single<SnapshotPublisher> { get<InMemorySnapshotPublisher>() }
    }
