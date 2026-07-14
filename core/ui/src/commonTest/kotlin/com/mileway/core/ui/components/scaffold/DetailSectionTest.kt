package com.mileway.core.ui.components.scaffold

import com.siddharth.kmp.common.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailSectionTest {
    private val allSections =
        listOf(
            DetailSection.Details,
            DetailSection.Timeline,
            DetailSection.Comments,
            DetailSection.Clarification,
            DetailSection.Attachments,
            DetailSection.Audit,
        )

    @Test
    fun `every section has a distinct Res label key`() {
        val keys = allSections.map { (it.label as UiText.Res).key }
        assertEquals(keys.size, keys.toSet().size, "expected every DetailSection label key to be unique")
    }

    @Test
    fun `label keys match the section name convention`() {
        assertEquals("detail_section_details", (DetailSection.Details.label as UiText.Res).key)
        assertEquals("detail_section_timeline", (DetailSection.Timeline.label as UiText.Res).key)
        assertEquals("detail_section_comments", (DetailSection.Comments.label as UiText.Res).key)
        assertEquals("detail_section_clarification", (DetailSection.Clarification.label as UiText.Res).key)
        assertEquals("detail_section_attachments", (DetailSection.Attachments.label as UiText.Res).key)
        assertEquals("detail_section_audit", (DetailSection.Audit.label as UiText.Res).key)
    }

    @Test
    fun `objects are singletons`() {
        assertTrue(DetailSection.Details === DetailSection.Details)
    }
}
