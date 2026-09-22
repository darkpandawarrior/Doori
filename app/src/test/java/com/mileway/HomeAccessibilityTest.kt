package com.mileway

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.ui.home.AtAGlanceRow
import com.mileway.ui.home.AtAGlanceRowView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * PLAN_V23 P8.1: accessibility sweep coverage for the Home screen's "At A Glance" row — TalkBack
 * must announce the count, title and subtitle as one merged node (not three separate `Text`
 * announcements) and the row must expose a click action for the a11y click-to-activate gesture.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class HomeAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `at-a-glance row merges its text into one accessible node with a click action`() {
        composeRule.setContent {
            MilewayTheme {
                AtAGlanceRowView(
                    row = AtAGlanceRow(count = 4, title = "Unreported Transactions", subtitle = "expenses & vouchers this month"),
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("4 Unreported Transactions, expenses & vouchers this month")
            .assertHasClickAction()
    }
}
