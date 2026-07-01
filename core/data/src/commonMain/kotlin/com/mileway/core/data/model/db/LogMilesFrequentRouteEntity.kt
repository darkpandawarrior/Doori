package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_miles_frequent_routes")
data class LogMilesFrequentRouteEntity(
    @PrimaryKey val routeKey: String,
    val locationsJson: String,
    val distanceKm: Double,
    val useCount: Int,
    val lastUsedAt: Long,
)
