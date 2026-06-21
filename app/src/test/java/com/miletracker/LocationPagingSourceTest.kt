package com.miletracker

import androidx.paging.PagingSource
import com.miletracker.core.data.dao.LocationDao
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.feature.tracking.paging.LocationPagingSource
import com.miletracker.feature.tracking.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G1 (Paging 3): proves the [LocationPagingSource] page boundaries — first page keys, append walking,
 * the short-page end-of-pagination signal, and the empty-trail case. The DAO is faked with MockK
 * honoring LIMIT/OFFSET so the test stays pure-JVM (no Room), matching the existing test style.
 */
class LocationPagingSourceTest {
    private val token = "trip-A"

    private fun point(id: Long) =
        LocationData(
            id = id,
            activity = "WALK",
            speed = 1f,
            lat = 18.0 + id,
            lng = 73.0 + id,
            token = token,
            date = 1_000L + id,
            batteryPercentage = 90.0,
        )

    /** Fake DAO backed by an in-memory list that honors the LIMIT/OFFSET contract. */
    private fun repoWith(total: Int): LocationRepository {
        val all = (0 until total).map { point(it.toLong()) }
        val dao = mockk<LocationDao>()
        coEvery { dao.getLocationsByTokenPaged(token, any(), any()) } answers {
            val limit = secondArg<Int>()
            val offset = thirdArg<Int>()
            all.drop(offset).take(limit)
        }
        coEvery { dao.countLocationsByToken(token) } returns total
        return LocationRepository(dao)
    }

    @Test
    fun `refresh returns the first page with no prevKey and a forward nextKey`() =
        runTest {
            val source = LocationPagingSource(token, repoWith(total = 65))
            val result =
                source.load(
                    PagingSource.LoadParams.Refresh(key = null, loadSize = 30, placeholdersEnabled = false),
                ) as PagingSource.LoadResult.Page<Int, LocationData>

            assertEquals(30, result.data.size)
            assertEquals(0L, result.data.first().id)
            assertNull(result.prevKey)
            assertEquals(30, result.nextKey)
        }

    @Test
    fun `append walks pages and stops at a short final page`() =
        runTest {
            val source = LocationPagingSource(token, repoWith(total = 65))

            val page2 =
                source.load(
                    PagingSource.LoadParams.Append(key = 30, loadSize = 30, placeholdersEnabled = false),
                ) as PagingSource.LoadResult.Page<Int, LocationData>
            assertEquals(30, page2.data.size)
            assertEquals(30L, page2.data.first().id)
            assertEquals(0, page2.prevKey)
            assertEquals(60, page2.nextKey)

            val page3 =
                source.load(
                    PagingSource.LoadParams.Append(key = 60, loadSize = 30, placeholdersEnabled = false),
                ) as PagingSource.LoadResult.Page<Int, LocationData>
            assertEquals(5, page3.data.size) // 65 - 60
            assertNull(page3.nextKey) // short page → end of pagination
        }

    @Test
    fun `empty trail yields an empty page with no keys`() =
        runTest {
            val source = LocationPagingSource(token, repoWith(total = 0))
            val result =
                source.load(
                    PagingSource.LoadParams.Refresh(key = null, loadSize = 30, placeholdersEnabled = false),
                ) as PagingSource.LoadResult.Page<Int, LocationData>

            assertTrue(result.data.isEmpty())
            assertNull(result.prevKey)
            assertNull(result.nextKey)
        }
}
