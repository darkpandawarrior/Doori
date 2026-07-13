package com.mileway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.SessionSource
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.platform.LocationNameResolver
import com.mileway.core.platform.LocationTracker
import com.mileway.core.ui.components.LocationPin
import com.mileway.core.ui.home.HomePluginConfig
import com.mileway.core.ui.home.HomePluginConfigController
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.profile.repository.NotificationRepository
import com.mileway.stub.ActionRequiredBanner
import com.mileway.stub.AtAGlanceCounts
import com.mileway.stub.HomeMockData
import com.mileway.stub.MarketingCarouselItem
import com.mileway.stub.ProfileMockData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * V29 P29.H.3: manager-only "who's waiting on me" summary, gated on [HomeUiState.isManager].
 * Counts come straight off [ApprovalsRepository]'s static demo dataset — no async load needed.
 */
data class ApprovalBreakdown(
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0,
)

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
 * @property pluginConfig V29 P29.H.1 — gates which optional Home sections render.
 * @property isManager V29 P29.H.3 — gates the manager-only [approvalBreakdown] card, sourced
 *   from the same `trackMileageManagerView` plugin flag the reportee-tracking surface uses.
 * @property approvalBreakdown manager-only pending/approved/rejected counts.
 * @property avatarPath local profile-photo path (set via the Profile tab's avatar picker);
 *   `null` renders the terminal `>_` glyph fallback in the header.
 */
data class HomeUiState(
    val greetingName: String = "",
    val notificationCount: Int = 0,
    val actionRequired: ActionRequiredBanner = HomeMockData.actionRequiredBanner(),
    val atAGlance: AtAGlanceCounts = HomeMockData.atAGlance(),
    val marketingItems: List<MarketingCarouselItem> = emptyList(),
    val currentPin: LocationPin? = DemoHomeLocationPin,
    val pluginConfig: HomePluginConfig = HomePluginConfig(),
    val isManager: Boolean = false,
    val approvalBreakdown: ApprovalBreakdown = ApprovalBreakdown(),
    val avatarPath: String? = null,
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
    private val homePluginConfigController: HomePluginConfigController,
    private val notificationRepository: NotificationRepository,
    private val pluginRegistry: PluginRegistry,
    private val sessionSource: SessionSource,
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

        // P29.H.1: live section-gating config, flippable from the debug menu.
        homePluginConfigController.config
            .onEach { config -> _uiState.update { it.copy(pluginConfig = config) } }
            .launchIn(viewModelScope)

        // P29.H.4: real unread count (Room-backed), replacing the static HomeMockData seed.
        viewModelScope.launch { notificationRepository.seedIfEmpty() }
        notificationRepository.observeUnreadCount()
            .onEach { count -> _uiState.update { it.copy(notificationCount = count) } }
            .launchIn(viewModelScope)

        // P29.H.3: same manager gate as the reportee-tracking surface (ManagerReporteesViewModel).
        pluginRegistry.observe("trackMileageManagerView")
            .onEach { isManager -> _uiState.update { it.copy(isManager = isManager) } }
            .launchIn(viewModelScope)

        // P29.H.6: local profile-photo path set via the Profile tab's avatar picker.
        sessionSource.sessionState
            .map { it.avatarPath }
            .onEach { path -> _uiState.update { it.copy(avatarPath = path) } }
            .launchIn(viewModelScope)
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
                approvalBreakdown =
                    ApprovalBreakdown(
                        pending = ApprovalsRepository.all.count { it.status == ApprovalStatus.PENDING },
                        approved = ApprovalsRepository.all.count { it.status == ApprovalStatus.APPROVED },
                        rejected = ApprovalsRepository.all.count { it.status == ApprovalStatus.REJECTED },
                    ),
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
