package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.engagement.TourRepository
import com.mileway.core.data.engagement.TourState
import com.mileway.core.data.engagement.TourStatus
import com.mileway.core.data.engagement.TourStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One trip purpose the simulated tour can classify with. */
enum class TourPurpose { BUSINESS, PERSONAL }

/**
 * PLAN_V24 P12.5 — the interactive training tour's screen state: the persisted [TourState] (the pure
 * state machine, see [com.mileway.core.data.engagement.TourStateMachine]) combined with a locally
 * simulated distance/duration ramp so the mock tracking HUD shows plausible movement between steps.
 */
data class TrainingTourUiState(
    val step: TourStep = TourStep.INTRO,
    val status: TourStatus = TourStatus.IN_PROGRESS,
    val distanceKm: Double = 0.0,
    val durationSec: Int = 0,
    val purpose: TourPurpose = TourPurpose.BUSINESS,
)

/**
 * Drives the tour surface. Rather than injecting fake GPS ticks into the live location layer, this
 * drives the UI layer with a seeded distance ramp (advanced only while the HUD step is showing) and
 * persists step/skip/complete through [TourRepository]. Every entry restarts the walkthrough, so the
 * first-run offer and the Support-hub re-entry both begin cleanly at step one.
 */
class TrainingTourViewModel(
    private val tourRepository: TourRepository,
) : ViewModel() {
    private val simulation = MutableStateFlow(SimState())

    val state: StateFlow<TrainingTourUiState> =
        combine(tourRepository.observe(), simulation) { tour, sim ->
            TrainingTourUiState(
                step = tour.step,
                status = tour.status,
                distanceKm = sim.distanceKm,
                durationSec = sim.durationSec,
                purpose = sim.purpose,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TrainingTourUiState())

    init {
        viewModelScope.launch { tourRepository.restart() }
        viewModelScope.launch { runRamp() }
    }

    /** Advance to the next coach-mark step (persisted). */
    fun next() {
        viewModelScope.launch { tourRepository.advance() }
    }

    /** Skip the tour (persisted, terminal). */
    fun skip() {
        viewModelScope.launch { tourRepository.skip() }
    }

    /** Pick the classification the mock trip is submitted with. */
    fun selectPurpose(purpose: TourPurpose) {
        simulation.update { it.copy(purpose = purpose) }
    }

    /** Ramp the simulated distance/duration while the live-HUD step is on screen. */
    private suspend fun runRamp() {
        while (true) {
            delay(RAMP_INTERVAL_MS)
            val current = state.value
            if (current.step == TourStep.LIVE_HUD && current.status == TourStatus.IN_PROGRESS) {
                simulation.update {
                    it.copy(
                        distanceKm = it.distanceKm + RAMP_KM_PER_TICK,
                        durationSec = it.durationSec + RAMP_SECONDS_PER_TICK,
                    )
                }
            }
        }
    }

    private data class SimState(
        val distanceKm: Double = 0.0,
        val durationSec: Int = 0,
        val purpose: TourPurpose = TourPurpose.BUSINESS,
    )

    private companion object {
        const val RAMP_INTERVAL_MS = 320L
        const val RAMP_KM_PER_TICK = 0.14
        const val RAMP_SECONDS_PER_TICK = 11
    }
}
