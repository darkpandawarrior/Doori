package com.mileway.core.ui.detail

import com.siddharth.kmp.common.UiText

/** One ordered section of a [DetailSpec]: an optional title and its list of [fields]. */
data class DetailSectionSpec(
    val id: String,
    val fields: List<DetailField>,
    val title: UiText? = null,
    val visible: Boolean = true,
)

/**
 * A declarative description of a details screen — an ordered list of [sections]. Build one with
 * [buildDetail], render it with `DetailScreen(spec)`, and optionally narrow it for a tenant with
 * `spec.applyConfig(config)`. Nothing in here is domain-specific; every field carries only [UiText]
 * (locale-resolved at the UI edge) and pre-formatted display values, never a raw domain type.
 */
data class DetailSpec(val sections: List<DetailSectionSpec>)
