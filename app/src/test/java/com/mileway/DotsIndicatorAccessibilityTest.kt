package com.mileway

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.ui.components.DotsIndicator
import com.mileway.core.ui.theme.MilewayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * PLAN_V22 P7.7: onboarding carousel a11y polish — each dot exposes a distinct TalkBack
 * announcement ("Current page N" vs "Page N"), and tapping a dot navigates to that page.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class DotsIndicatorAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `dots expose distinct per-page descriptions and tap navigates`() {
        var selected = 0

        composeRule.setContent {
            MilewayTheme {
                DotsIndicator(
                    pageCount = 3,
                    selectedIndex = selected,
                    onDotClick = { index -> selected = index },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Current page 1").assertExists()
        composeRule.onNodeWithContentDescription("Page 2").assertExists()
        composeRule.onNodeWithContentDescription("Page 3").assertExists()

        composeRule.onNodeWithContentDescription("Page 3").performClick()

        assert(selected == 2) { "Expected tapping the third dot to navigate to index 2, got $selected" }
    }
}
