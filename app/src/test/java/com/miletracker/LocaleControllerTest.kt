package com.miletracker

import com.miletracker.core.ui.theme.AppLanguage
import com.miletracker.core.ui.theme.LocaleController
import org.junit.Test
import kotlin.test.assertEquals

/**
 * UX.6: the shared locale state is a plain holder, verified directly — default, tag/language setters, and the
 * unknown-tag fallback to English.
 */
class LocaleControllerTest {

    @Test
    fun `defaults to English`() {
        val controller = LocaleController()
        assertEquals(AppLanguage.ENGLISH.tag, controller.currentTag.value)
        assertEquals(AppLanguage.ENGLISH, controller.currentLanguage)
    }

    @Test
    fun `setLanguage updates the tag and resolved language`() {
        val controller = LocaleController()
        controller.setLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI.tag, controller.currentTag.value)
        assertEquals(AppLanguage.HINDI, controller.currentLanguage)
    }

    @Test
    fun `unknown tag resolves back to English`() {
        val controller = LocaleController()
        controller.setLocale("zz")
        assertEquals("zz", controller.currentTag.value)
        assertEquals(AppLanguage.ENGLISH, controller.currentLanguage)
    }
}
