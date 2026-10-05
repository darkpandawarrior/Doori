package com.mileway

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.JustificationReason
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.core.forms.hasCompleteAffidavit
import com.mileway.core.forms.hasValidJustification
import com.mileway.feature.logging.affidavit.AffidavitField
import com.mileway.feature.logging.justification.JustificationReasonPicker
import com.mileway.feature.logging.report.ReportJourneyStore
import com.mileway.feature.logging.report.ReportSubmitScreen
import com.mileway.feature.logging.report.ReportSubmitViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class ExpenseExceptionsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalResourceApi::class)
    @Before
    fun resources() {
        ComposeResourcesTestFixture.install()
        setResourceReaderAndroidContext(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun pickerAndTextWorkOfflineWithoutAKey() {
        var line = ExpenseLine("line", 100, "INR", merchant = "Cafe", category = "FOOD")
        composeRule.setContent {
            var value by remember { mutableStateOf(line) }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AffidavitField(
                        value.affidavitAccepted,
                        value.affidavitNote,
                        onAcceptedChange = {
                            value = value.copy(affidavitAccepted = it)
                            line = value
                        },
                        onNoteChange = {
                            value = value.copy(affidavitNote = it)
                            line = value
                        },
                    )
                    JustificationReasonPicker(
                        value.justificationReason,
                        value.justificationNote,
                        onReasonChange = {
                            value = value.copy(justificationReason = it)
                            line = value
                        },
                        onNoteChange = {
                            value = value.copy(justificationNote = it)
                            line = value
                        },
                    )
                }
            }
        }
        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.onNodeWithText("Why is the receipt missing? (required)").performScrollTo().performTextInput("Receipt was lost")
        composeRule.runOnIdle { assertTrue(line.hasCompleteAffidavit()) }
        composeRule.onNodeWithText("Other").performScrollTo().performClick()
        composeRule.runOnIdle { assertFalse(line.hasValidJustification()) }
        composeRule.onNodeWithText("Justification note (required for Other)").performScrollTo().performTextInput("Urgent client visit")
        composeRule.runOnIdle {
            assertTrue(line.hasValidJustification())
            assertEquals(JustificationReason.OTHER, line.justificationReason)
        }
    }

    @Test
    fun reportScreenSavesAffidavitAndOfflineReasonBeforeSubmitting() {
        val row = MutableStateFlow(Report("report", "employee", listOf(ExpenseLine("line", 500, "INR", merchant = "Taxi", category = "TRAVEL"))))
        val store =
            object : ReportJourneyStore {
                override fun observe(id: String) = row.map { it.takeIf { report -> report.id == id } }

                override fun observeByEmployee(employeeId: String) = row.map { listOf(it).filter { report -> report.employeeId == employeeId } }

                override suspend fun save(report: Report): Report {
                    val saved = report.copy(recordVersion = report.recordVersion + 1)
                    row.value = saved
                    return saved
                }

                override suspend fun recall(id: String) = row.value
            }
        val session =
            object : SessionSource {
                override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.GUEST, employeeCode = "employee"))
            }
        val vm = ReportSubmitViewModel(store, session)
        composeRule.setContent { MaterialTheme { ReportSubmitScreen("report", vm, {}) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Submit report").assertIsNotEnabled()
        composeRule.onNode(isToggleable()).performScrollTo().performClick()
        composeRule.onNodeWithText("Why is the receipt missing? (required)").performScrollTo().performTextInput("Vendor gave no receipt")
        composeRule.onNodeWithText("Other").performScrollTo().performClick()
        composeRule.onNodeWithText("Justification note (required for Other)").performScrollTo().performTextInput("Urgent client visit")
        composeRule.onNodeWithText("Submit report").assertIsNotEnabled()
        composeRule.onNode(isToggleable()).assertIsOn()
        composeRule.onNodeWithText("Save exception details").performScrollTo().performClick()
        composeRule.waitUntil { row.value.recordVersion > 0 && !vm.state.value.busy }
        composeRule.runOnIdle {
            val saved = row.value.lines.single() as ExpenseLine
            assertTrue("Saved affidavit: $saved; error: ${vm.state.value.error}", saved.hasCompleteAffidavit())
            assertEquals(JustificationReason.OTHER, saved.justificationReason)
            assertEquals("Urgent client visit", saved.justificationNote)
        }
        composeRule.onNodeWithText("Submit report").assertIsEnabled().performClick()
        composeRule.waitUntil { row.value.state == ReportLifecycleState.SUBMITTED }
        composeRule.runOnIdle { assertEquals(ReportLifecycleState.SUBMITTED, row.value.state) }
    }

    @Test
    fun suggestedReasonNeedsATapAndUserCanOverrideIt() {
        composeRule.setContent {
            var reason by remember { mutableStateOf<JustificationReason?>(null) }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    JustificationReasonPicker(reason, "", { reason = it }, {}, suggestion = JustificationReason.CLIENT_REQUEST)
                }
            }
        }
        composeRule.onNodeWithText("Client request").assertIsNotSelected()
        composeRule.onNodeWithText("Suggested: Client request. Tap to use.").performClick()
        composeRule.onNodeWithText("Client request").assertIsSelected()
        composeRule.onNodeWithText("Business necessity").performClick()
        composeRule.onNodeWithText("Business necessity").assertIsSelected()
        composeRule.onNodeWithText("Client request").assertIsNotSelected()
    }
}
