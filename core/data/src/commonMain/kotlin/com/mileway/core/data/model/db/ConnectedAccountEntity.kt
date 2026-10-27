package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V22 P6.6: a single third-party integration shown on Preferences' "Connected Accounts"
 * tile — Mileway's local/offline equivalent of the reference app's cab/passport
 * connect-disconnect integrations list. [isConnected] is the only mutable field; toggling it
 * never makes a network call (there is no backend to call — see the project's "The backend"),
 * it only flips this persisted row.
 */
@Entity(tableName = "connected_accounts")
data class ConnectedAccountEntity(
    @PrimaryKey
    val id: String,
    val providerName: String,
    val category: String,
    val isConnected: Boolean,
    val updatedAtMs: Long,
)
