package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.vehicle.EcometerRepository
import com.mileway.core.data.vehicle.EcometerTotals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * PLAN_V24 P11.4: the Ecometer dashboard. Surfaces CO₂ saved, fuel cost saved, distance and trip
 * count computed from the user's REAL completed trips ([EcometerRepository], a shared core:data
 * source — no feature-to-feature dependency), never fabricated totals.
 */
class EcoDashboardViewModel(ecometerRepository: EcometerRepository) : ViewModel() {
    val state: StateFlow<EcometerTotals> =
        ecometerRepository.observeTotals()
            .stateIn(viewModelScope, SharingStarted.Eagerly, EcometerTotals())
}
