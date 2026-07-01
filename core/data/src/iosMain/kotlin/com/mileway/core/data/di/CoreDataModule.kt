package com.mileway.core.data.di

import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.database.buildMilewayDatabase
import com.mileway.core.data.model.display.InMemorySnapshotPublisher
import com.mileway.core.data.model.display.SnapshotPublisher
import com.mileway.core.data.outbox.RoomSubmitOutbox
import com.mileway.core.data.outbox.SubmitOutbox
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.core.data.settings.AgentSessionStoreImpl
import com.mileway.core.data.settings.DemoSettingsRepository
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val coreDataModule =
    module {
        single<MilewayDatabase> { buildMilewayDatabase() }
        single { get<MilewayDatabase>().locationDao() }
        single { get<MilewayDatabase>().savedTrackDao() }
        single { get<MilewayDatabase>().hardwareEventDao() }
        single { get<MilewayDatabase>().logMilesDraftDao() }
        single { get<MilewayDatabase>().logMilesFrequentRouteDao() }
        single { get<MilewayDatabase>().tripAttachmentDao() }
        single { get<MilewayDatabase>().mediaLibraryDao() }
        single { get<MilewayDatabase>().submitDraftDao() }
        single { get<MilewayDatabase>().agentDao() }
        single { CurrentTrackDataStore() }
        single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
        single { SessionRepository() }
        single { DemoSettingsRepository() }
        single<AgentSessionStore> { AgentSessionStoreImpl() }
        single { Json { ignoreUnknownKeys = true } }
        single<SubmitOutbox<TripDraft>> { RoomSubmitOutbox(get(), get()) }
        // L.1: in-process snapshot channel for glanceable surfaces (publish + observe).
        single { InMemorySnapshotPublisher() }
        single<SnapshotPublisher> { get<InMemorySnapshotPublisher>() }
    }
