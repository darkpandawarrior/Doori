package com.miletracker.ui.home

import androidx.lifecycle.ViewModel
import com.miletracker.core.network.model.EmployeeProfile
import com.miletracker.stub.ActionRequiredBanner
import com.miletracker.stub.AtAGlanceCounts
import com.miletracker.stub.HomeMockData
import com.miletracker.stub.MarketingCarouselItem
import com.miletracker.stub.ProfileMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Immutable state for the Home tab.
 *
 * Everything the screen renders is assembled once, up front, from the deterministic
 * offline mock providers ([HomeMockData], [ProfileMockData]). There is no async load —
 * the home dashboard is fully local — so the screen never needs a loading or error state.
 *
 * @property greetingName first name extracted from the signed-in profile, used by the
 *   amber "Hello, <name>" greeting in the header.
 * @property notificationCount unread-notification count shown in the header bell badge.
 * @property actionRequired the red-accented "Action Required" banner contents.
 * @property atAGlance the three "At A Glance" summary counters.
 * @property marketingItems the static marketing/benefits card strip.
 */
data class HomeUiState(
    val greetingName: String = "",
    val notificationCount: Int = 0,
    val actionRequired: ActionRequiredBanner = HomeMockData.actionRequiredBanner(),
    val atAGlance: AtAGlanceCounts = HomeMockData.atAGlance(),
    val marketingItems: List<MarketingCarouselItem> = emptyList(),
)

/**
 * Thin ViewModel for the Home tab.
 *
 * It holds no business logic of its own: it snapshots the offline mock data into a single
 * [HomeUiState] at construction and exposes one-shot intent methods that simply forward to
 * the navigation callbacks the integrator passes into [HomeScreen]. Keeping the intents on
 * the ViewModel (rather than calling the callbacks straight from the composable) keeps the
 * screen stateless and gives a single, testable entry point per user action.
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * User tapped the primary "Start" pill on the Mileage carousel card.
     * Forwards to the host so it can open the live tracking flow.
     */
    fun onStartTracking(navigateToTracking: () -> Unit) = navigateToTracking()

    /**
     * User tapped a quick action / banner that begins expense logging
     * ("Add Expense", "Take Action"). Forwards to the host's log-miles entry point.
     */
    fun onAddExpense(navigateToAddExpense: () -> Unit) = navigateToAddExpense()

    /**
     * User tapped an "At A Glance" row or an account-oriented quick action.
     * Forwards to the host's account/details entry point.
     */
    fun onOpenAccount(navigateToAccount: () -> Unit) = navigateToAccount()

    private companion object {

        fun buildInitialState(): HomeUiState = HomeUiState(
            greetingName = firstName(ProfileMockData.primaryProfile()),
            notificationCount = HomeMockData.notificationCount(),
            actionRequired = HomeMockData.actionRequiredBanner(),
            atAGlance = HomeMockData.atAGlance(),
            marketingItems = HomeMockData.carouselItems(),
        )

        /** First whitespace-delimited token of the profile name, e.g. "Demo User" -> "Demo". */
        fun firstName(profile: EmployeeProfile): String =
            profile.name.trim().substringBefore(' ').ifBlank { profile.name.trim() }
    }
}

/**
 * Koin module for the Home tab. Registered by the integrator alongside the other feature
 * modules; exposes [HomeViewModel] via `koinViewModel()`.
 */
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
