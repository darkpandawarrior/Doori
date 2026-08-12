package com.mileway.core.ui.detail

import androidx.compose.runtime.Composable
import com.mileway.core.ui.components.StatusTone
import com.siddharth.kmp.common.UiText

@DslMarker
annotation class DetailDslMarker

/** Type-safe builder entry point for a [DetailSpec] — see `sampleTrackDetailSpec` for a full example. */
fun buildDetail(block: DetailSpecBuilder.() -> Unit): DetailSpec = DetailSpecBuilder().apply(block).build()

@DetailDslMarker
class DetailSpecBuilder {
    private val sections = mutableListOf<DetailSectionSpec>()

    fun section(
        id: String,
        title: UiText? = null,
        visible: Boolean = true,
        block: DetailSectionBuilder.() -> Unit,
    ) {
        sections += DetailSectionBuilder(id, title, visible).apply(block).build()
    }

    fun build(): DetailSpec = DetailSpec(sections.toList())
}

@DetailDslMarker
class DetailSectionBuilder internal constructor(
    private val id: String,
    private val title: UiText?,
    private val visible: Boolean,
) {
    private val fields = mutableListOf<DetailField>()

    fun keyValue(
        id: String,
        label: UiText,
        value: UiText,
        visible: Boolean = true,
    ) {
        fields += DetailField.KeyValue(id, label, value, visible)
    }

    fun metricGrid(
        id: String,
        vararg metrics: DetailField.MetricGrid.Metric,
        columns: Int = 2,
        visible: Boolean = true,
    ) {
        fields += DetailField.MetricGrid(id, metrics.toList(), columns, visible)
    }

    fun status(
        id: String,
        value: UiText,
        tone: StatusTone,
        label: UiText? = null,
        visible: Boolean = true,
    ) {
        fields += DetailField.Status(id, value, tone, label, visible)
    }

    fun amount(
        id: String,
        label: UiText,
        value: UiText,
        visible: Boolean = true,
    ) {
        fields += DetailField.Amount(id, label, value, visible)
    }

    fun date(
        id: String,
        label: UiText,
        value: UiText,
        visible: Boolean = true,
    ) {
        fields += DetailField.DateField(id, label, value, visible)
    }

    fun attachments(
        id: String,
        attachments: List<DetailField.AttachmentList.Attachment>,
        title: UiText? = null,
        visible: Boolean = true,
    ) {
        fields += DetailField.AttachmentList(id, attachments, title, visible)
    }

    fun slot(
        id: String,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        fields += DetailField.Slot(id, visible, content)
    }

    fun expandable(
        id: String,
        title: UiText,
        initiallyExpanded: Boolean = false,
        visible: Boolean = true,
        block: DetailSectionBuilder.() -> Unit,
    ) {
        val nested = DetailSectionBuilder(id, null, true).apply(block).build()
        fields += DetailField.Expandable(id, title, nested.fields, initiallyExpanded, visible)
    }

    fun divider(
        id: String = "divider_${fields.size}",
        visible: Boolean = true,
    ) {
        fields += DetailField.Divider(id, visible)
    }

    fun actionRow(
        id: String,
        vararg actions: DetailAction,
        visible: Boolean = true,
    ) {
        fields += DetailField.ActionRow(id, actions.toList(), visible)
    }

    fun build(): DetailSectionSpec = DetailSectionSpec(id, fields.toList(), title, visible)
}
