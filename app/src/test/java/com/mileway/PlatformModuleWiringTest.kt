package com.mileway

import android.content.Context
import com.mileway.core.platform.NotificationScheduler
import com.mileway.core.platform.di.platformModule
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertNotNull

/**
 * KOIN.1: `platformModule()` is wired into the production graph via `initKoin()`.
 * This test loads the Android `actual fun platformModule()` into a Koin graph and resolves
 * a binding that flows `androidContext()` through, proving the module is registered and wired.
 *
 * [NotificationScheduler] is used here — it stores the context and creates channels lazily,
 * so it is safe to instantiate with a mock context. Location constructs a gms client eagerly
 * (needs real Play-services init), so it is exercised by `assemble` + the iOS framework link instead.
 */
class PlatformModuleWiringTest : KoinTest {

    @Before
    fun setUp() {
        try { stopKoin() } catch (_: Exception) {}
        startKoin {
            androidContext(mockk<Context>(relaxed = true))
            modules(platformModule())
        }
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (_: Exception) {}
    }

    @Test
    fun `platformModule loads and resolves a context-backed service`() {
        val notificationScheduler by inject<NotificationScheduler>()
        assertNotNull(notificationScheduler)
    }
}
