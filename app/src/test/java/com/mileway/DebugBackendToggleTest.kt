package com.mileway

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mileway.core.network.api.NetworkBackendFlags
import com.mileway.debug.DebugBackendToggle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class DebugBackendToggleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    @After
    fun resetBackendChoice() {
        context.getSharedPreferences("debug_backend", Context.MODE_PRIVATE).edit().clear().commit()
        NetworkBackendFlags.useRealBackend = false
    }

    @Test
    fun `backend defaults to offline when no choice has been saved`() {
        assertFalse(DebugBackendToggle.readEnabled(context))
        DebugBackendToggle.applyBeforeKoin(context)
        assertFalse(NetworkBackendFlags.useRealBackend)
    }

    @Test
    fun `saved debug choice survives context recreation without changing the running backend`() {
        DebugBackendToggle.setEnabled(context, true)
        val recreatedContext = context.createConfigurationContext(context.resources.configuration)
        assertEquals(BuildConfig.DEBUG, DebugBackendToggle.readEnabled(recreatedContext))
        assertFalse(NetworkBackendFlags.useRealBackend)

        DebugBackendToggle.applyBeforeKoin(recreatedContext)
        assertEquals(BuildConfig.DEBUG, NetworkBackendFlags.useRealBackend)

        DebugBackendToggle.setEnabled(recreatedContext, false)
        assertFalse(DebugBackendToggle.readEnabled(context))
        assertEquals(BuildConfig.DEBUG, NetworkBackendFlags.useRealBackend)
        DebugBackendToggle.applyBeforeKoin(context)
        assertFalse(NetworkBackendFlags.useRealBackend)
    }

    @Test
    fun `release ignores a persisted opt in and resets the flag before Koin`() {
        context.getSharedPreferences("debug_backend", Context.MODE_PRIVATE).edit().putBoolean("use_real_backend", true).commit()
        NetworkBackendFlags.useRealBackend = true
        DebugBackendToggle.applyBeforeKoin(context)
        assertEquals(BuildConfig.DEBUG, DebugBackendToggle.readEnabled(context))
        assertEquals(BuildConfig.DEBUG, NetworkBackendFlags.useRealBackend)
    }
}
