package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.campaign.Campaign
import com.mileway.core.data.campaign.CampaignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P5.4: drives `MarketingHubScreen` — the campaign list and the one-shot "Get in touch"
 * interest capture (the reference app `MarketingViewModel`). Campaigns come from the shared core:data
 * [CampaignRepository] (also backing the HomeScreen marketing strip).
 */
data class MarketingHubUiState(
    val campaigns: List<Campaign> = emptyList(),
)

class MarketingHubViewModel(private val repository: CampaignRepository) : ViewModel() {
    private val _state = MutableStateFlow(MarketingHubUiState())
    val state: StateFlow<MarketingHubUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        repository.observeAll()
            .onEach { campaigns -> _state.update { it.copy(campaigns = campaigns) } }
            .launchIn(viewModelScope)
    }

    /** Records interest in a campaign (one-shot; the CTA disables after). */
    fun captureInterest(id: String) {
        viewModelScope.launch { repository.captureInterest(id) }
    }
}
