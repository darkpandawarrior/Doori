package com.mileway.core.ui.components.sheet

import kotlin.test.Test
import kotlin.test.assertEquals

class MilewayPickerSheetTest {
    @Test
    fun tapping_an_option_selects_it_before_dismissing() {
        val calls = mutableListOf<String>()
        val option = MilewayPickerOption(value = "sedan", label = "Sedan")

        selectAndDismiss(
            option = option,
            onSelect = { calls += "select:$it" },
            onDismiss = { calls += "dismiss" },
        )

        // No "Done" button in this taxonomy tier — a single tap must select AND dismiss in one
        // motion, select first so a caller reading the new selection in onDismiss sees it applied.
        assertEquals(listOf("select:sedan", "dismiss"), calls)
    }

    @Test
    fun each_callback_fires_exactly_once_per_tap() {
        var selectCount = 0
        var dismissCount = 0
        val option = MilewayPickerOption(value = 1, label = "One")

        selectAndDismiss(option, onSelect = { selectCount++ }, onDismiss = { dismissCount++ })

        assertEquals(1, selectCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun the_selected_value_passed_is_the_tapped_options_value_not_some_other_option() {
        var captured: String? = null
        val tapped = MilewayPickerOption(value = "b", label = "B")

        selectAndDismiss(tapped, onSelect = { captured = it }, onDismiss = {})

        assertEquals("b", captured)
    }
}
