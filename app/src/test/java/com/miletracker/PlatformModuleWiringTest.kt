package com.miletracker

import android.content.Context
import com.miletracker.core.platform.BackgroundScheduler
import com.miletracker.core.platform.di.platformModule
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
 * KOIN.1: `platformModule()` is now wired into the production graph (`initKoin()` prepends it to
 * `MileTrackerApplication`'s module list, and the iOS `MainViewController` does the same on iOS). This test
 * loads the Android `actual fun platformModule()` into a Koin graph and resolves a binding that flows
 * `androidContext()` through — proving the module is registered and wired.
 *
 * Only [BackgroundScheduler] is instantiated here: it stores the context and defers all work to WorkManager.
 * The other bindings ([com.miletracker.core.platform.LocationTracker] / TextRecognizer) construct gms /
 * ML-Kit clients eagerly, which need a real `MlKitContext` / Play-services init — so per this project's
 * "no gms in JVM tests" rule they're exercised by both-flavor `assemble` + the iOS framework link, not here.
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
        val background by inject<BackgroundScheduler>()
        assertNotNull(background)
    }
}
