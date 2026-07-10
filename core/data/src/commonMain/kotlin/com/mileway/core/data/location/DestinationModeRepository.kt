package com.mileway.core.data.location

import com.mileway.core.data.dao.DestinationModeDao
import com.mileway.core.data.model.db.DestinationModeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P11.3 — the head-home destination budget, in ms. A destination stays active for this
 * long once picked, then auto-expires (the seeded "30 sim-minutes" time budget from the plan).
 */
const val DESTINATION_BUDGET_MS: Long = 30 * 60 * 1000L

/** Account key used for head-home state when no persona is signed in (guest). Shared by every reader. */
const val DESTINATION_GUEST_KEY: String = "guest"

/**
 * PLAN_V24 P11.3 — the seeded region-preference options. A pure preference list (id → display
 * name); selecting them stores a set per account with no routing behaviour attached.
 * ponytail: preference store only — there is no region-based routing/dispatch engine to feed, so
 * these are stored and shown as chips and nothing more (ceiling noted in PROGRESS).
 */
data class DestinationRegion(val id: String, val name: String)

val DESTINATION_REGIONS: List<DestinationRegion> =
    listOf(
        DestinationRegion("north", "North Zone"),
        DestinationRegion("south", "South Zone"),
        DestinationRegion("east", "East Zone"),
        DestinationRegion("west", "West Zone"),
        DestinationRegion("central", "Central Zone"),
    )

/**
 * PLAN_V24 P11.3 — remaining head-home time in ms: `expiresAt − now`, floored at 0. A `null`
 * [expiresAt] (no active destination) is 0. Pure so the countdown/expiry rule is unit-tested once.
 */
fun destinationRemainingMs(
    expiresAt: Long?,
    now: Long,
): Long = if (expiresAt == null) 0L else (expiresAt - now).coerceAtLeast(0L)

/** PLAN_V24 P11.3 — active iff there is an unexpired expiry instant. Pure (see [destinationRemainingMs]). */
fun isDestinationActive(
    expiresAt: Long?,
    now: Long,
): Boolean = expiresAt != null && expiresAt > now

/** Parse the comma-separated region-id set stored on [DestinationModeEntity.selectedRegionsCsv]. */
fun parseSelectedRegions(csv: String): Set<String> = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/** The resolved head-home state a UI observes: address + live remaining time + region preference set. */
data class DestinationState(
    val active: Boolean = false,
    val address: String = "",
    val remainingMs: Long = 0L,
    val selectedRegions: Set<String> = emptySet(),
)

/**
 * PLAN_V24 P11.3 — the per-account head-home destination store. Lives in core:data because the
 * tracking screen (feature:tracking) both reads it (the panel) and stamps it onto a starting trip
 * (`SavedTrack.destinationTag`). Pure/offline over Room — no backend. The active destination
 * auto-expires when [DESTINATION_BUDGET_MS] elapses; regions are a preference set only.
 */
class DestinationModeRepository(
    private val dao: DestinationModeDao,
    private val clock: Clock = Clock.System,
) {
    /** Live head-home state for [accountId]; [now] snapshots the countdown at collection time. */
    fun observe(accountId: String): Flow<DestinationState> = dao.observe(accountId).map { row -> row.toState(clock.now().toEpochMilliseconds()) }

    /**
     * The raw persisted row for [accountId] — for callers (the tracking panel) that recompute the
     * live countdown themselves on a 1-second tick, since [observe]'s remaining time is only a
     * snapshot taken when the DB flow last emitted.
     */
    fun observeEntity(accountId: String): Flow<DestinationModeEntity?> = dao.observe(accountId)

    /** The active destination address for [accountId], or `null` when none is active — used to tag a starting trip. */
    suspend fun activeTag(accountId: String): String? {
        val row = dao.get(accountId) ?: return null
        return if (isDestinationActive(row.expiresAt, clock.now().toEpochMilliseconds())) row.address else null
    }

    /** Pick a saved place as the head-home destination; starts the [DESTINATION_BUDGET_MS] countdown. */
    suspend fun activate(
        accountId: String,
        placeId: String,
        address: String,
        lat: Double?,
        lng: Double?,
    ) {
        val existing = dao.get(accountId)
        dao.upsert(
            DestinationModeEntity(
                accountId = accountId,
                placeId = placeId,
                address = address,
                lat = lat,
                lng = lng,
                expiresAt = clock.now().toEpochMilliseconds() + DESTINATION_BUDGET_MS,
                selectedRegionsCsv = existing?.selectedRegionsCsv ?: "",
            ),
        )
    }

    /** Clear the active destination (Disable), preserving the region preferences. */
    suspend fun disable(accountId: String) {
        val existing = dao.get(accountId)
        dao.upsert(
            DestinationModeEntity(
                accountId = accountId,
                expiresAt = null,
                selectedRegionsCsv = existing?.selectedRegionsCsv ?: "",
            ),
        )
    }

    /** Toggle a region id in the preference set (preference-only — no routing). */
    suspend fun toggleRegion(
        accountId: String,
        regionId: String,
    ) {
        val existing = dao.get(accountId)
        val current = parseSelectedRegions(existing?.selectedRegionsCsv ?: "")
        val next = if (regionId in current) current - regionId else current + regionId
        dao.upsert(
            (existing ?: DestinationModeEntity(accountId = accountId))
                .copy(selectedRegionsCsv = next.joinToString(",")),
        )
    }

    private fun DestinationModeEntity?.toState(now: Long): DestinationState {
        if (this == null) return DestinationState()
        val active = isDestinationActive(expiresAt, now)
        return DestinationState(
            active = active,
            address = if (active) address.orEmpty() else "",
            remainingMs = destinationRemainingMs(expiresAt, now),
            selectedRegions = parseSelectedRegions(selectedRegionsCsv),
        )
    }
}
