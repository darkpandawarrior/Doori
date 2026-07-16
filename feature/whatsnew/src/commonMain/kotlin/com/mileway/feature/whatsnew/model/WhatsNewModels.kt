package com.mileway.feature.whatsnew.model

import kotlinx.datetime.LocalDate

/*
 * PLAN_V36 P1: flattened, KMP-clean mirror of the reference marketing flow's list/detail
 * feature model — one string description (no rich blocks), plain media list, monotonic
 * [version] driving the badge. See data/WhatsNewCatalog.kt for the seed content.
 */

/** One What's New release note. */
data class WhatsNewEntry(
    val id: String,
    val version: Int,
    val title: String,
    val description: String,
    val media: List<WhatsNewMedia> = emptyList(),
    val releasedOn: LocalDate,
    val modules: List<String> = emptyList(),
    val contactEmail: String? = null,
    val link: String? = null,
)

/** One media item (screenshot/GIF) attached to a [WhatsNewEntry]. */
data class WhatsNewMedia(
    val path: String,
    val caption: String? = null,
)
