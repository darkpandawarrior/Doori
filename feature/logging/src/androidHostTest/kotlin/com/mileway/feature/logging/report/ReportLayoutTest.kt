package com.mileway.feature.logging.report

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The shared state renderer must keep report content visible inside the form's scroll body. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ReportLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `report body remains visible under unbounded vertical scroll constraints`() {
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ScreenStateContent(ScreenState.Content("Report body")) { Text(it) }
            }
        }
        compose.onNodeWithText("Report body").assertIsDisplayed()
    }
}
