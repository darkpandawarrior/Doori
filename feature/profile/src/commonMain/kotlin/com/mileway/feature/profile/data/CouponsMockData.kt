package com.mileway.feature.profile.data

import com.mileway.core.data.coupon.Coupon
import com.mileway.core.data.coupon.CouponStatus

/**
 * PLAN_V24 P5.2: seed set for the coupons screen — a mix of active, expired and already-redeemed
 * promotions so the apply-code entry exercises all three error/success paths. Room-backed via
 * CouponsRepository.seedIfEmpty (mirroring [DocumentMockData]).
 */
object CouponsMockData {
    val coupons: List<Coupon> =
        listOf(
            Coupon("CPN-1", "WELCOME50", "₹50 off your next expense", "Min. spend ₹200. One use per account.", "Valid till 31 Dec 2026", CouponStatus.ACTIVE),
            Coupon("CPN-2", "FUEL10", "10% off fuel expenses", "Max discount ₹100 on fuel category.", "Valid till 30 Sep 2026", CouponStatus.ACTIVE),
            Coupon("CPN-3", "TRAVEL25", "₹25 travel credit", "Applies to travel bookings only.", "Valid till 15 Nov 2026", CouponStatus.ACTIVE),
            Coupon("CPN-4", "SUMMER20", "20% summer bonus", "Seasonal promotion.", "Expired 30 Jun 2026", CouponStatus.EXPIRED),
            Coupon("CPN-5", "NEWYEAR", "New year ₹100 bonus", "Limited-time offer.", "Expired 15 Jan 2026", CouponStatus.EXPIRED),
            Coupon("CPN-6", "FIRSTRIDE", "Free first ride credit", "New users only.", "Redeemed", CouponStatus.REDEEMED),
        )
}
