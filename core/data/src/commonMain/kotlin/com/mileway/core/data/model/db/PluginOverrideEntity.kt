package com.mileway.core.data.model.db

import androidx.room.Entity

/**
 * PLAN_V24 P0.1 — one per-account plugin override (the USER resolution layer). Composite key
 * `(accountId, pluginId)` keeps overrides isolated per persona (respects V23 multi-profile
 * isolation): switching accounts changes which rows apply, without touching the PRESET or FORCED
 * layers. [value] is a raw string interpreted per descriptor kind (see
 * [com.mileway.core.data.plugin.PluginValue.parse]).
 */
@Entity(tableName = "plugin_overrides", primaryKeys = ["accountId", "pluginId"])
data class PluginOverrideEntity(
    val accountId: String,
    val pluginId: String,
    val value: String,
)
