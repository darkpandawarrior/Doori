package com.mileway

import android.content.Context
import com.mileway.core.data.settings.StorageRepository
import com.mileway.core.data.settings.StorageTier
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P6.6: [StorageRepository]'s byte-count readout + clear-cache action, backing Preferences'
 * Storage tile/sheet.
 *
 * Runs on a plain JVM with a mocked [Context] over real [TemporaryFolder] directories rather than
 * under Robolectric: [StorageRepository] only touches `cacheDir` + `getDatabasePath`, and doing
 * real cacheDir I/O under Robolectric corrupted its managed temp-dir bookkeeping (Z.5b — a Linux-CI
 * `NoSuchFileException` in the next test method's sandbox setup). A TemporaryFolder is exactly the
 * isolated, disposable filesystem this needs.
 */
class StorageRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var cacheDir: File
    private lateinit var dbDir: File
    private lateinit var filesDir: File

    private fun repository(): StorageRepository {
        cacheDir = tmp.newFolder("cache")
        dbDir = tmp.newFolder("databases")
        filesDir = tmp.newFolder("files")
        val context =
            mockk<Context> {
                every { cacheDir } returns this@StorageRepositoryTest.cacheDir
                every { filesDir } returns this@StorageRepositoryTest.filesDir
                every { getDatabasePath(any()) } answers { File(dbDir, firstArg()) }
                every { deleteDatabase(any()) } answers {
                    dbDir.listFiles { file -> file.name.startsWith(firstArg<String>()) }
                        ?.forEach { it.delete() }
                    true
                }
            }
        return StorageRepository(context)
    }

    @Test
    fun `cacheBytes reflects files written under cacheDir`() {
        val repository = repository()
        assertEquals(0L, repository.cacheBytes())

        File(cacheDir, "demo.tmp").writeBytes(ByteArray(2_048))

        assertEquals(2_048L, repository.cacheBytes())
    }

    @Test
    fun `clearCache empties cacheDir and totalBytes drops accordingly`() {
        val repository = repository()
        File(cacheDir, "demo.tmp").writeBytes(ByteArray(4_096))
        assertTrue(repository.cacheBytes() > 0L)

        repository.clearCache()

        assertEquals(0L, repository.cacheBytes())
        assertFalse(File(cacheDir, "demo.tmp").exists())
        assertTrue(cacheDir.exists())
    }

    @Test
    fun `totalBytes is database plus cache`() {
        val repository = repository()
        File(cacheDir, "demo.tmp").writeBytes(ByteArray(1_024))

        assertEquals(repository.databaseBytes() + repository.cacheBytes(), repository.totalBytes())
    }

    @Test
    fun `storageAreas reports cache as Safe, preferences as Caution, database as Danger`() {
        val repository = repository()
        val areas = repository.storageAreas().associateBy { it.id }

        assertEquals(StorageTier.SAFE, areas.getValue(StorageRepository.AREA_CACHE).tier)
        assertEquals(StorageTier.CAUTION, areas.getValue(StorageRepository.AREA_PREFERENCES).tier)
        assertEquals(StorageTier.DANGER, areas.getValue(StorageRepository.AREA_DATABASE).tier)
    }

    @Test
    fun `clearArea AREA_PREFERENCES empties the datastore directory`() {
        val repository = repository()
        val datastoreDir = File(filesDir, "datastore").also { it.mkdirs() }
        File(datastoreDir, "session.preferences_pb").writeBytes(ByteArray(512))
        assertTrue(repository.preferencesBytes() > 0L)

        repository.clearArea(StorageRepository.AREA_PREFERENCES)

        assertEquals(0L, repository.preferencesBytes())
    }

    @Test
    fun `clearArea AREA_DATABASE deletes the database file`() {
        val repository = repository()
        File(dbDir, "mileway.db").writeBytes(ByteArray(1_024))
        assertTrue(repository.databaseBytes() > 0L)

        repository.clearArea(StorageRepository.AREA_DATABASE)

        assertEquals(0L, repository.databaseBytes())
    }
}
