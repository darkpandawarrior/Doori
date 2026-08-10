package com.mileway.core.ui.mvi

import com.siddharth.kmp.common.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pure-logic coverage for the PARTIAL (Content.partialError) and stale-refresh flags. */
class ScreenStateTest {
    @Test
    fun `map preserves isStale and partialError on Content`() {
        val partial = UiText.Dynamic("2 of 5 sections failed to load")
        val state: ScreenState<Int> = ScreenState.Content(1, isStale = true, partialError = partial)

        val mapped = state.map { it * 10 }

        assertEquals(ScreenState.Content(10, isStale = true, partialError = partial), mapped)
    }

    @Test
    fun `asContent defaults to no stale flag and no partial error`() {
        val state = 5.asContent()

        assertEquals(ScreenState.Content(5, isStale = false, partialError = null), state)
    }

    @Test
    fun `asContent carries partialError through without affecting dataOrNull`() {
        val partial = UiText.Dynamic("one section failed")
        val state = 5.asContent(partialError = partial)

        assertEquals(5, state.dataOrNull)
        assertEquals(partial, (state as ScreenState.Content).partialError)
    }

    @Test
    fun `non-content branches never carry a partialError`() {
        assertNull((ScreenState.Empty as ScreenState<Int>).dataOrNull)
        assertEquals(ScreenState.Loading, ScreenState.Loading.map { _: Nothing -> 1 })
    }
}
