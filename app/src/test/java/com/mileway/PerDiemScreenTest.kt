package com.mileway

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.dao.PerDiemRateDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.PerDiemRateEntity
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.feature.logging.perdiem.PerDiemEntryViewModel
import com.mileway.feature.logging.perdiem.PerDiemSheet
import com.mileway.feature.logging.report.ReportJourneyStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class PerDiemScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalResourceApi::class)
    @Before
    fun resources() {
        ComposeResourcesTestFixture.install()
        setResourceReaderAndroidContext(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `per diem form previews dated money and creates a report for review`() {
        val rateDao = mockk<PerDiemRateDao>()
        every { rateDao.observeAll() } returns MutableStateFlow(listOf(PerDiemRateEntity("Pune_Standard", "Pune", "Standard", 10000, "INR", 0)))
        val store = mockk<ReportJourneyStore>()
        coEvery { store.save(any()) } answers { firstArg<Report>().copy(recordVersion = 1) }
        val session =
            object : SessionSource {
                override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.GUEST, employeeCode = "employee"))
            }
        val viewModel = PerDiemEntryViewModel(rateDao, store, session)
        var openedReport: String? = null
        composeRule.setContent {
            MaterialTheme { PerDiemSheet(viewModel, onBack = {}, onOpenReport = { openedReport = it }) }
        }
        composeRule.onNodeWithText("Start date (YYYY-MM-DD)").performScrollTo().performTextInput("2026-09-01")
        composeRule.onNodeWithText("End date (YYYY-MM-DD)").performScrollTo().performTextInput("2026-09-01")
        composeRule.onNodeWithText("2026-09-01 · ₹ 100.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Create report and review").performClick()
        composeRule.waitUntil { openedReport != null }
        assertNotNull(openedReport)
        coVerify(exactly = 1) { store.save(any()) }
    }
}
