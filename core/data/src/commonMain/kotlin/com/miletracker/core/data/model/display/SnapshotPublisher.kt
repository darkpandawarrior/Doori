package com.miletracker.core.data.model.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * L.1: the in-process channel for the latest [SurfaceSnapshot]. Producers (the widget update, a future
 * trip-completion hook) publish; glanceable surfaces observe [snapshot]. Held as a Koin singleton so a
 * surface can read the most recent snapshot without recomputing it from Room — mirroring how
 * TrackingStatePublisher bridges the tracking service and the ViewModel.
 */
interface SnapshotPublisher {
    val snapshot: StateFlow<SurfaceSnapshot>
}

/** Default in-memory [SnapshotPublisher]; the latest snapshot survives for the app-process lifetime. */
class InMemorySnapshotPublisher : SnapshotPublisher {
    private val _snapshot = MutableStateFlow(SurfaceSnapshot())
    override val snapshot: StateFlow<SurfaceSnapshot> = _snapshot.asStateFlow()

    fun publish(snapshot: SurfaceSnapshot) {
        _snapshot.value = snapshot
    }
}
