package com.mileway

import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the profile/settings ViewModel, driven through a fake repository.
 */
class ProfileViewModelTest {
    private fun viewModel() =
        ProfileViewModel(
            FakeProfileRepository(MockAccountRepository(FakeMockAccountDao())),
            ThemeController(),
            FakeActiveAccountSource(),
            mockk<DemoSettingsRepository> { every { settings } returns MutableStateFlow(DemoSettings()) },
            mockk<SessionRepository>(relaxed = true) { every { sessionState } returns MutableStateFlow(SessionState()) },
        )

    @Test
    fun `notifications default on and toggles off`() {
        val vm = viewModel()
        assertTrue(vm.notificationsEnabled.value)
        vm.toggleNotifications()
        assertFalse(vm.notificationsEnabled.value)
    }

    @Test
    fun `units default to miles and toggle`() {
        val vm = viewModel()
        assertTrue(vm.useMiles.value)
        vm.toggleUnits()
        assertFalse(vm.useMiles.value)
    }

    @Test
    fun `dark theme override starts null then can be forced`() {
        val vm = viewModel()
        assertEquals(null, vm.darkThemeOverride.value)
        vm.setDarkTheme(true)
        assertEquals(true, vm.darkThemeOverride.value)
    }

    @Test
    fun `header is populated from the repository`() {
        val vm = viewModel()
        assertTrue(
            vm.uiState.value.header.name
                .isNotBlank(),
        )
        // The rich profile + completion are populated from the repository.
        assertTrue(
            vm.uiState.value.profile.name
                .isNotBlank(),
        )
        assertTrue(
            vm.uiState.value.completion.categories
                .isNotEmpty(),
        )
    }
}
