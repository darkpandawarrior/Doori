package com.mileway.core.data.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.savedLocationsDataStore by preferencesDataStore(name = "saved_locations")

/** Android actual: persists [SavedLocationsData] as one JSON string in a Preferences DataStore. */
class SavedLocationsStore(private val context: Context) : SavedLocationsSource {
    private val key = stringPreferencesKey("saved_locations_json")

    override val data: Flow<SavedLocationsData> =
        context.savedLocationsDataStore.data.map { SavedLocationsCodec.decode(it[key]) }

    override suspend fun addRecent(place: SavedPlace) = mutate { SavedLocationsCodec.addRecent(it, place) }

    override suspend fun removeRecent(name: String) = mutate { SavedLocationsCodec.removeRecent(it, name) }

    override suspend fun clearRecent() = mutate { SavedLocationsCodec.clearRecent(it) }

    override suspend fun toggleFavorite(place: SavedPlace) = mutate { SavedLocationsCodec.toggleFavorite(it, place) }

    override suspend fun saveAs(
        place: SavedPlace,
        label: String,
    ) = mutate { SavedLocationsCodec.saveAs(it, place, label) }

    override suspend fun removeSaved(label: String) = mutate { SavedLocationsCodec.removeSaved(it, label) }

    private suspend fun mutate(block: (SavedLocationsData) -> SavedLocationsData) {
        context.savedLocationsDataStore.edit { prefs ->
            prefs[key] = SavedLocationsCodec.encode(block(SavedLocationsCodec.decode(prefs[key])))
        }
    }
}
