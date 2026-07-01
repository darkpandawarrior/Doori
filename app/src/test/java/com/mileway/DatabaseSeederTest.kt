package com.mileway

import com.mileway.seeder.DatabaseSeeder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Verifies DatabaseSeeder only seeds when the DB is empty and seeds exactly 5 tracks.
 */
class DatabaseSeederTest {

    private lateinit var trackDao: SavedTrackDao
    private lateinit var locationDao: LocationDao
    private lateinit var seeder: DatabaseSeeder

    @Before
    fun setUp() {
        trackDao = mockk(relaxed = true)
        locationDao = mockk(relaxed = true)
        seeder = DatabaseSeeder(trackDao, locationDao)
    }

    @Test
    fun `does not seed when db is non-empty`() = runTest {
        coEvery { trackDao.count() } returns 3L
        seeder.seedIfEmpty()
        coVerify(exactly = 0) { trackDao.insertSavedTrack(any()) }
    }

    @Test
    fun `seeds exactly 5 tracks when db is empty`() = runTest {
        coEvery { trackDao.count() } returns 0L
        seeder.seedIfEmpty()
        coVerify(exactly = 5) { trackDao.insertSavedTrack(any<SavedTrack>()) }
    }

    @Test
    fun `seeds at least 12 locations per track`() = runTest {
        coEvery { trackDao.count() } returns 0L
        seeder.seedIfEmpty()
        // Each of 5 tracks gets 13 locations (0..12 inclusive)
        coVerify(atLeast = 5 * 13) { locationDao.insertLocation(any()) }
    }

    @Test
    fun `seeded tracks have unique routeIds`() = runTest {
        coEvery { trackDao.count() } returns 0L
        val inserted = mutableListOf<SavedTrack>()
        coEvery { trackDao.insertSavedTrack(capture(inserted)) } returns Unit
        seeder.seedIfEmpty()
        val uniqueIds = inserted.map { it.routeId }.toSet()
        assert(uniqueIds.size == 5) { "Expected 5 unique routeIds, got $uniqueIds" }
    }
}
