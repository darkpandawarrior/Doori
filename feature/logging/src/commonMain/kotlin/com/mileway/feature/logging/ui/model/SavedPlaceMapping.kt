package com.mileway.feature.logging.ui.model

import com.mileway.core.data.location.SavedPlace

/**
 * Boundary mappers between core:data's persistence model ([SavedPlace]) and the logging feature's UI
 * model ([LocationEntry]). Kept here so core:data stays UI-free — the store persists `SavedPlace`, the
 * screen/ViewModel work in `LocationEntry`.
 */

/** A named saved place surfaced as a Home/Work/custom chip. */
data class SavedPlaceUi(
    val label: String,
    val entry: LocationEntry,
)

fun LocationEntry.toSavedPlace(
    label: String? = null,
    favorite: Boolean = false,
): SavedPlace =
    SavedPlace(
        name = name,
        subtitle = subtitle,
        lat = lat,
        lng = lng,
        category = category.name,
        label = label,
        favorite = favorite,
    )

fun SavedPlace.toLocationEntry(): LocationEntry =
    LocationEntry(
        name = name,
        subtitle = subtitle,
        lat = lat,
        lng = lng,
        category = runCatching { PoiCategory.valueOf(category) }.getOrDefault(PoiCategory.OTHER),
    )
