package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P1.1: a real, persisted, seedable multi-persona account — replaces `DemoAccount`'s bare
 * in-memory serializable (`stub.ProfileMockData.accounts()`) as the actual source of truth for
 * "which personas can this demo session switch between." `DemoAccount` (core/network) stays the
 * screen-facing model `ProfileScreen`'s `PersonaSwitcherRow` already renders — P1.2 maps this
 * entity to/from it so no UI shape changes yet.
 *
 * [isActive] is exclusive across the table (`setActive` clears every other row inside one
 * `@Transaction`); [lastLoginAtMs]/[createdAtMs] back the `AccountDetailsSheet` fields P1.3 adds.
 */
@Entity(tableName = "mock_accounts")
data class MockAccountEntity(
    @PrimaryKey
    val accountId: String,
    val displayName: String,
    val employeeCode: String,
    val organization: String,
    val avatarSeed: String,
    val isActive: Boolean,
    val lastLoginAtMs: Long,
    val createdAtMs: Long,
    // PLAN_V24 P1.6: registered phone (added by MIGRATION_19_20); two seeded personas share one.
    val phone: String = "",
)
