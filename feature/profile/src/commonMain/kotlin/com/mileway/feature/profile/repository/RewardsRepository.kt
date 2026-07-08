package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.RewardCardDao
import com.mileway.core.data.model.db.RewardCardEntity
import com.mileway.core.data.rewards.RewardCard
import com.mileway.core.data.rewards.RewardStatus
import com.mileway.feature.profile.data.RewardsMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P5.3: Room-backed scratch-card store. Seeded once from [RewardsMockData]. [scratch]
 * flips an UNSCRATCHED card to SCRATCHED (the reveal) and returns the credits it granted.
 *
 * ponytail: a separate credits ledger is out of scope — the granted credits live on the card row;
 * the total is derived from scratched cards. Noted in PROGRESS.
 */
class RewardsRepository(private val dao: RewardCardDao, private val clock: Clock = Clock.System) {
    /** Live, newest-first reward cards. */
    fun observeAll(): Flow<List<RewardCard>> = dao.observeAll().map { rows -> rows.map { it.toCard() } }

    /** Seeds [RewardsMockData.cards] on first run only. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(RewardsMockData.cards.mapIndexed { index, card -> card.toEntity(now - index * 1_000L) })
    }

    /** Reveals [id] (marks it SCRATCHED). Returns the credits granted, or 0 if already scratched/unknown. */
    suspend fun scratch(id: String): Int {
        val entity = dao.get(id) ?: return 0
        if (entity.status == RewardStatus.SCRATCHED.name) return 0
        dao.upsert(entity.copy(status = RewardStatus.SCRATCHED.name))
        return entity.credits
    }

    private fun RewardCardEntity.toCard(): RewardCard =
        RewardCard(
            id = id,
            title = title,
            rewardLabel = rewardLabel,
            credits = credits,
            status = RewardStatus.entries.firstOrNull { it.name == status } ?: RewardStatus.UNSCRATCHED,
        )

    private fun RewardCard.toEntity(grantedAt: Long): RewardCardEntity =
        RewardCardEntity(
            id = id,
            title = title,
            rewardLabel = rewardLabel,
            credits = credits,
            status = status.name,
            grantedAtMs = grantedAt,
        )
}
