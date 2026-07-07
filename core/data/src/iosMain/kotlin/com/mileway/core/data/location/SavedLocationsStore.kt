package com.mileway.core.data.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

/**
 * iOS actual: persists [SavedLocationsData] as one JSON string in a Preferences DataStore under the
 * app's Documents directory (durable across restarts, unlike the ephemeral track session which uses
 * the temp dir).
 */
class SavedLocationsStore : SavedLocationsSource {
    private val key = stringPreferencesKey("saved_locations_json")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (documentsDir() + "/saved_locations.preferences_pb").toPath() },
        )

    override val data: Flow<SavedLocationsData> =
        store.data.map { SavedLocationsCodec.decode(it[key]) }

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
        store.edit { prefs ->
            prefs[key] = SavedLocationsCodec.encode(block(SavedLocationsCodec.decode(prefs[key])))
        }
    }

    private fun documentsDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return (paths.firstOrNull() as? String) ?: NSTemporaryDirectory()
    }
}
