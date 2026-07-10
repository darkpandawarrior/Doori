package com.mileway.core.data.model.db

import androidx.room.Entity

/**
 * PLAN_V24 P13.3: one row per (account, forced-popup) the user has acknowledged. Composite primary
 * key isolates each persona; persisting the acknowledgement is what makes a one-shot popup a true
 * one-shot across app restarts (closing the P12.9 offer + P12.7 signature session-scoped ceilings).
 */
@Entity(tableName = "popup_acks", primaryKeys = ["accountId", "popupId"])
data class PopupAckEntity(
    val accountId: String,
    val popupId: String,
    val acknowledgedAtMs: Long,
)
