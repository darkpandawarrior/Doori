package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.ui.auth.OnboardingField
import com.mileway.ui.auth.OnboardingFormConfig
import com.mileway.ui.auth.SignupOnboardingViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V24 P2.1 — the onboarding form's value is its config-driven validation (per the reference app's gating flags),
 * so the required/optional matrix is pinned here.
 */
class SignupOnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm(session: SessionRepository = mockk(relaxed = true)) = SignupOnboardingViewModel(session)

    @Test
    fun `first name is always required`() {
        val vm = vm()
        vm.configure(OnboardingFormConfig())
        assertTrue(OnboardingField.FIRST_NAME in vm.validate())
        vm.onFirstNameChange("Asha")
        assertTrue(OnboardingField.FIRST_NAME !in vm.validate())
    }

    @Test
    fun `last name and email required only when not optional`() {
        val vm = vm()
        vm.onFirstNameChange("Asha")

        vm.configure(OnboardingFormConfig(lastNameOptional = true, emailOptional = true))
        assertTrue(vm.validate().isEmpty(), "optional last name + email → valid with just a first name")

        vm.configure(OnboardingFormConfig(lastNameOptional = false, emailOptional = false))
        assertEquals(setOf(OnboardingField.LAST_NAME, OnboardingField.EMAIL), vm.validate())
    }

    @Test
    fun `email format is checked when present`() {
        val vm = vm()
        vm.onFirstNameChange("Asha")
        vm.configure(OnboardingFormConfig())
        vm.onEmailChange("not-an-email")
        assertTrue(OnboardingField.EMAIL in vm.validate())
        vm.onEmailChange("asha@example.com")
        assertTrue(OnboardingField.EMAIL !in vm.validate())
    }

    @Test
    fun `gender and dob required only when flagged`() {
        val vm = vm()
        vm.onFirstNameChange("Asha")
        vm.configure(OnboardingFormConfig(genderRequired = true, dobRequired = true))
        assertEquals(setOf(OnboardingField.GENDER, OnboardingField.DOB), vm.validate())
        vm.onGenderChange("Female")
        vm.onDobChange(123L)
        assertTrue(vm.validate().isEmpty())
    }

    @Test
    fun `valid submit persists and completes`() =
        runTest {
            val session = mockk<SessionRepository>(relaxed = true)
            val vm = vm(session)
            vm.configure(OnboardingFormConfig())
            vm.onFirstNameChange("Asha")
            vm.submit()
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            coVerify { session.saveOnboarding(displayName = "Asha", email = null, gender = "", dateOfBirthMillis = null) }
        }

    @Test
    fun `skip marks onboarding done only when allowed`() =
        runTest {
            val session = mockk<SessionRepository>(relaxed = true)
            val vm = vm(session)
            vm.configure(OnboardingFormConfig(showSkip = false))
            vm.skip()
            advanceUntilIdle()
            assertTrue(!vm.state.value.done, "skip is a no-op when the persona hides it")

            vm.configure(OnboardingFormConfig(showSkip = true))
            vm.skip()
            advanceUntilIdle()
            assertTrue(vm.state.value.done)
            coVerify { session.skipOnboarding() }
        }
}
