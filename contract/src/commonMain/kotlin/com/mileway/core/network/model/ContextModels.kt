package com.mileway.core.network.model

// PLAN_V33 A1: this file (and its VendorModels/ProfileModels/OfficeEntityModels siblings) moved
// here from core:network verbatim, same package — core:network re-exports via
// `api(project(":contract"))`, so nothing importing `com.mileway.core.network.model.*` changed.

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Tracking contexts ─────────────────────────────────────────────────────────

/**
 * The kind of business context a tracked journey can be tagged against.
 * Mirrors the session-management fields a tracked journey carries
 * (trip id/title, itinerary id/name, petty-cash id/title, event id).
 */
@Serializable
enum class TrackingContextType {
    @SerialName("trip")
    TRIP,

    @SerialName("itinerary")
    ITINERARY,

    @SerialName("pettyCash")
    PETTY_CASH,

    @SerialName("event")
    EVENT,
}

/**
 * A selectable context indicator shown while tracking, e.g. the active trip,
 * an itinerary leg, a petty-cash wallet, or a company event the journey belongs to.
 */
@Serializable
data class TrackingContext(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("type") val type: TrackingContextType,
)
