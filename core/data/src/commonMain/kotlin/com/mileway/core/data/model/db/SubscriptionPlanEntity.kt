package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P6.2: a persisted subscription plan tier (seeded once from `SubscriptionMockData`).
 * [period] and [featuresCsv] store the enum name / `|`-joined feature list as TEXT; [tierRank]
 * orders the plan cards low → high.
 */
@Entity(tableName = "subscription_plans")
data class SubscriptionPlanEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val priceAmount: Double,
    val period: String,
    val savingsCopy: String,
    val monthlySavingsAmount: Double,
    val featuresCsv: String,
    val tierRank: Int,
)

/**
 * PLAN_V24 P6.2: the single active-subscription row. [id] is the constant [ACTIVE_SUBSCRIPTION_ID]
 * so there is at most one row; a mock purchase REPLACEs it, sign-out clears it. [cancelAtPeriodEnd]
 * keeps access until [renewsAtMs] (source: cancel keeps access until period end).
 */
@Entity(tableName = "active_subscription")
data class ActiveSubscriptionEntity(
    @PrimaryKey
    val id: String,
    val planId: String,
    val status: String,
    val startedAtMs: Long,
    val renewsAtMs: Long,
    val cancelAtPeriodEnd: Boolean,
)

/** The sole primary key used by [ActiveSubscriptionEntity] — enforces a single active row. */
const val ACTIVE_SUBSCRIPTION_ID = "current"
