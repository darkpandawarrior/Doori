package com.mileway.core.ui.detail

import com.mileway.core.ui.components.StatusTone
import com.siddharth.kmp.common.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetailSpecTest {
    @Test
    fun `sampleTrackDetailSpec reproduces every TrackDetailScreen content block`() {
        val spec = sampleTrackDetailSpec()

        assertEquals(listOf("summary", "metrics", "attachments", "actions"), spec.sections.map { it.id })

        val metrics = spec.sections.single { it.id == "metrics" }.fields
        assertEquals(3, metrics.size, "expected 3 metric-grid rows, matching TrackDetailScreen's 3 Row blocks")
        assertTrue(metrics.all { it is DetailField.MetricGrid })
        assertEquals(2, (metrics[0] as DetailField.MetricGrid).metrics.size, "each grid row is 2-column")

        val actions = (spec.sections.single { it.id == "actions" }.fields.single() as DetailField.ActionRow).actions
        assertEquals(5, actions.size, "TrackDetailScreen has 5 action buttons")
    }

    @Test
    fun `the amount-vehicle metric row visibility mirrors the caller's data condition`() {
        val withAmount = sampleTrackDetailSpec(hasReimbursableAmount = true)
        val withoutAmount = sampleTrackDetailSpec(hasReimbursableAmount = false)

        val amountRow = { spec: DetailSpec -> spec.sections.single { it.id == "metrics" }.fields.single { it.id == "metrics_amount_vehicle" } }
        assertTrue(amountRow(withAmount).visible)
        assertFalse(amountRow(withoutAmount).visible)
    }

    @Test
    fun `buildDetail preserves authored section and field order`() {
        val spec =
            buildDetail {
                section(id = "a") { keyValue(id = "a1", label = UiText.of("A1"), value = UiText.of("v")) }
                section(id = "b") { keyValue(id = "b1", label = UiText.of("B1"), value = UiText.of("v")) }
            }

        assertEquals(listOf("a", "b"), spec.sections.map { it.id })
    }

    @Test
    fun `expandable groups nested fields under one field id`() {
        val spec =
            buildDetail {
                section(id = "s") {
                    expandable(id = "more", title = UiText.of("More")) {
                        keyValue(id = "nested1", label = UiText.of("N1"), value = UiText.of("v1"))
                        keyValue(id = "nested2", label = UiText.of("N2"), value = UiText.of("v2"))
                    }
                }
            }

        val expandable = spec.sections.single().fields.single() as DetailField.Expandable
        assertEquals(listOf("nested1", "nested2"), expandable.fields.map { it.id })
    }

    @Test
    fun `applyConfig hides a section by id`() {
        val spec = sampleTrackDetailSpec()
        val config = DetailConfig(hiddenIds = setOf("attachments"))

        val result = spec.applyConfig(config)

        assertEquals(listOf("summary", "metrics", "actions"), result.sections.map { it.id })
    }

    @Test
    fun `applyConfig hides a field within a section by id`() {
        val spec = sampleTrackDetailSpec()
        val config = DetailConfig(hiddenIds = setOf("metrics_amount_vehicle"))

        val result = spec.applyConfig(config)

        val fieldIds = result.sections.single { it.id == "metrics" }.fields.map { it.id }
        assertEquals(listOf("metrics_distance_duration", "metrics_speed_gps"), fieldIds)
    }

    @Test
    fun `applyConfig reorders sections, unlisted ids keep their relative order after the named ones`() {
        val spec = sampleTrackDetailSpec()
        val config = DetailConfig(order = mapOf("" to listOf("actions", "summary")))

        val result = spec.applyConfig(config)

        assertEquals(listOf("actions", "summary", "metrics", "attachments"), result.sections.map { it.id })
    }

    @Test
    fun `applyConfig relabels a section title and a field label by id`() {
        val spec =
            buildDetail {
                section(id = "s", title = UiText.of("Original section")) {
                    keyValue(id = "k1", label = UiText.of("Original label"), value = UiText.of("v"))
                }
            }
        val config =
            DetailConfig(
                labelOverrides =
                    mapOf(
                        "s" to UiText.of("Renamed section"),
                        "k1" to UiText.of("Renamed label"),
                    ),
            )

        val result = spec.applyConfig(config)
        val section = result.sections.single()
        val field = section.fields.single() as DetailField.KeyValue

        assertEquals(UiText.of("Renamed section"), section.title)
        assertEquals(UiText.of("Renamed label"), field.label)
        assertEquals(UiText.of("v"), field.value, "only the label is overridden, not the value")
    }

    @Test
    fun `status field defaults carry the given tone`() {
        val spec =
            buildDetail {
                section(id = "s") {
                    status(id = "st", value = UiText.of("Submitted"), tone = StatusTone.Success)
                }
            }

        val status = spec.sections.single().fields.single() as DetailField.Status
        assertEquals(StatusTone.Success, status.tone)
        assertEquals(null, status.label)
    }
}
