package com.mileway.feature.travel.model

enum class TripStatus { ACTIVE, UPCOMING, COMPLETED }

enum class TransportMode { FLIGHT, TRAIN, BUS, CAB }

data class BookingRecord(
    val id: String,
    val mode: TransportMode,
    val origin: String,
    val destination: String,
    val carrier: String,
    val flightOrTrainNumber: String,
    val departureMs: Long,
    val amountRupees: Double,
    val status: TripStatus,
    val gate: String? = null,
    val boardingTime: String? = null,
)
