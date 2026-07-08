package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.feature.profile.model.SavedPlace
import com.mileway.feature.profile.model.SavedPlaceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P3.4: Room-backed store for `SavedPlacesScreen`'s home/work/other places. Persists
 * across navigation and process death (unlike a `mutableStateListOf` seed).
 *
 * ponytail: `EmployeeProfile.homeLocation` is a read-only free-text field on the mock network
 * model with no write path (same situation as P2.1 onboarding / P3.1 phone) — so the sync is
 * one-way: the HOME saved place is the canonical, editable home location surfaced going forward,
 * and write-back to `EmployeeProfile.homeLocation` is deferred until a real profile write path
 * exists. See [observeHomeAddress] for the read seam callers should use.
 */
class SavedPlacesRepository(private val dao: SavedPlaceDao, private val clock: Clock = Clock.System) {
    /** Live list of all saved places, most-recently-updated first. */
    fun observeAll(): Flow<List<SavedPlace>> = dao.observeAll().map { rows -> rows.map { it.toSavedPlace() } }

    /** The current HOME place's address (canonical home location), or null if none is saved. */
    fun observeHomeAddress(): Flow<String?> =
        dao.observeAll().map { rows ->
            rows.firstOrNull { it.type == SavedPlaceType.HOME.name }?.address
        }

    /**
     * Inserts or updates a place. A blank [id] mints a new one; a non-blank [id] updates in place.
     * [label] and [address] must be non-blank — callers (the ViewModel) validate and surface the
     * error rather than silently no-oping here.
     */
    suspend fun save(
        id: String,
        type: SavedPlaceType,
        label: String,
        address: String,
        lat: Double?,
        lng: Double?,
    ) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            SavedPlaceEntity(
                id = id.ifBlank { "PLACE-" + now.toString().takeLast(10) },
                type = type.name,
                label = label,
                address = address,
                latitude = lat,
                longitude = lng,
                updatedAtMs = now,
            ),
        )
    }

    /** Deletes [id] outright. */
    suspend fun delete(id: String) = dao.delete(id)

    private fun SavedPlaceEntity.toSavedPlace(): SavedPlace =
        SavedPlace(
            id = id,
            type = SavedPlaceType.entries.firstOrNull { it.name == type } ?: SavedPlaceType.OTHER,
            label = label,
            address = address,
            lat = latitude,
            lng = longitude,
        )
}
