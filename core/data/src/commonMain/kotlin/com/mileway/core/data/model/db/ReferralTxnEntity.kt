package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P5.1: a persisted referral transaction (one referred user). [status] stores the
 * `ReferralStatus` enum name as TEXT (converter-free enum-as-string, same as the schema's other
 * enum columns). [submittedAtMillis] is the SimulatedReviewEngine submit timestamp used while
 * PENDING so the referral can resolve to SUCCESS/FAILED on observe.
 */
@Entity(tableName = "referral_txns")
data class ReferralTxnEntity(
    @PrimaryKey
    val id: String,
    val refereeName: String,
    val status: String,
    val taskMessage: String,
    val processedMoney: Double,
    val processedCredits: Int,
    val userNumRides: Int,
    val nextTargetRides: Int,
    val nextTargetMoney: Double,
    val nextTargetCredits: Int,
    val submittedAtMillis: Long,
)
