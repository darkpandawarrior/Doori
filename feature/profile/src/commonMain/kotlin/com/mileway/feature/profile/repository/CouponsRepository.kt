package com.mileway.feature.profile.repository

import com.mileway.core.data.coupon.Coupon
import com.mileway.core.data.coupon.CouponApplyResult
import com.mileway.core.data.coupon.CouponStatus
import com.mileway.core.data.dao.CouponDao
import com.mileway.core.data.model.db.CouponEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P5.2: Room-backed coupons store. Seeded once from [CouponsMockData]. [applyCode]
 * validates a typed code against the seeded set and marks an ACTIVE coupon REDEEMED, returning the
 * source's invalid/expired/already-used/success outcome. Redeem-notification logging is done by the
 * ViewModel (localized strings resolved in the composable).
 */
class CouponsRepository(private val dao: CouponDao, private val clock: Clock = Clock.System) {
    /** Live coupons, active first (status ASC puts ACTIVE before EXPIRED/REDEEMED alphabetically). */
    fun observeAll(): Flow<List<Coupon>> = dao.observeAll().map { rows -> rows.map { it.toCoupon() } }

    /** Seeds [com.mileway.feature.profile.data.CouponsMockData] on first run only. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(com.mileway.feature.profile.data.CouponsMockData.coupons.map { it.toEntity(now) })
    }

    /**
     * Applies a typed [code]: unknown → INVALID, expired → EXPIRED, already redeemed → ALREADY_USED,
     * otherwise marks the coupon REDEEMED and returns SUCCESS.
     */
    suspend fun applyCode(code: String): CouponApplyResult {
        val entity = dao.findByCode(code.trim()) ?: return CouponApplyResult.INVALID
        return when (CouponStatus.entries.firstOrNull { it.name == entity.status }) {
            CouponStatus.EXPIRED -> CouponApplyResult.EXPIRED
            CouponStatus.REDEEMED -> CouponApplyResult.ALREADY_USED
            else -> {
                dao.upsert(entity.copy(status = CouponStatus.REDEEMED.name, updatedAtMs = clock.now().toEpochMilliseconds()))
                CouponApplyResult.SUCCESS
            }
        }
    }

    private fun CouponEntity.toCoupon(): Coupon =
        Coupon(
            id = id,
            code = code,
            title = title,
            terms = terms,
            expiryLabel = expiryLabel,
            status = CouponStatus.entries.firstOrNull { it.name == status } ?: CouponStatus.ACTIVE,
        )

    private fun Coupon.toEntity(now: Long): CouponEntity =
        CouponEntity(
            id = id,
            code = code,
            title = title,
            terms = terms,
            expiryLabel = expiryLabel,
            status = status.name,
            updatedAtMs = now,
        )
}
