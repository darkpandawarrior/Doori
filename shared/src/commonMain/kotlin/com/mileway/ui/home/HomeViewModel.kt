package com.mileway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.platform.LocationNameResolver
import com.mileway.core.platform.LocationTracker
import com.mileway.core.ui.components.LocationPin
import com.mileway.stub.ActionRequiredBanner
import com.mileway.stub.AtAGlanceCounts
import com.mileway.stub.HomeMockData
import com.mileway.stub.MarketingCarouselItem
import com.mileway.stub.ProfileMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Demo "current location" pin (Pune) — the default so previews and the screenshot gallery render a
 * pinned map. The real [HomeViewModel] replaces it with the device's actual location at runtime,
 * or `null` when location permission isn't granted.
 */
val DemoHomeLocationPin =
    LocationPin(
        latitude = 18.5204,
        longitude = 73.8567,
        label = "Pune, India",
        coordinates = "18.5204, 73.8567",
    )

/**
 * Immutable state for the Home tab.
 *
 * Everything the screen renders is assembled once, up front, from the deterministic
 * offline mock providers ([HomeMockData], [ProfileMockData]). There is no async load,
 * the home dashboard is fully local, so the screen never needs a loading or error state.
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
    val currentPin: LocationPin? = DemoHomeLocationPin,
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
class HomeViewModel(
    private val locationTracker: LocationTracker,
    private val locationNameResolver: LocationNameResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Best-effort one-shot: pin the device's real current location on the header map, with an
        // offline-resolved place label. A null fix (no permission / unavailable) leaves no pin.
        viewModelScope.launch {
            val fix = locationTracker.current() ?: return@launch
            val place = locationNameResolver.resolve(fix.latitude, fix.longitude)
            _uiState.update {
                it.copy(
                    currentPin =
                        LocationPin(
                            latitude = fix.latitude,
                            longitude = fix.longitude,
                            label = place.name,
                            coordinates = place.coordinates,
                        ),
                )
            }
        }
    }

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
        fun buildInitialState(): HomeUiState =
            HomeUiState(
                greetingName = firstName(ProfileMockData.primaryProfile()),
                notificationCount = HomeMockData.notificationCount(),
                actionRequired = HomeMockData.actionRequiredBanner(),
                atAGlance = HomeMockData.atAGlance(),
                marketingItems = HomeMockData.carouselItems(),
                // Real app starts pin-less; the init coroutine fills the device's actual location.
                currentPin = null,
            )

        /** First whitespace-delimited token of the profile name, e.g. "Demo User" -> "Demo". */
        fun firstName(profile: EmployeeProfile): String = profile.name.trim().substringBefore(' ').ifBlank { profile.name.trim() }
    }
}

/**
 * Koin module for the Home tab. Registered by the integrator alongside the other feature
 * modules; exposes [HomeViewModel] via `koinViewModel()`.
 */
val homeModule =
    module {
        viewModelOf(::HomeViewModel)
    }
