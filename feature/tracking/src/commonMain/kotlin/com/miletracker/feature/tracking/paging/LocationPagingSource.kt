package com.miletracker.feature.tracking.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.feature.tracking.repository.LocationRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * G1 (Paging 3): an offset-keyed [PagingSource] over a single track's GPS trail.
 *
 * A recorded journey can hold thousands of location fixes, so the raw route-points log is the one
 * genuinely list-heavy, flat, Room-backed surface in the app — the honest place for Paging 3 (the
 * other history surfaces are small mock lists or aggregate-derived, where paging would be cargo-cult).
 *
 * The key is the row offset. Each [load] pulls one window via [LocationRepository.pageForToken]
 * (`SELECT … LIMIT :limit OFFSET :offset ORDER BY date ASC`), so points stream in chronologically as
 * the user scrolls. Lives in `commonMain` (paging-common is multiplatform); the Compose collection
 * via `collectAsLazyPagingItems()` stays in `androidMain`.
 */
class LocationPagingSource(
    private val token: String,
    private val repository: LocationRepository,
) : PagingSource<Int, LocationData>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocationData> {
        val offset = params.key ?: 0
        return try {
            val rows = repository.pageForToken(token, limit = params.loadSize, offset = offset)
            LoadResult.Page(
                data = rows,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                // A short page means we've hit the end of the trail — stop appending.
                nextKey = if (rows.size < params.loadSize) null else offset + rows.size,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * Anchor the refresh on the row nearest the user's scroll position so a re-load (e.g. after an
     * invalidation) lands back where they were, not at the top.
     */
    override fun getRefreshKey(state: PagingState<Int, LocationData>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestPageToPosition(anchor) ?: return null
        return closest.prevKey?.plus(state.config.pageSize)
            ?: closest.nextKey?.minus(state.config.pageSize)
    }
}
