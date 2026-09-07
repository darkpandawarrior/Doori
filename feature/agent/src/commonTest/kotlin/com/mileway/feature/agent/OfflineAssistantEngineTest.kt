package com.mileway.feature.agent

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.model.AdvanceRequest
import com.mileway.feature.advances.model.AdvanceRequestStatus
import com.mileway.feature.advances.model.AdvanceSection
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.SubmittedRequest
import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.OfflineAssistantEngine
import com.mileway.feature.logging.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

// ── Fake DAO ──────────────────────────────────────────────────────────────────

private class FakeSavedTrackDao(tracks: List<SavedTrack> = emptyList()) : SavedTrackDao {
    @Suppress("ktlint:standard:property-naming")
    private val _flow = MutableStateFlow(tracks)

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = _flow

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = _flow

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> =
        MutableStateFlow(_flow.value.filter { it.startedByAccountId == accountId })

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) = Unit

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    // Stale-fake catch-up: SavedTrackDao.updateSmartDistanceFinal was added by the SmartDistance
    // commit without updating this test fake (the tracking/core-data fakes were fixed, this one missed).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    override suspend fun deleteSavedTrack(track: SavedTrack) = Unit

    override suspend fun deleteSavedTrack(routeId: String) = Unit

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override suspend fun count(): Long = _flow.value.size.toLong()

    override suspend fun getActiveTrack(): SavedTrack? = null

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(null)

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null

    override fun getRetainedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRange(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun countInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Int = 0

    override suspend fun updateTrackName(
        routeId: String,
        name: String,
    ) = Unit

    override suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    ) = Unit

    override suspend fun markTrackDraft(
        routeId: String,
        draftSavedAt: Long,
    ): Int = 0

    override suspend fun updateSubmissionTime(
        routeId: String,
        submissionTime: Long,
    ): Int = 0

    override suspend fun finalizeTrack(
        routeId: String,
        endTime: Long,
        finalDistance: Double,
        avgSpeed: Double,
        maxSpeed: Double,
    ) = Unit

    override suspend fun markTrackCompleted(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
        submittedAmount: Double,
        submittedAmountCurrency: String,
        transId: String?,
    ): Int = 0

    override suspend fun markTrackEndedLocally(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
    ): Int = 0

    override suspend fun markRetained(routeIds: List<String>) = Unit

    override suspend fun markRetainedBefore(threshold: Long): Int = 0

    override suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    ) = Unit

    override suspend fun deleteCorruptedTracks(): Int = 0

    override suspend fun getCorruptedTrackCount(): Int = 0

    override suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int = 0

    override suspend fun getLastNRouteIdsFromRange(
        start: Long,
        end: Long,
        limit: Int,
    ): List<String> = emptyList()

    override suspend fun getAverageTrackMetrics(): TrackMetrics = TrackMetrics(0.0, 0L, 0f, 0)

    override suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack? = null

    override suspend fun getSimilarTracks(routeId: String): List<SavedTrack> = emptyList()

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()

    override suspend fun markLocalDataPurged(routeId: String) = Unit

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0

    override suspend fun setOfficeAndEntity(
        routeId: String,
        officeId: Long?,
        entityId: Long?,
    ): Int = 0
}

private fun fakeTrack(
    routeId: String,
    distanceKm: Double,
    endTimeMs: Long,
): SavedTrack =
    SavedTrack(
        routeId = routeId,
        name = routeId,
        isCompleted = true,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = endTimeMs - 60_000L,
        endTime = endTimeMs,
        distance = distanceKm,
        duration = 60_000L,
    )

// ── Fake AdvancesRepository ───────────────────────────────────────────────────

/** Only [openRequests]/[closedRequests] matter to OfflineAssistantEngine; the rest are unused stubs. */
private class FakeAdvancesRepository(
    private val open: List<AdvanceRequest> = emptyList(),
    private val closed: List<AdvanceRequest> = emptyList(),
) : AdvancesRepository {
    override fun activePettyCards(): Flow<List<PettyCard>> = flowOf(emptyList())

    override fun pastPettyCards(): Flow<List<PettyCard>> = flowOf(emptyList())

    override fun pettyTransactions(cardId: String): Flow<List<AdvanceTransaction>> = flowOf(emptyList())

    override fun pettyTypes(): List<AdvanceType> = emptyList()

    override suspend fun submitPettyRequest(
        type: String?,
        amount: Double,
        title: String,
        description: String,
        startMs: Long?,
        endMs: Long?,
        declarationAccepted: Boolean,
    ): Result<SubmittedRequest> = Result.success(SubmittedRequest(0L))

    override suspend fun rechargeCard(
        cardId: String,
        amount: Double,
        remarks: String,
    ): Result<Unit> = Result.success(Unit)

    override fun openRequests(): Flow<List<AdvanceRequest>> = flowOf(open)

    override fun closedRequests(): Flow<List<AdvanceRequest>> = flowOf(closed)
}

private fun fakeAdvanceRequest(
    id: Long,
    title: String,
    amount: Double,
    status: AdvanceRequestStatus,
    createdAtMs: Long,
): AdvanceRequest =
    AdvanceRequest(
        id = id,
        title = title,
        description = title,
        amount = amount,
        type = "Travel Petty Cash",
        section = AdvanceSection.PETTY,
        status = status,
        createdAtMs = createdAtMs,
    )

// ── Tests ─────────────────────────────────────────────────────────────────────

class OfflineAssistantEngineTest {
    private fun engine(
        dao: SavedTrackDao = FakeSavedTrackDao(),
        advances: AdvancesRepository = FakeAdvancesRepository(),
        expenses: ExpenseRepository = ExpenseRepository(),
    ) = OfflineAssistantEngine(dao, advances, expenses)

    @Test
    fun `respond emits Thinking then Tokens then Done`() =
        runTest {
            val chunks = engine().respond("conv1", "hello", 0).toList()
            assertTrue(chunks.first() is AssistantChunk.Thinking, "first chunk should be Thinking")
            assertTrue(chunks.last() is AssistantChunk.Done, "last chunk should be Done")
            val tokens = chunks.filterIsInstance<AssistantChunk.Token>()
            assertTrue(tokens.isNotEmpty(), "should emit at least one Token")
        }

    @Test
    fun `Done chunk fullText matches concatenated tokens`() =
        runTest {
            val chunks = engine().respond("conv1", "hello", 0).toList()
            val concatenated = chunks.filterIsInstance<AssistantChunk.Token>().joinToString("") { it.text }
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertEquals(done.fullText.trim(), concatenated.trim())
        }

    @Test
    fun `mileage query with no tracks returns zero-trip message`() =
        runTest {
            val chunks = engine(dao = FakeSavedTrackDao(emptyList())).respond("conv1", "km this week", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.contains("haven't tracked", ignoreCase = true))
        }

    @Test
    fun `mileage query with recent track returns grounded km count`() =
        runTest {
            val recentEndMs = Clock.System.now().toEpochMilliseconds() - 60_000L
            val track = fakeTrack("r1", distanceKm = 42.0, endTimeMs = recentEndMs)
            val chunks = engine(dao = FakeSavedTrackDao(listOf(track))).respond("conv1", "km this week", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.contains("42"), "reply should contain the distance '42'")
            assertTrue(done.fullText.contains("1 trip", ignoreCase = true) || done.fullText.contains("1trip", ignoreCase = true))
        }

    @Test
    fun `mileage query with old track (beyond 7 days) returns zero-trip message`() =
        runTest {
            val oldEnd = 1_000_000_000L
            val track = fakeTrack("r1", distanceKm = 100.0, endTimeMs = oldEnd)
            val chunks = engine(dao = FakeSavedTrackDao(listOf(track))).respond("conv1", "km this week", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.contains("haven't tracked", ignoreCase = true))
        }

    @Test
    fun `first-turn reply includes title suggestion`() =
        runTest {
            val chunks = engine().respond("conv1", "what is policy cap", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertNotNull(done.titleSuggestion, "first-turn Done should carry a title suggestion")
        }

    @Test
    fun `subsequent-turn reply omits title suggestion`() =
        runTest {
            val chunks = engine().respond("conv1", "what is policy cap", historySize = 3).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertEquals(null, done.titleSuggestion)
        }

    @Test
    fun `generic fallback reply is non-empty`() =
        runTest {
            val chunks = engine().respond("conv1", "xyzzy nonsense", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.isNotBlank())
        }

    // ── Real-data replies (PLAN: mileway-assistant-real-data) ─────────────────

    @Test
    fun `expense rejection reply cites the real rejected record, not EXP-003`() =
        runTest {
            val chunks = engine().respond("conv1", "why was my expense rejected", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            // ExpenseRepository's seed has exactly one REJECTED record: EXP-007.
            assertTrue(done.fullText.contains("EXP-007"), "should cite the real rejected expense id")
            assertTrue(
                done.fullText.contains("client entertainment", ignoreCase = true),
                "should include the real rejection reason, not a fabricated one",
            )
        }

    @Test
    fun `advance status reply cites the real most-recent request, not ADV-001`() =
        runTest {
            val advances =
                FakeAdvancesRepository(
                    open =
                        listOf(
                            fakeAdvanceRequest(1L, "Travel Kit top-up", 4000.0, AdvanceRequestStatus.PENDING, createdAtMs = 2_000L),
                        ),
                    closed =
                        listOf(
                            fakeAdvanceRequest(2L, "Office Supplies restock", 2000.0, AdvanceRequestStatus.APPROVED, createdAtMs = 5_000L),
                        ),
                )
            val chunks = engine(advances = advances).respond("conv1", "what is my advance status", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.contains("Office Supplies restock"), "should cite the most recently created request")
            assertTrue(done.fullText.contains("APPROVED"), "should cite its real status")
            assertTrue(!done.fullText.contains("ADV-001"), "must not fall back to the old hardcoded id")
        }

    @Test
    fun `advance status reply degrades gracefully with no requests`() =
        runTest {
            val chunks = engine(advances = FakeAdvancesRepository()).respond("conv1", "advance status please", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            assertTrue(done.fullText.contains("no advance requests", ignoreCase = true))
        }

    @Test
    fun `card balance reply cites real card numbers, not a fictional 4821`() =
        runTest {
            val chunks = engine().respond("conv1", "what is my card balance", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            // CardsMockData's real virtual cards: 4291, 8830, 5102, 7744.
            assertTrue(done.fullText.contains("4291"), "should cite a real card number")
            assertTrue(!done.fullText.contains("4821"), "must not fall back to the old fictional card number")
        }

    @Test
    fun `pending approvals reply cites the real breakdown, not a flat 3`() =
        runTest {
            val chunks = engine().respond("conv1", "how many pending approvals do I have", 0).toList()
            val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
            // ApprovalsRepository.all is golden data pinned elsewhere: 4 pending (2 mileage, 1
            // expense, 1 travel) — see ApprovalsRepository's own class-doc note.
            assertTrue(done.fullText.contains("4"), "should cite the real pending count")
            assertTrue(done.fullText.contains("mileage", ignoreCase = true))
        }
}
