package com.mileway.core.data.settings

import android.content.Context
import java.io.File

/** P31.MISC.2: how disruptive clearing a [StorageArea] is — drives the confirmation UI. */
enum class StorageTier { SAFE, CAUTION, DANGER }

/** P31.MISC.2: one clearable on-device storage area, listed on the storage-management screen. */
data class StorageArea(
    val id: String,
    val label: String,
    val description: String,
    val tier: StorageTier,
    val bytes: Long,
)

/**
 * PLAN_V22 P6.6: Preferences' "Storage" tile's real, on-device cache-size readout + working
 * clear-cache action — replaces its previous
 * `ProfileAction.RaisePreferenceMessage("Manage local data in the full app.")` snackbar-only tap.
 * Reports the Room database file's size plus everything under [Context.cacheDir] (never
 * `filesDir`/DataStore files — those hold real, still-needed local records, not disposable
 * cache), and [clearCache] only ever deletes [Context.cacheDir]'s contents, matching
 * `DebugMenuComposeViewModel.clearAppCache`'s existing pattern.
 */
class StorageRepository(private val context: Context) {
    /** Bytes used by the Room database file (`mileway.db` + its `-wal`/`-shm`/`-journal` siblings, if present). */
    fun databaseBytes(): Long {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val siblings = dbFile.parentFile?.listFiles { file -> file.name.startsWith(DATABASE_NAME) }.orEmpty()
        return siblings.sumOf { it.length() }
    }

    /** Bytes used by everything under [Context.cacheDir] — the only thing [clearCache] deletes. */
    fun cacheBytes(): Long = directorySizeBytes(context.cacheDir)

    /** Total on-device footprint this screen reports: database + cache. */
    fun totalBytes(): Long = databaseBytes() + cacheBytes()

    /**
     * Empties [Context.cacheDir] by deleting its contents, leaving the directory itself in place.
     * (Deleting and recreating the directory briefly leaves `cacheDir` missing, and under Robolectric
     * removing the managed cache directory corrupts its temp-dir bookkeeping — emptying avoids both.)
     */
    fun clearCache() {
        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    /** Bytes used by every DataStore preferences file under [Context.filesDir]/datastore. */
    fun preferencesBytes(): Long = directorySizeBytes(File(context.filesDir, DATASTORE_DIR))

    /**
     * P31.MISC.2: the tiered storage-management screen's area list — Safe (cache, no data loss),
     * Caution (local settings/session, disruptive but recoverable — the user may be signed out) and
     * Danger (the Room database — every trip/expense/record on this device, unrecoverable).
     */
    fun storageAreas(): List<StorageArea> =
        listOf(
            StorageArea(
                id = AREA_CACHE,
                label = "App cache",
                description = "Temporary files — safe to clear any time.",
                tier = StorageTier.SAFE,
                bytes = cacheBytes(),
            ),
            StorageArea(
                id = AREA_PREFERENCES,
                label = "Saved preferences",
                description = "Resets local app settings and session data. You may be signed out.",
                tier = StorageTier.CAUTION,
                bytes = preferencesBytes(),
            ),
            StorageArea(
                id = AREA_DATABASE,
                label = "Local database",
                description = "Deletes every trip, expense, and record on this device. Cannot be undone.",
                tier = StorageTier.DANGER,
                bytes = databaseBytes(),
            ),
        )

    /** Clears one [StorageArea] by [areaId] (one of the `AREA_*` constants). */
    fun clearArea(areaId: String) {
        when (areaId) {
            AREA_CACHE -> clearCache()
            AREA_PREFERENCES -> File(context.filesDir, DATASTORE_DIR).listFiles()?.forEach { it.deleteRecursively() }
            AREA_DATABASE -> context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun directorySizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        private const val DATABASE_NAME = "mileway.db"
        private const val DATASTORE_DIR = "datastore"
        const val AREA_CACHE = "cache"
        const val AREA_PREFERENCES = "preferences"
        const val AREA_DATABASE = "database"
    }
}
