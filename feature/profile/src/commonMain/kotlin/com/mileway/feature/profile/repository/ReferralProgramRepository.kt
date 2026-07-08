package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.ReferralTxnDao
import com.mileway.core.data.model.db.ReferralTxnEntity
import com.mileway.core.data.referral.ReferralStatus
import com.mileway.core.data.referral.ReferralTxn
import com.mileway.core.data.review.ReviewResult
import com.mileway.core.data.review.SimulatedReviewEngine
import com.mileway.feature.profile.data.ReferralMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P5.1: Room-backed referral-transactions ledger. Seeded once from [ReferralMockData]
 * (mirroring [DocumentRepository]). PENDING referees resolve to SUCCESS/FAILED through
 * [SimulatedReviewEngine] — a payload containing the reject marker (here, a FAILED seed's task
 * message) fails; everything else succeeds. The leaderboard/activity feeds are static (not Room).
 */
class ReferralProgramRepository(
    private val dao: ReferralTxnDao,
    private val reviewEngine: SimulatedReviewEngine,
    private val clock: Clock = Clock.System,
) {
    /** Live, most-recent-first referral transactions. */
    fun observeAll(): Flow<List<ReferralTxn>> = dao.observeAll().map { rows -> rows.map { it.toTxn() } }

    /** Seeds [ReferralMockData.txns] on first run only. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(
            ReferralMockData.txns.mapIndexed { index, txn ->
                // Stagger submit times so pending referees become reviewable at slightly different points.
                txn.toEntity(submittedAt = now - index * 1_000L)
            },
        )
    }

    /**
     * Resolves any PENDING referee the review engine now considers reviewed — a task message
     * containing the reject marker becomes FAILED, otherwise SUCCESS (credited to its next target).
     */
    suspend fun resolveReviewablePending() {
        currentEntities().filter { it.status == ReferralStatus.PENDING.name }.forEach { entity ->
            when (reviewEngine.resolve(entity.submittedAtMillis, entity.taskMessage)) {
                is ReviewResult.Approved -> dao.upsert(entity.markSuccess())
                is ReviewResult.Rejected -> dao.upsert(entity.copy(status = ReferralStatus.FAILED.name))
                ReviewResult.Pending -> Unit
            }
        }
    }

    /** One-shot snapshot of the seeded rows (the review pass reads the full set once). */
    private suspend fun currentEntities(): List<ReferralTxnEntity> = ReferralMockData.txns.mapNotNull { dao.get(it.id) }

    private fun ReferralTxnEntity.markSuccess(): ReferralTxnEntity =
        copy(
            status = ReferralStatus.SUCCESS.name,
            userNumRides = nextTargetRides,
            processedMoney = processedMoney + nextTargetMoney,
            processedCredits = processedCredits + nextTargetCredits,
            taskMessage = "Target reached — reward credited",
        )

    private fun ReferralTxnEntity.toTxn(): ReferralTxn =
        ReferralTxn(
            id = id,
            refereeName = refereeName,
            status = ReferralStatus.entries.firstOrNull { it.name == status } ?: ReferralStatus.PENDING,
            taskMessage = taskMessage,
            processedMoney = processedMoney,
            processedCredits = processedCredits,
            userNumRides = userNumRides,
            nextTargetRides = nextTargetRides,
            nextTargetMoney = nextTargetMoney,
            nextTargetCredits = nextTargetCredits,
        )

    private fun ReferralTxn.toEntity(submittedAt: Long): ReferralTxnEntity =
        ReferralTxnEntity(
            id = id,
            refereeName = refereeName,
            status = status.name,
            taskMessage = taskMessage,
            processedMoney = processedMoney,
            processedCredits = processedCredits,
            userNumRides = userNumRides,
            nextTargetRides = nextTargetRides,
            nextTargetMoney = nextTargetMoney,
            nextTargetCredits = nextTargetCredits,
            submittedAtMillis = submittedAt,
        )
}
