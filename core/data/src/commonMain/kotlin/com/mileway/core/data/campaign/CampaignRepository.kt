package com.mileway.core.data.campaign

import com.mileway.core.data.dao.CampaignDao
import com.mileway.core.data.model.db.CampaignEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P5.4: Room-backed campaigns store, seeded once from [CampaignMockData]. Shared by the
 * profile marketing hub and the HomeScreen marketing strip (hence core:data, not a feature module).
 * [captureInterest] is the one-shot "Get in touch" — flips the flag; the UI disables the CTA after.
 */
class CampaignRepository(private val dao: CampaignDao, private val clock: Clock = Clock.System) {
    /** Live, newest-first campaigns (source: `startedOn` desc). */
    fun observeAll(): Flow<List<Campaign>> = dao.observeAll().map { rows -> rows.map { it.toCampaign() } }

    /** Seeds [CampaignMockData.campaigns] on first run only. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(CampaignMockData.campaigns.mapIndexed { index, c -> c.toEntity(now - index * 86_400_000L) })
    }

    /** Records interest in [id] (one-shot). A no-op if already captured or unknown. */
    suspend fun captureInterest(id: String) {
        val entity = dao.get(id) ?: return
        if (entity.interestCaptured) return
        dao.upsert(entity.copy(interestCaptured = true))
    }

    private fun CampaignEntity.toCampaign(): Campaign =
        Campaign(
            id = id,
            name = name,
            description = description,
            badge = badge,
            status = CampaignStatus.entries.firstOrNull { it.name == status } ?: CampaignStatus.LIVE,
            mobileExclusive = mobileExclusive,
            contactEmail = contactEmail,
            interestCaptured = interestCaptured,
        )

    private fun Campaign.toEntity(startedOn: Long): CampaignEntity =
        CampaignEntity(
            id = id,
            name = name,
            description = description,
            badge = badge,
            status = status.name,
            mobileExclusive = mobileExclusive,
            contactEmail = contactEmail,
            interestCaptured = interestCaptured,
            startedOnMs = startedOn,
        )
}
