package com.mileway.feature.profile.model

/**
 * PLAN_V24 P3.4: the kind of a saved place. HOME/WORK are the two canonical "quick" places (at
 * most one of each is meaningful); OTHER covers any additional named place. Mirrors the reference
 * app's HOME/WORK/OTHER place-type enum.
 */
enum class SavedPlaceType { HOME, WORK, OTHER }

/**
 * PLAN_V24 P3.4: a saved place as rendered by `SavedPlacesScreen`. [address] is free text; [lat]
 * and [lng] are optional (manual entry — no map picker in this phase), so a place can carry only a
 * text address. The HOME place is the canonical home location surfaced alongside
 * `EmployeeProfile.homeLocation`.
 */
data class SavedPlace(
    val id: String,
    val type: SavedPlaceType,
    val label: String,
    val address: String,
    val lat: Double?,
    val lng: Double?,
)
