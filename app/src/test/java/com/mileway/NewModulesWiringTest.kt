package com.mileway

import android.content.Context
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.events.repository.EventsRepository
import com.mileway.feature.media.di.androidMediaModule
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.media.repository.MediaRepository
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.payments.repository.PaymentsRepository
import com.mileway.feature.profile.di.profileAndroidModule
import com.mileway.feature.profile.di.profileModule
import com.mileway.feature.profile.repository.ProfileRepository
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertNotNull

/**
 * Verifies the Koin modules added for the bottom-nav shell (theme), media capture and
 * profile features wire together without definition errors. These run without the Room
 * DB module so a mock Context is sufficient; [MockAccountDao] (P1.2's `MockAccountRepository`
 * dependency) is faked here for the same reason.
 */
class NewModulesWiringTest : KoinTest {
    @Before
    fun setUp() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
        startKoin {
            androidContext(mockk<Context>(relaxed = true))
            modules(
                module { single<MockAccountDao> { FakeMockAccountDao() } },
                coreUiModule,
                mediaModule,
                androidMediaModule,
                profileModule,
                profileAndroidModule,
                paymentsModule,
                eventsModule,
            )
        }
    }

    @After
    fun tearDown() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
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
