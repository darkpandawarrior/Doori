package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P5.2: a persisted coupon/promotion. [status] stores the `CouponStatus` enum name as
 * TEXT (converter-free enum-as-string). [code] is the redeem code the "Have a code?" entry checks.
 */
@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    val title: String,
    val terms: String,
    val expiryLabel: String,
    val status: String,
    val updatedAtMs: Long,
)
