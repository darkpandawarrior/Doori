package com.mileway

import app.cash.turbine.test
import com.mileway.core.common.UiText
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.contentOrElse
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.mvi.isLoading
import com.mileway.core.ui.mvi.map
import com.mileway.core.ui.mvi.onContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A.0e: tests for the hardened MVI base (BaseViewModel) and ScreenState extensions. */
class MviInfraTest {
    // --- ScreenState extensions (pure) ---

    @Test
    fun `map transforms content and preserves isStale`() {
        val state: ScreenState<Int> = ScreenState.Content(2, isStale = true)
        val mapped = state.map { it * 10 }
        assertEquals(ScreenState.Content(20, isStale = true), mapped)
    }

    @Test
    fun `map passes through non-content branches`() {
        assertEquals(ScreenState.Loading, ScreenState.Loading.map { _: Nothing -> 1 })
        val err = ScreenState.Error(UiText.Static("boom"))
        assertEquals(err, err.map { _: Nothing -> 1 })
    }

    @Test
    fun `onContent runs only when content present`() {
        var seen = -1
        ScreenState.Content(7).onContent { seen = it }
        assertEquals(7, seen)
        seen = -1
        (ScreenState.Loading as ScreenState<Int>).onContent { seen = it }
        assertEquals(-1, seen)
    }

    @Test
    fun `dataOrNull and contentOrElse behave per branch`() {
        assertEquals(5, ScreenState.Content(5).dataOrNull)
        assertNull((ScreenState.Empty as ScreenState<Int>).dataOrNull)
        assertEquals(9, (ScreenState.Loading as ScreenState<Int>).contentOrElse(9))
        assertTrue(ScreenState.Loading.isLoading)
        assertFalse(ScreenState.Content(1).isLoading)
    }

    // --- BaseViewModel ---

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private class CounterVm : BaseViewModel<CounterVm.S, CounterVm.E, CounterVm.A>(S()) {
        data class S(val count: Int = 0)

        sealed interface E {
            data class Toast(val msg: String) : E
        }

        sealed interface A {
            data object Inc : A

            data object Fire : A
        }

        override fun onAction(action: A) {
            when (action) {
                A.Inc -> setState { copy(count = count + 1) }
                A.Fire -> emitEffect(E.Toast("once"))
            }
        }

        fun broadcastToast(msg: String) = emitBroadcast(E.Toast(msg))
    }

    @Test
    fun `setState reducer updates state`() =
        runTest {
            val vm = CounterVm()
            assertEquals(0, vm.state.value.count)
            vm.onAction(CounterVm.A.Inc)
            vm.onAction(CounterVm.A.Inc)
            assertEquals(2, vm.state.value.count)
        }

    @Test
    fun `emitEffect is received exactly once`() =
        runTest {
            val vm = CounterVm()
            vm.effect.test {
                vm.onAction(CounterVm.A.Fire)
                assertEquals(CounterVm.E.Toast("once"), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `emitBroadcast delivers to active collector`() =
        runTest {
            val vm = CounterVm()
            vm.broadcast.test {
                vm.broadcastToast("hello")
                assertEquals(CounterVm.E.Toast("hello"), awaitItem())
            }
        }
}
