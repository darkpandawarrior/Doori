package com.mileway.core.data.location

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A place the user has interacted with in location search: a recent pick, a starred favorite, or a
 * named saved place (Home / Work / a custom label).
 *
 * Persistence-layer model, deliberately UI-free: [category] is a plain string (a feature-side
 * `PoiCategory.name`) so core:data never depends on the logging feature's UI model. The logging
 * feature maps this to/from its own `LocationEntry` at the boundary (see `SavedPlaceMapping.kt`).
 *
 * @param label   non-null only for saved places (e.g. "Home", "Work"); null for recents/favorites.
 * @param favorite `true` when this entry is in the favorites list; carried for round-tripping.
 */
@Serializable
data class SavedPlace(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val category: String = "OTHER",
    val label: String? = null,
    val favorite: Boolean = false,
)

/**
 * The full persisted snapshot: three independent lists, mirroring the reference structure of
 * recents / favorites / saved (named) places. A place may appear in more than one list (e.g. a
 * recent that's also favorited); identity within a list is [SavedPlace.name].
 */
@Serializable
data class SavedLocationsData(
    val recent: List<SavedPlace> = emptyList(),
    val favorites: List<SavedPlace> = emptyList(),
    val saved: List<SavedPlace> = emptyList(),
)

/**
 * Read + mutate the user's saved/recent/favorite places. Backed by DataStore (one JSON blob) per
 * platform; see the androidMain / iosMain `SavedLocationsStore` actuals. All writes are offline and
 * survive process death. Never throws on decode — a corrupt blob resets to empty.
 */
interface SavedLocationsSource {
    /** The live snapshot; emits on every mutation. */
    val data: Flow<SavedLocationsData>

    suspend fun addRecent(place: SavedPlace)

    suspend fun removeRecent(name: String)

    suspend fun clearRecent()

    /** Add to favorites if absent, else remove — matched by [SavedPlace.name]. */
    suspend fun toggleFavorite(place: SavedPlace)

    /** Save (or replace) a named place. One entry per [label]; also de-dupes the same place. */
    suspend fun saveAs(
        place: SavedPlace,
        label: String,
    )

    suspend fun removeSaved(label: String)
}

/**
 * Pure encode/decode + list-mutation logic, shared by every platform's store so the real behavior is
 * written and tested once (see `SavedLocationsCodecTest`). Platform actuals only own the DataStore
 * read/write of the single JSON string; every transform routes through here.
 */
object SavedLocationsCodec {
    /** Most-recent-first recents are capped at this many entries. */
    const val RECENT_CAP: Int = 8

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(data: SavedLocationsData): String = json.encodeToString(SavedLocationsData.serializer(), data)

    /** Decode a stored blob; `null`/blank/corrupt all resolve to an empty snapshot (never throws). */
    fun decode(raw: String?): SavedLocationsData {
        if (raw.isNullOrBlank()) return SavedLocationsData()
        return runCatching { json.decodeFromString(SavedLocationsData.serializer(), raw) }
            .getOrElse { SavedLocationsData() }
    }

    fun addRecent(
        data: SavedLocationsData,
        place: SavedPlace,
    ): SavedLocationsData {
        val deduped = data.recent.filterNot { it.name == place.name }
        return data.copy(recent = (listOf(place.copy(label = null)) + deduped).take(RECENT_CAP))
    }

    fun removeRecent(
        data: SavedLocationsData,
        name: String,
    ): SavedLocationsData = data.copy(recent = data.recent.filterNot { it.name == name })

    fun clearRecent(data: SavedLocationsData): SavedLocationsData = data.copy(recent = emptyList())

    fun toggleFavorite(
        data: SavedLocationsData,
        place: SavedPlace,
    ): SavedLocationsData {
        val exists = data.favorites.any { it.name == place.name }
        return if (exists) {
            data.copy(favorites = data.favorites.filterNot { it.name == place.name })
        } else {
            data.copy(favorites = listOf(place.copy(favorite = true, label = null)) + data.favorites)
        }
    }

    fun saveAs(
        data: SavedLocationsData,
        place: SavedPlace,
        label: String,
    ): SavedLocationsData {
        // One saved entry per label (a single Home/Work), and never two rows for the same place.
        val filtered = data.saved.filterNot { it.label == label || it.name == place.name }
        return data.copy(saved = filtered + place.copy(label = label))
    }

    fun removeSaved(
        data: SavedLocationsData,
        label: String,
    ): SavedLocationsData = data.copy(saved = data.saved.filterNot { it.label == label })
}
