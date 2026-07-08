package com.mileway.core.data.coupon

/*
 * PLAN_V24 P5.2: promo-code / coupon models (the reference app `get_coupons_and_promotions` → `PromoCoupon`).
 * Offline/mock — in core:data (the base module the Room entity and every consumer reach), same
 * seam choice as the P4.1 verification and P5.1 referral models.
 */

/** A coupon's state: redeemable, past its expiry, or already used. */
enum class CouponStatus { ACTIVE, EXPIRED, REDEEMED }

/** The outcome of applying a typed code (mirrors the source's invalid/expired/already-used cases). */
enum class CouponApplyResult { SUCCESS, INVALID, EXPIRED, ALREADY_USED }

/** A single coupon / promotion. [code] is what the user types in "Have a code?". */
data class Coupon(
    val id: String,
    val code: String,
    val title: String,
    val terms: String,
    val expiryLabel: String,
    val status: CouponStatus,
)
