package com.miletracker.feature.tracking.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.insights.ActivityResult
import com.miletracker.feature.tracking.insights.DistanceQualityResult
import com.miletracker.feature.tracking.insights.QualityResult
import com.miletracker.feature.tracking.insights.RouteAnalysisResult
import com.miletracker.feature.tracking.insights.RouteAnalyzer
import com.miletracker.feature.tracking.insights.SystemImpactResult
import com.miletracker.feature.tracking.repository.HardwareEventRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * UI state for the Track Insights screen.
 *
 * [qualityResult], [activityResult], [systemImpactResult], [distanceQualityResult] are all
 * populated when the richer [RouteAnalyzer] has run successfully.
 * The legacy summary fields (avgSpeedKmh, distanceKm …) are kept for backward-compat
 * with the existing LazyColumn layout.
 */
data class TrackInsightData(
    // --- legacy / summary fields ------------------------------------------
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val distanceKm: Double,
    val durationMs: Long,
    val locationCount: Int,
    val mockLocationCount: Int,
    val abnormalLocationCount: Int,
    val pauseCount: Int,
    // --- quality score (from RouteAnalyzer) --------------------------------
    val qualityScore: Int,
    val qualityLabel: String,
    // --- rich analyzer results --------------------------------------------
    val qualityResult: QualityResult? = null,
    val activityResult: ActivityResult? = null,
    val systemImpactResult: SystemImpactResult? = null,
    val distanceQualityResult: DistanceQualityResult? = null,
    // --- recommendations (unchanged) --------------------------------------
    val recommendations: List<String> = emptyList(),
)

data class TrackInsightsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val track: SavedTrack? = null,
    val insights: TrackInsightData? = null,
)

sealed interface TrackInsightsAction {
    data class Load(val routeId: String) : TrackInsightsAction
}

sealed interface TrackInsightsEffect

class TrackInsightsViewModel(
    private val savedTrackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    private val hardwareEventRepository: HardwareEventRepository,
    private val routeAnalyzer: RouteAnalyzer = RouteAnalyzer(),
) : BaseViewModel<TrackInsightsUiState, TrackInsightsEffect, TrackInsightsAction>(TrackInsightsUiState()) {
    companion object {
        private const val TAG = "TrackInsightsVM"
    }

    override fun onAction(action: TrackInsightsAction) {
        when (action) {
            is TrackInsightsAction.Load -> loadInsights(action.routeId)
        }
    }

    private fun loadInsights(routeId: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            try {
                val track =
                    savedTrackRepository.getByRouteId(routeId) ?: run {
                        setState { copy(error = "Track not found", isLoading = false) }
                        return@launch
                    }
                setState { copy(track = track) }

                val locations = locationRepository.getForToken(routeId)
                val events = hardwareEventRepository.getEventsForRoute(routeId).getOrDefault(emptyList())

                val analysis = routeAnalyzer.analyze(track, locations, events)
                setState { copy(insights = buildInsightData(track, locations, analysis), isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading insights", e)
                setState { copy(error = e.message ?: "Unknown error", isLoading = false) }
            }
        }
    }

    private fun buildInsightData(
        track: SavedTrack,
        locations: List<LocationData>,
        analysis: RouteAnalysisResult,
    ): TrackInsightData {
        val mockCount = locations.count { it.isMock }
        val abnormalCount = locations.count { it.isAbnormal }
        val pauseCount = locations.count { it.isPaused }

        val qualityScore = analysis.quality.qualityScore
        val qualityLabel =
            when {
                qualityScore >= 90 -> "Excellent"
                qualityScore >= 75 -> "Good"
                qualityScore >= 55 -> "Fair"
                qualityScore >= 35 -> "Poor"
                else -> "Critical"
            }

        return TrackInsightData(
            avgSpeedKmh = if (track.avgSpeed > 0) track.avgSpeed * 3.6 else 0.0,
            maxSpeedKmh = if (track.maxSpeed > 0) track.maxSpeed * 3.6 else 0.0,
            distanceKm = track.distance / 1000.0,
            durationMs = max(0L, track.endTime - track.startTime),
            locationCount = locations.size,
            mockLocationCount = mockCount,
            abnormalLocationCount = abnormalCount,
            pauseCount = pauseCount,
            qualityScore = qualityScore,
            qualityLabel = qualityLabel,
            qualityResult = analysis.quality,
            activityResult = analysis.activity,
            systemImpactResult = analysis.systemImpact,
            distanceQualityResult = analysis.distanceQuality,
            recommendations = buildRecommendations(track, mockCount, abnormalCount),
        )
    }

    private fun buildRecommendations(
        track: SavedTrack,
        mockCount: Int,
        abnormalCount: Int,
    ): List<String> {
        val recs = mutableListOf<String>()
        if (mockCount > 0) recs += "Disable mock location apps for accurate distance tracking."
        if (abnormalCount > 5) recs += "Keep the device in an open area for better GPS signal."
        if (track.wasBatteryOptimizationEnabled) recs += "Disable battery optimisation for Mileway to avoid interruptions."
        if (track.wasPowerSaverEnabled) recs += "Turn off power saver mode while tracking for best accuracy."
        if (track.wasAppKilled) recs += "Avoid closing the app while tracking — use the pause button instead."
        if (recs.isEmpty()) recs += "Great tracking session! GPS accuracy was consistent throughout."
        return recs
    }
}
