package com.mileway

import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.di.advancesModule
import com.mileway.feature.advances.viewmodel.AdvancesHomeViewModel
import com.mileway.feature.advances.viewmodel.AskAdvanceViewModel
import com.mileway.feature.advances.viewmodel.PettyCardDetailViewModel
import com.mileway.feature.advances.viewmodel.QrCardDetailViewModel
import com.mileway.feature.advances.viewmodel.QrRequestViewModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.assertNotNull

/**
 * PLAN_V35: `advancesModule` is composed into the production graph (MilewayApplication +
 * MilewayAppViewController). The home wallet sections and every advances screen `koinInject`/
 * `koinViewModel` these types at COMPOSITION time — a missing binding is invisible to
 * compilation and JVM unit tests and crashes on-device (the exact failure mode of the
 * PermissionsProvider bug run-verification caught in V34). This resolves each injected type
 * from the module so the graph can't silently regress.
 */
class AdvancesModuleWiringTest : KoinTest {
    @Before
    fun setUp() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
        startKoin { modules(advancesModule) }
    }

    @After
    fun tearDown() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun `advancesModule resolves the repositories the home sections inject`() {
        assertNotNull(getKoin().get<AdvancesRepository>())
        assertNotNull(getKoin().get<QrCardsRepository>())
    }

    @Test
    fun `advancesModule resolves every screen ViewModel`() {
        assertNotNull(getKoin().get<AdvancesHomeViewModel>())
        assertNotNull(getKoin().get<AskAdvanceViewModel>())
        assertNotNull(getKoin().get<QrRequestViewModel>())
        assertNotNull(getKoin().get<PettyCardDetailViewModel>())
        assertNotNull(getKoin().get<QrCardDetailViewModel>())
    }
}
