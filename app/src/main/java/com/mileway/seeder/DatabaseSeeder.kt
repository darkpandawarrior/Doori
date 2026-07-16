package com.mileway.seeder

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.repository.VehiclePricingCache
import com.mileway.feature.tracking.repository.VehiclePricingSnapshot
import com.mileway.stub.DemoMockData
import kotlinx.coroutines.flow.first

class DatabaseSeeder(
    private val trackDao: SavedTrackDao,
    private val locationDao: LocationDao,
    private val vehicleCache: VehiclePricingCache,
) {
    suspend fun seedIfEmpty() {
        if (trackDao.count() > 0L) return
        demoTracks().forEach { track ->
            trackDao.insertSavedTrack(track)
            demoLocations(track).forEach { locationDao.insertLocation(it) }
        }
    }

    /**
     * Bug found PLAN_V34 P1 E2E (2026-07-16): [VehiclePricingRepository][com.mileway.feature.tracking.repository.VehiclePricingRepository]
     * emits [Error][com.mileway.core.network.ReadState.Error] when offline with a cold cache (real
     * backend never fetched yet, or a fresh install that starts offline) — TrackMilesViewModel then
     * never selects a vehicle, so JourneyStep can never leave VEHICLE and START silently no-ops.
     * Seed the same demo vehicles :stub serves so the cache is never cold (real==stub parity,
     * PLAN_V33 B6) — a later online refresh overwrites this with live data as normal.
     */
    suspend fun seedVehiclesIfEmpty() {
        if (vehicleCache.snapshot.first() != null) return
        vehicleCache.write(VehiclePricingSnapshot(vehicles = DemoMockData.vehicles().vehicles))
    }

    private fun now() = System.currentTimeMillis()

    private fun demoTracks(): List<SavedTrack> {
        val base = now() - 7 * 86_400_000L
        return listOf(
            SavedTrack(
                routeId = "demo-track-1",
                name = "Kothrud → Hinjewadi",
                startLatitude = 18.5018, startLongitude = 73.8141,
                endLatitude = 18.5975, endLongitude = 73.7313,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = base, endTime = base + 2_400_000L,
                distance = 8700.0, duration = 2_400_000L,
                isCompleted = true, serverUploaded = true,
                selectedVehicleType = "fourWheelerPetrol", vehiclePricing = 10.0,
                service = "Own Car", submissionTime = base + 2_500_000L,
                submittedAmount = 87.0, submittedAmountCurrency = "INR",
                transId = "DEMO-TXN-001", createdAt = base, startedAtTimestamp = base,
                startedByAccountId = "ACC-001"
            ),
            SavedTrack(
                routeId = "demo-track-2",
                name = "FC Road → Viman Nagar",
                startLatitude = 18.5308, startLongitude = 73.8475,
                endLatitude = 18.5667, endLongitude = 73.9039,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = base + 86_400_000L, endTime = base + 86_400_000L + 2_700_000L,
                distance = 12400.0, duration = 2_700_000L,
                isCompleted = true, serverUploaded = true,
                selectedVehicleType = "twoWheeler", vehiclePricing = 16.0,
                service = "Own Car", submissionTime = base + 86_400_000L + 2_800_000L,
                submittedAmount = 198.4, submittedAmountCurrency = "INR",
                transId = "DEMO-TXN-002", createdAt = base + 86_400_000L,
                startedAtTimestamp = base + 86_400_000L,
                startedByAccountId = "ACC-001"
            ),
            SavedTrack(
                routeId = "demo-track-3",
                name = "Wakad → Baner",
                startLatitude = 18.5975, startLongitude = 73.7898,
                endLatitude = 18.5574, endLongitude = 73.7844,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = base + 2 * 86_400_000L, endTime = base + 2 * 86_400_000L + 1_200_000L,
                distance = 5200.0, duration = 1_200_000L,
                isCompleted = true, serverUploaded = false, isDraft = true,
                selectedVehicleType = "fourWheelerCng", vehiclePricing = 10.0,
                service = "Company Car", createdAt = base + 2 * 86_400_000L,
                startedAtTimestamp = base + 2 * 86_400_000L,
                startedByAccountId = "ACC-001"
            ),
            SavedTrack(
                routeId = "demo-track-4",
                name = "Bandra → BKC",
                startLatitude = 19.0596, startLongitude = 72.8295,
                endLatitude = 19.0659, endLongitude = 72.8662,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = base + 3 * 86_400_000L, endTime = base + 3 * 86_400_000L + 1_800_000L,
                distance = 6100.0, duration = 1_800_000L,
                isCompleted = true, serverUploaded = true,
                selectedVehicleType = "meterTaxi", vehiclePricing = 0.0,
                service = "Taxi / Cab", submissionTime = base + 3 * 86_400_000L + 1_900_000L,
                submittedAmount = 0.0, submittedAmountCurrency = "INR",
                transId = "DEMO-TXN-004", createdAt = base + 3 * 86_400_000L,
                startedAtTimestamp = base + 3 * 86_400_000L,
                startedByAccountId = "ACC-002"
            ),
            SavedTrack(
                routeId = "demo-track-5",
                name = "Kalyani Nagar → MIDC",
                startLatitude = 18.5502, startLongitude = 73.8952,
                endLatitude = 18.6229, endLongitude = 73.8955,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = base + 5 * 86_400_000L, endTime = base + 5 * 86_400_000L + 900_000L,
                distance = 3800.0, duration = 900_000L,
                isCompleted = true, serverUploaded = true,
                selectedVehicleType = "twoWheeler", vehiclePricing = 16.0,
                service = "Own Car", submissionTime = base + 5 * 86_400_000L + 1_000_000L,
                submittedAmount = 60.8, submittedAmountCurrency = "INR",
                transId = "DEMO-TXN-005", createdAt = base + 5 * 86_400_000L,
                startedAtTimestamp = base + 5 * 86_400_000L,
                startedByAccountId = "ACC-003"
            )
        )
    }

    private fun demoLocations(track: SavedTrack): List<LocationData> {
        val points = 12
        val latStep = (track.endLatitude - track.startLatitude) / points
        val lngStep = (track.endLongitude - track.startLongitude) / points
        val timeStep = (track.endTime - track.startTime) / points
        return (0..points).map { i ->
            LocationData(
                activity = "DRIVING",
                speed = (25f + (Math.random() * 30).toFloat()),
                lat = track.startLatitude + latStep * i,
                lng = track.startLongitude + lngStep * i,
                token = track.routeId,
                date = track.startTime + timeStep * i,
                uploaded = track.serverUploaded,
                displacement = (track.distance / points).toDouble(),
                accuracy = 5f,
                batteryPercentage = 80.0
            )
        }
    }
}
