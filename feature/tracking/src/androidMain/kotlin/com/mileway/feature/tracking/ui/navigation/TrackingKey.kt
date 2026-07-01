@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.tracking.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface TrackingKey

@Serializable
data object SavedTracks : TrackingKey

@Serializable
data class LiveTrack(val routeId: String) : TrackingKey

@Serializable
data class LiveMap(val routeId: String) : TrackingKey

@Serializable
data class TrackDetail(val routeId: String) : TrackingKey

@Serializable
data class TrackInsights(val routeId: String) : TrackingKey

@Serializable
data class HwEvents(val routeId: String) : TrackingKey

@Serializable
data object CheckInHistory : TrackingKey

@Serializable
data class RouteMap(val routeId: String) : TrackingKey

@Serializable
data class TrackSubmit(
    val routeId: String,
    val distanceKm: Double,
    val vehicleKey: String,
    val startTime: Long,
    val endTime: Long,
) : TrackingKey

@Serializable
data class OdometerCamera(
    val purpose: String,
    val distanceKm: Double = 0.0,
    val startReading: Int = 45_000,
) : TrackingKey

@Serializable
data class TrackingSuccess(
    val distanceKm: Double,
    val reimbursableAmount: Double,
    val vehicleName: String,
    val startTime: Long,
    val endTime: Long,
    val transactionId: String = "",
    val submissionStatus: String = "SUCCESS",
    val violationCount: Int = 0,
    val violationMessage: String = "",
    val voucherNumber: String = "",
    val voucherAmount: Double = 0.0,
) : TrackingKey

@Serializable
data object CreateVoucher : TrackingKey

@Serializable
data object TrackSettings : TrackingKey

@Serializable
data object TrackCustomization : TrackingKey

@Serializable
data object SetupGuide : TrackingKey

@Serializable
data object GeoCheckIn : TrackingKey

@Serializable
data object ManualCheckIn : TrackingKey

@Serializable
data class TrackDataPreview(val routeId: String) : TrackingKey
