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
import androidx.compose.ui.test.assertIsNotSelected
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
import com.mileway.core.forms.hasCompleteAffidavit
import com.mileway.core.forms.hasValidJustification
import com.mileway.feature.logging.affidavit.AffidavitField
import com.mileway.feature.logging.justification.JustificationReasonPicker
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
                        value.affidavitAccepted, value.affidavitNote,
                        onAcceptedChange = { value = value.copy(affidavitAccepted = it); line = value },
                        onNoteChange = { value = value.copy(affidavitNote = it); line = value },
                    )
                    JustificationReasonPicker(
                        value.justificationReason, value.justificationNote,
                        onReasonChange = { value = value.copy(justificationReason = it); line = value },
                        onNoteChange = { value = value.copy(justificationNote = it); line = value },
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
        composeRule.runOnIdle { assertTrue(line.hasValidJustification()); assertEquals(JustificationReason.OTHER, line.justificationReason) }
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
