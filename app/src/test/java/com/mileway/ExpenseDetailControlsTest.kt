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
import com.mileway.feature.logging.model.ExpenseCategory
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
class ExpenseDetailControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalResourceApi::class)
    @Before
    fun resources() {
        ComposeResourcesTestFixture.install()
        setResourceReaderAndroidContext(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun renderedFieldsEditSplitsAndRecomputePerHeadPolicy() {
        val vm =
            com.mileway.feature.logging.viewmodel
                .ExpenseViewModel(
                    com.mileway.feature.logging.repository
                        .ExpenseRepository(),
                )
        vm.onAction(
            com.mileway.feature.logging.viewmodel.ExpenseAction
                .SelectCategory(ExpenseCategory.FOOD),
        )
        vm.onAction(com.mileway.feature.logging.viewmodel.ExpenseAction.AdvanceStep)
        vm.onAction(
            com.mileway.feature.logging.viewmodel.ExpenseAction
                .SetAmount("6000.00"),
        )
        vm.onAction(
            com.mileway.feature.logging.viewmodel.ExpenseAction
                .SetMerchant("New Cafe"),
        )
        composeRule.setContent {
            MaterialTheme {
                com.mileway.feature.logging.ui.screens
                    .ExpenseScreen(onBack = {}, onSubmitted = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithText("Add split").performScrollTo().performClick()
        composeRule.onNodeWithText("Cost centre name or ID").performScrollTo().performTextInput("Sales")
        composeRule.onNodeWithText("INR 6000.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add attendee").performScrollTo().performClick()
        composeRule.onNodeWithText("Attendee 1").performScrollTo().performTextInput("Alex")
        composeRule.onNodeWithText("Per-head expense exceeds the policy limit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add attendee").performScrollTo().performClick()
        composeRule.onNodeWithText("Attendee 2").performScrollTo().performTextInput("Jordan")
        composeRule.onNodeWithText("Per head: INR 3000.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add itemized line").performScrollTo().performClick()
        composeRule.onNodeWithText("Line 1 description").performScrollTo().performTextInput("Meals")
        composeRule.onNodeWithText("Line 1 amount (INR)").performScrollTo().performTextInput("6000.00")
        composeRule.onNodeWithText("Difference from receipt: INR 0.00").performScrollTo().assertIsDisplayed()
    }
}
