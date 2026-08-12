package com.mileway.core.ui.detail

import com.siddharth.kmp.common.UiText

/** Key [DetailConfig.order] uses for the top-level section list itself. */
private const val TOP_LEVEL = ""

/**
 * A server/tenant override applied on top of an already-built [DetailSpec] — the explicit
 * configurability requirement: hide, reorder, or relabel sections and fields without a code
 * change. [order] is keyed by parent id ([TOP_LEVEL] for the section list, otherwise a section id
 * for the fields inside it) so one map covers both levels. Ids not named in an [order] list keep
 * their original relative order, appended after the named ones.
 */
data class DetailConfig(
    val hiddenIds: Set<String> = emptySet(),
    val order: Map<String, List<String>> = emptyMap(),
    val labelOverrides: Map<String, UiText> = emptyMap(),
)

/** Applies [config]'s hide/reorder/relabel rules to this spec. Data-driven [DetailField.visible]/[DetailSectionSpec.visible] set by the caller are untouched — this only adds config-driven hiding on top. */
fun DetailSpec.applyConfig(config: DetailConfig): DetailSpec {
    val orderedSections =
        sections
            .filterNot { it.id in config.hiddenIds }
            .reorderedBy({ it.id }, config.order[TOP_LEVEL])
            .map { it.applyConfig(config) }
    return DetailSpec(orderedSections)
}

private fun DetailSectionSpec.applyConfig(config: DetailConfig): DetailSectionSpec {
    val relabeled = config.labelOverrides[id]?.let { copy(title = it) } ?: this
    val orderedFields =
        relabeled.fields
            .filterNot { it.id in config.hiddenIds }
            .reorderedBy({ it.id }, config.order[id])
            .map { it.applyLabelOverride(config) }
    return relabeled.copy(fields = orderedFields)
}

private fun DetailField.applyLabelOverride(config: DetailConfig): DetailField {
    val override = config.labelOverrides[id] ?: return this
    return when (this) {
        is DetailField.KeyValue -> copy(label = override)
        is DetailField.Status -> copy(label = override)
        is DetailField.Amount -> copy(label = override)
        is DetailField.DateField -> copy(label = override)
        is DetailField.AttachmentList -> copy(title = override)
        is DetailField.Expandable -> copy(title = override)
        is DetailField.MetricGrid, is DetailField.Slot, is DetailField.PagedSlot, is DetailField.Divider, is DetailField.ActionRow -> this
    }
}

/** Ids in [order] come first, in that order; everything else keeps its original relative order after them. */
private fun <T> List<T>.reorderedBy(
    idOf: (T) -> String,
    order: List<String>?,
): List<T> {
    if (order.isNullOrEmpty()) return this
    val byId = associateBy(idOf)
    val prioritized = order.mapNotNull { byId[it] }
    val remaining = filterNot { idOf(it) in order }
    return prioritized + remaining
}
