package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.campaign.Campaign
import com.mileway.core.data.campaign.CampaignRepository
import com.mileway.core.data.coupon.Coupon
import com.mileway.feature.profile.repository.CouponsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P12.9 — the offers hub is one screen over TWO existing repos (P5.2 coupons + P5.4
 * campaigns), no new data. Both are seeded once here (idempotent) then observed together.
 */
data class OffersHubUiState(
    val coupons: List<Coupon> = emptyList(),
    val campaigns: List<Campaign> = emptyList(),
)

class OffersHubViewModel(
    private val couponsRepository: CouponsRepository,
    private val campaignRepository: CampaignRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OffersHubUiState())
    val state: StateFlow<OffersHubUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            couponsRepository.seedIfEmpty()
            campaignRepository.seedIfEmpty()
        }
        combine(
            couponsRepository.observeAll(),
            campaignRepository.observeAll(),
        ) { coupons, campaigns -> OffersHubUiState(coupons = coupons, campaigns = campaigns) }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }
}
