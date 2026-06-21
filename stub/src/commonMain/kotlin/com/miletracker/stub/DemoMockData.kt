package com.miletracker.stub

import com.miletracker.core.data.model.network.ApprovedVehicle
import com.miletracker.core.data.model.network.ExpenseSubmissionResponse
import com.miletracker.core.data.model.network.LogMilesRoutesResponse
import com.miletracker.core.data.model.network.LogMilesServiceDto
import com.miletracker.core.data.model.network.LogMilesServicesResponse
import com.miletracker.core.data.model.network.MileageConfig
import com.miletracker.core.data.model.network.PolicyApprovedVehiclesResponse
import com.miletracker.core.data.model.network.TrackMileageStatusResponse
import com.miletracker.core.data.model.network.UserConfigResponseV2
import com.miletracker.core.data.model.network.UserProfile

/** A named geographic point used for local geofence check-in validation. */
data class MockCheckInLocation(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val type: String,
    /** Per-location radius override in metres; null falls back to the global default (100 m). */
    val radiusMeters: Double? = null,
)

object DemoMockData {
    fun userConfig(): UserConfigResponseV2 =
        UserConfigResponseV2(
            currency = "INR",
            miles = true,
            logMiles = true,
            mileage = MileageConfig(time = 10, distance = 0.0),
            isOdometerMandatory = false,
            geoCheckIn = false,
            autoDiscardTrackMileage = false,
            calculateDistanceOnBackend = false,
            trackMilesV2 = true,
            draftTrackMiles = true,
            multiServiceLogMiles = false,
            profile =
                UserProfile(
                    code = "EMP001",
                    name = "Demo User",
                    email = "demo@mileway.app",
                    tenant = "DEMO",
                    currency = "INR",
                ),
        )

    fun vehicles(trackMiles: Boolean = true): PolicyApprovedVehiclesResponse =
        PolicyApprovedVehiclesResponse(
            vehicles =
                listOf(
                    ApprovedVehicle("fourWheelerPetrol", "Four Wheeler (Petrol)", 10.0),
                    ApprovedVehicle("fourWheelerDiesel", "Four Wheeler (Diesel)", 10.0),
                    ApprovedVehicle("fourWheelerCng", "Four Wheeler (CNG)", 10.0),
                    ApprovedVehicle("twoWheeler", "Two Wheeler", 16.0),
                    ApprovedVehicle("autoRicshaw", "Auto Rickshaw", 8.0),
                    ApprovedVehicle("electricCar", "Electric Car", 6.0),
                    ApprovedVehicle("electricBikeChargedInsideOffice", "Electric Bike (Office)", 4.0),
                    ApprovedVehicle("electricBikeChargedOutsideOffice", "Electric Bike (Own)", 4.0),
                    ApprovedVehicle("meterTaxi", "Meter Taxi", 0.0),
                    ApprovedVehicle("accompaniedVehicle", "Accompanied Vehicle", 0.0),
                    ApprovedVehicle("ownVehicle", "Own Vehicle", 0.0),
                ),
        )

    fun logMilesServices(isInsideTrip: Boolean = false): LogMilesServicesResponse =
        LogMilesServicesResponse(
            services =
                listOf(
                    LogMilesServiceDto(1L, "Own Car", "CONV-001"),
                    LogMilesServiceDto(2L, "Company Car", "CONV-002"),
                    LogMilesServiceDto(3L, "Taxi / Cab", "CONV-003"),
                    LogMilesServiceDto(4L, "Public Transport", "CONV-004"),
                    LogMilesServiceDto(5L, "Auto Rickshaw", "CONV-005"),
                    LogMilesServiceDto(6L, "Two Wheeler", "CONV-006"),
                ),
        )

    fun submissionResponse(distanceKm: Double = 8.7): ExpenseSubmissionResponse =
        ExpenseSubmissionResponse(
            status = 1,
            reimbursableAmount = (distanceKm * 10.0),
            distance = distanceKm,
            message = "Journey submitted successfully",
            transId = "DEMO-TXN-${1000 + (distanceKm * 100).toInt() % 9000}",
        )

    fun trackMileageStatus(): TrackMileageStatusResponse = TrackMileageStatusResponse(statusCode = 200, description = "Active")

    fun logMilesRoutes(): LogMilesRoutesResponse = LogMilesRoutesResponse()

    /**
     * A small set of realistic-looking mock check-in locations for local geofence validation.
     * Coordinates are placed around a typical urban cluster to allow meaningful distance testing.
     *
     * The first location (HEAD_OFFICE) sits near the city centre; the others are spread across
     * a ~5 km radius so that demo tracking trips naturally pass near at least one of them.
     */
    fun checkInLocations(): List<MockCheckInLocation> =
        listOf(
            MockCheckInLocation(
                id = "CHK-001",
                name = "Head Office",
                lat = 18.5204,
                lng = 73.8567,
                type = "OFFICE",
                radiusMeters = 100.0,
            ),
            MockCheckInLocation(
                id = "CHK-002",
                name = "Warehouse / Supply Center",
                lat = 18.5480,
                lng = 73.8718,
                type = "SUPPLY_CENTER",
                radiusMeters = 150.0,
            ),
            MockCheckInLocation(
                id = "CHK-003",
                name = "North Job Site",
                lat = 18.5601,
                lng = 73.8234,
                type = "JOB_SITE",
                radiusMeters = 200.0,
            ),
            MockCheckInLocation(
                id = "CHK-004",
                name = "East Distribution Hub",
                lat = 18.5120,
                lng = 73.9012,
                type = "DISTRIBUTION_HUB",
                radiusMeters = 120.0,
            ),
            MockCheckInLocation(
                id = "CHK-005",
                name = "South Service Point",
                lat = 18.4890,
                lng = 73.8350,
                type = "SERVICE_POINT",
                radiusMeters = 80.0,
            ),
        )
}
