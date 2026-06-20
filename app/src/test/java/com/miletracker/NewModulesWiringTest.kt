package com.miletracker

import android.content.Context
import com.miletracker.core.ui.di.coreUiModule
import com.miletracker.core.ui.theme.ThemeController
import com.miletracker.feature.events.di.eventsModule
import com.miletracker.feature.events.repository.EventsRepository
import com.miletracker.feature.media.di.mediaModule
import com.miletracker.feature.media.repository.MediaRepository
import com.miletracker.feature.payments.di.paymentsModule
import com.miletracker.feature.payments.repository.PaymentsRepository
import com.miletracker.feature.profile.di.profileModule
import com.miletracker.feature.profile.repository.ProfileRepository
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
 * Verifies the Koin modules added for the bottom-nav shell (theme), media capture and
 * profile features wire together without definition errors. These run without the Room
 * DB module so a mock Context is sufficient.
 */
class NewModulesWiringTest : KoinTest {

    @Before
    fun setUp() {
        try { stopKoin() } catch (_: Exception) {}
        startKoin {
            androidContext(mockk<Context>(relaxed = true))
            modules(coreUiModule, mediaModule, profileModule, paymentsModule, eventsModule)
        }
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (_: Exception) {}
    }

    @Test
    fun `coreUiModule provides a singleton ThemeController`() {
        val a by inject<ThemeController>()
        val b by inject<ThemeController>()
        assertNotNull(a)
        assert(a === b) { "ThemeController must be a singleton shared by shell and settings" }
    }

    @Test
    fun `mediaModule provides MediaRepository`() {
        val repo by inject<MediaRepository>()
        assertNotNull(repo)
    }

    @Test
    fun `profileModule provides ProfileRepository`() {
        val repo by inject<ProfileRepository>()
        assertNotNull(repo)
    }

    @Test
    fun `paymentsModule provides PaymentsRepository`() {
        val repo by inject<PaymentsRepository>()
        assertNotNull(repo)
    }

    @Test
    fun `eventsModule provides EventsRepository`() {
        val repo by inject<EventsRepository>()
        assertNotNull(repo)
    }
}
