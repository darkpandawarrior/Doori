package com.mileway

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.StatementImportEntity
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.core.network.payout.PayoutBeneficiaryStore
import com.mileway.feature.cards.import.StatementImportScreen
import com.mileway.feature.cards.import.StatementImportViewModel
import com.mileway.feature.cards.import.StatementImporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class StatementImportScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun importButtonWritesMatchAndBeneficiaryButtonPersistsAddress() {
        val line = ExpenseLine("line", 12000, "INR", merchant = "Cafe Orchard", category = "FOOD", incurredOn = "2026-09-25")
        var savedLine: ExpenseLine? = null
        var savedBatch: StatementImportEntity? = null
        val importer =
            StatementImporter(
                loadReports = { listOf(Report("report", "employee", listOf(line))) },
                batchExists = { false },
                saveMatches = { batch, _, matches ->
                    savedBatch = batch
                    savedLine = matches["line"]
                    true
                },
            )
        val beneficiary = mockk<PayoutBeneficiaryStore>(relaxed = true)
        every { beneficiary.read() } returns null
        val session =
            object : SessionSource {
                override val sessionState = MutableStateFlow(SessionState(employeeCode = "employee"))
            }
        val viewModel = StatementImportViewModel(importer, beneficiary, session, importDispatcher = Dispatchers.Unconfined)
        compose.setContent { MaterialTheme { StatementImportScreen({}, viewModel) } }
        compose
            .onNodeWithText(
                "Paste CSV or OFX text",
            ).performScrollTo()
            .performTextInput("id,date,merchant,amount,currency\ntx,2026-09-25,Cafe Orchard,120.00,INR")
        compose.onNodeWithText("Import and match").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.result != null || viewModel.state.value.error != null }
        assertNull(viewModel.state.value.error)
        compose.onNodeWithText("1 matched; 0 unmatched. Matched amounts are locked.").performScrollTo().assertIsDisplayed()
        assertNotNull(savedBatch)
        assertNotNull(savedLine?.cardMatchId)
        assertEquals(12000, savedLine?.amountMinor)
        compose.onNodeWithText("Payout address (name@handle)").performScrollTo().performTextInput("fixture@bank")
        compose.onNodeWithText("Save payout address").performScrollTo().performClick()
        compose.onNodeWithText("Payout address saved securely").performScrollTo().assertIsDisplayed()
        verify { beneficiary.store("fixture@bank") }
    }
}
