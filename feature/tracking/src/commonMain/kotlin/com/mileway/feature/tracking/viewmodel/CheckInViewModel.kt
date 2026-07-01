package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.EventAudience
import io.github.aakira.napier.Napier
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.tracking.checkin.CheckInValidator
import com.mileway.feature.tracking.checkin.CheckInValidator.CheckInLocation
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CheckInUiState(
    val isSubmitting: Boolean = false,
    val checkInSuccess: Boolean = false,
    val successMessage: String = "",
    val error: String? = null,
    val showRadiusWarning: Boolean = false,
    val radiusWarningMessage: String = "",
    val pendingValidationResult: CheckInValidator.ValidationResult? = null,
    val showManualCheckInSheet: Boolean = false,
    val manualReason: String = "",
    val showGeoCheckInSheet: Boolean = false,
    val currentToken: String = "",
)

sealed interface CheckInAction {
    data object OpenManualCheckIn : CheckInAction

    data object DismissManualCheckIn : CheckInAction

    data class UpdateManualReason(val text: String) : CheckInAction

    data object SubmitManualCheckIn : CheckInAction

    data object OpenGeoCheckIn : CheckInAction

    data object DismissGeoCheckIn : CheckInAction

    data class ValidateAndGeoCheckIn(val lat: Double, val lng: Double) : CheckInAction

    data object DismissRadiusWarning : CheckInAction

    data object ForceGeoCheckInDespiteRadius : CheckInAction

    data object AcknowledgeSuccess : CheckInAction

    data object ClearError : CheckInAction
}

sealed interface CheckInEffect

class CheckInViewModel(
    private val locationRepo: LocationRepository,
    private val hardwareEventRepo: HardwareEventRepository,
    private val currentTrackRepository: CurrentTrackRepository,
    private val geoCheckInLocations: List<CheckInLocation>,
    private val defaultRadiusMeters: Double = 100.0,
) : BaseViewModel<CheckInUiState, CheckInEffect, CheckInAction>(CheckInUiState()) {
    companion object {
        private const val TAG = "CheckInViewModel"
    }

    init {
        viewModelScope.launch {
            currentTrackRepository.currentTrackFlow.collect { session ->
                setState { copy(currentToken = session.token) }
            }
        }
    }

    override fun onAction(action: CheckInAction) {
        when (action) {
            CheckInAction.OpenManualCheckIn ->
                setState { copy(showManualCheckInSheet = true, error = null) }
            CheckInAction.DismissManualCheckIn ->
                setState { copy(showManualCheckInSheet = false, manualReason = "") }
            is CheckInAction.UpdateManualReason ->
                setState { copy(manualReason = action.text) }
            CheckInAction.SubmitManualCheckIn -> submitManualCheckIn()
            CheckInAction.OpenGeoCheckIn ->
                setState { copy(showGeoCheckInSheet = true, error = null) }
            CheckInAction.DismissGeoCheckIn ->
                setState { copy(showGeoCheckInSheet = false, showRadiusWarning = false, pendingValidationResult = null) }
            is CheckInAction.ValidateAndGeoCheckIn -> validateAndGeoCheckIn(action.lat, action.lng)
            CheckInAction.DismissRadiusWarning ->
                setState { copy(showRadiusWarning = false, pendingValidationResult = null) }
            CheckInAction.ForceGeoCheckInDespiteRadius -> forceGeoCheckIn()
            CheckInAction.AcknowledgeSuccess ->
                setState { copy(checkInSuccess = false, successMessage = "") }
            CheckInAction.ClearError ->
                setState { copy(error = null) }
        }
    }

    private fun submitManualCheckIn() {
        val snapshot = currentState
        if (snapshot.isSubmitting) return

        viewModelScope.launch {
            setState { copy(isSubmitting = true, error = null) }
            try {
                val token =
                    snapshot.currentToken.ifBlank {
                        currentTrackRepository.currentTrackFlow.first().token
                    }
                if (token.isBlank()) {
                    setState { copy(isSubmitting = false, error = "No active tracking session found.") }
                    return@launch
                }

                val lastLocation: LocationData? =
                    try {
                        locationRepo.locationsForToken(token).first().lastOrNull()
                    } catch (e: Exception) {
                        null
                    }

                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                val checkInRecord =
                    if (lastLocation != null) {
                        lastLocation.copy(
                            id = 0,
                            date = now,
                            uploaded = false,
                            wasCheckInPoint = true,
                            checkInType = "MANUAL",
                            reason = snapshot.manualReason.take(200),
                        )
                    } else {
                        LocationData(
                            token = token,
                            lat = 0.0, lng = 0.0,
                            activity = "MANUAL_CHECK_IN",
                            speed = 0f,
                            batteryPercentage = 0.0,
                            date = now,
                            uploaded = false,
                            wasCheckInPoint = true,
                            checkInType = "MANUAL",
                            reason = snapshot.manualReason.take(200),
                        )
                    }

                locationRepo.insert(checkInRecord)

                val hwEvent =
                    HardwareEvent(
                        token = token,
                        eventType = EventType.CHECK_IN,
                        event = "Manual check-in recorded",
                        lat = checkInRecord.lat.takeIf { it != 0.0 },
                        lng = checkInRecord.lng.takeIf { it != 0.0 },
                        time = now,
                        audience = EventAudience.USER,
                        metadata = if (snapshot.manualReason.isNotBlank()) "Reason: ${snapshot.manualReason}" else null,
                    )
                hardwareEventRepo.insert(hwEvent)

                Napier.i("Manual check-in saved for token=${token.take(8)}…", tag = "CheckInViewModel")
                setState {
                    copy(
                        isSubmitting = false,
                        showManualCheckInSheet = false,
                        manualReason = "",
                        checkInSuccess = true,
                        successMessage = "Manual check-in recorded.",
                    )
                }
            } catch (e: Exception) {
                Napier.e("Manual check-in failed: ${e.message}", e, tag = "CheckInViewModel")
                setState { copy(isSubmitting = false, error = e.message ?: "Check-in failed.") }
            }
        }
    }

    private fun validateAndGeoCheckIn(
        userLat: Double,
        userLng: Double,
    ) {
        if (geoCheckInLocations.isEmpty()) {
            setState { copy(error = "No check-in locations configured.", showGeoCheckInSheet = false) }
            return
        }

        val result =
            CheckInValidator.validate(
                userLat = userLat,
                userLng = userLng,
                candidates = geoCheckInLocations,
                defaultRadiusMeters = defaultRadiusMeters,
            )

        if (result.withinRadius) {
            persistGeoCheckIn(result)
        } else {
            val message = CheckInValidator.buildOutsideRadiusMessage(result)
            Napier.d("Geo check-in outside radius: ${result.distanceOutside.toInt()} m outside", tag = "CheckInViewModel")
            setState {
                copy(
                    showGeoCheckInSheet = false,
                    showRadiusWarning = true,
                    radiusWarningMessage = message,
                    pendingValidationResult = result,
                )
            }
        }
    }

    private fun forceGeoCheckIn() {
        val pending = currentState.pendingValidationResult ?: return
        setState { copy(showRadiusWarning = false, pendingValidationResult = null) }
        persistGeoCheckIn(pending, isOverride = true)
    }

    private fun persistGeoCheckIn(
        result: CheckInValidator.ValidationResult,
        isOverride: Boolean = false,
    ) {
        val snapshot = currentState
        if (snapshot.isSubmitting) return

        viewModelScope.launch {
            setState { copy(isSubmitting = true, error = null) }
            try {
                val token =
                    snapshot.currentToken.ifBlank {
                        currentTrackRepository.currentTrackFlow.first().token
                    }
                if (token.isBlank()) {
                    setState { copy(isSubmitting = false, error = "No active tracking session found.") }
                    return@launch
                }

                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                val checkInType = if (isOverride) "GEO_OVERRIDE" else "GEO"
                val checkInRecord =
                    LocationData(
                        token = token,
                        lat = result.userLat,
                        lng = result.userLng,
                        activity = "GEO_CHECK_IN",
                        speed = 0f,
                        batteryPercentage = 0.0,
                        date = now,
                        uploaded = false,
                        wasCheckInPoint = true,
                        checkInType = checkInType,
                        miscellaneous = result.nearestLocation.name,
                        reason = if (isOverride) "Override: ${result.distanceOutside.toInt()} m outside radius" else "",
                    )

                locationRepo.insert(checkInRecord)

                val hwEvent =
                    HardwareEvent(
                        token = token,
                        eventType = EventType.CHECK_IN,
                        event = if (isOverride) "Geo check-in recorded (override)" else "Geo check-in recorded",
                        lat = result.userLat,
                        lng = result.userLng,
                        time = now,
                        audience = EventAudience.USER,
                        metadata =
                            buildString {
                                append("Location: ${result.nearestLocation.name}")
                                append(", Distance: ${result.distanceMeters.toInt()} m")
                                append(", Radius: ${result.effectiveRadius.toInt()} m")
                                if (isOverride) append(", Override: ${result.distanceOutside.toInt()} m outside")
                            },
                    )
                hardwareEventRepo.insert(hwEvent)

                Napier.i("Geo check-in ($checkInType) saved for token=${token.take(8)}… at ${result.nearestLocation.name}", tag = "CheckInViewModel")
                setState {
                    copy(
                        isSubmitting = false,
                        showGeoCheckInSheet = false,
                        checkInSuccess = true,
                        successMessage = "Checked in at ${result.nearestLocation.name}.",
                    )
                }
            } catch (e: Exception) {
                Napier.e("Geo check-in failed: ${e.message}", e, tag = "CheckInViewModel")
                setState { copy(isSubmitting = false, error = e.message ?: "Check-in failed.") }
            }
        }
    }
}
