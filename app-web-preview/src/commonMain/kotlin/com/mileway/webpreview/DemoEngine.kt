package com.mileway.webpreview

import androidx.compose.runtime.Immutable
import com.siddharth.kmp.location.KalmanSmoother
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Web-preview port of `feature:tracking`'s `SimulatedLocationSource` (androidMain): same ~22 m
 * steps with gentle heading drift and positional jitter from the same Pune origin — but fully
 * deterministic (seeded [Random], synthetic clock) and pure commonMain so it compiles to wasm.
 */
@Immutable
data class DemoFix(
    val lat: Double,
    val lng: Double,
    val timeMs: Long,
    val speedMps: Double,
    val accuracyM: Double,
)

@Immutable
data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val distanceKm: Double = 0.0,
    val speedKmh: Double = 0.0,
    val accuracyM: Double = 0.0,
    val elapsedSec: Long = 0,
    val path: List<Pair<Double, Double>> = emptyList(),
)

/**
 * Consumes the simulated drive through the production Kalman smoother (`com.siddharth.kmp:location`,
 * the same class the Android tracking service runs) and accumulates haversine distance.
 */
class DemoTrackingEngine(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val smoother = KalmanSmoother()
    private var job: Job? = null
    private var rnd = Random(SEED)
    private var lat = START_LAT
    private var lng = START_LNG
    private var bearing = 45.0
    private var timeMs = 0L

    fun start() {
        if (_state.value.isTracking && !_state.value.isPaused) return
        _state.update { it.copy(isTracking = true, isPaused = false) }
        job =
            scope.launch {
                while (true) {
                    step()
                    delay(TICK_MS)
                }
            }
    }

    fun pause() {
        job?.cancel()
        job = null
        _state.update { it.copy(isPaused = true, speedKmh = 0.0) }
    }

    fun stop() {
        job?.cancel()
        job = null
        smoother.reset()
        rnd = Random(SEED)
        lat = START_LAT
        lng = START_LNG
        bearing = 45.0
        timeMs = 0L
        _state.value = TrackingState()
    }

    private fun step() {
        val speedMps = 8.0 + rnd.nextDouble() * 6.0 // 8–14 m/s (~29–50 km/h)
        val accuracy = 4.0 + rnd.nextDouble() * 4.0
        val fix =
            DemoFix(
                lat = lat + (rnd.nextDouble() - 0.5) * 0.00002,
                lng = lng + (rnd.nextDouble() - 0.5) * 0.00002,
                timeMs = timeMs,
                speedMps = speedMps,
                accuracyM = accuracy,
            )
        val (sLat, sLng) = smoother.smooth(fix.lat, fix.lng, fix.accuracyM.toFloat(), fix.timeMs)
        _state.update { s ->
            val last = s.path.lastOrNull()
            val deltaM = last?.let { haversineMeters(it.first, it.second, sLat, sLng) } ?: 0.0
            s.copy(
                distanceKm = s.distanceKm + deltaM / 1000.0,
                speedKmh = fix.speedMps * 3.6,
                accuracyM = fix.accuracyM,
                elapsedSec = s.elapsedSec + TICK_MS / 1000,
                path = s.path + (sLat to sLng),
            )
        }
        // Advance along the current bearing by speed * dt (same math as the Android simulator,
        // scaled by SIM_SPEEDUP so a short demo session covers a believable trip).
        val distanceM = speedMps * (TICK_MS / 1000.0) * SIM_SPEEDUP
        val bearingRad = bearing * PI / 180.0
        lat += (distanceM * cos(bearingRad)) / 111_320.0
        lng += (distanceM * sin(bearingRad)) / (111_320.0 * cos(lat * PI / 180.0))
        bearing += (rnd.nextDouble() - 0.5) * 20.0
        timeMs += TICK_MS
    }

    companion object {
        const val SEED = 42
        const val START_LAT = 18.5204 // Pune city centre, same origin as the Android simulator
        const val START_LNG = 73.8567
        const val TICK_MS = 1_000L
        const val SIM_SPEEDUP = 4.0
    }
}

/** Same great-circle formula as core:data's `haversineMeters` (not importable here — Room chain). */
fun haversineMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    val r = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}

// ---------------------------------------------------------------------------------------------
// Expense log fake — mirrors feature:logging's ExpenseRecord/ExpenseRepository shape (status
// lifecycle Draft → Submitted → Approved) with a deterministic in-memory seed.
// ---------------------------------------------------------------------------------------------

enum class ExpenseStatus { DRAFT, SUBMITTED, APPROVED }

@Immutable
data class DemoExpense(
    val id: Int,
    val title: String,
    val category: String,
    val amountInr: Double,
    val date: String,
    val status: ExpenseStatus,
)

class DemoExpenseStore {
    private val _expenses =
        MutableStateFlow(
            listOf(
                DemoExpense(1, "Client site drive", "Mileage", 486.50, "18 Jul", ExpenseStatus.APPROVED),
                DemoExpense(2, "Mumbai–Pune toll", "Toll", 320.00, "18 Jul", ExpenseStatus.APPROVED),
                DemoExpense(3, "Fuel top-up", "Fuel", 2100.00, "17 Jul", ExpenseStatus.SUBMITTED),
                DemoExpense(4, "Airport parking", "Parking", 240.00, "16 Jul", ExpenseStatus.SUBMITTED),
                DemoExpense(5, "Team lunch, Baner", "Food", 1350.00, "15 Jul", ExpenseStatus.APPROVED),
                DemoExpense(6, "Hotel, Bengaluru", "Lodging", 5400.00, "14 Jul", ExpenseStatus.APPROVED),
                DemoExpense(7, "Metro recharge", "Transit", 500.00, "13 Jul", ExpenseStatus.DRAFT),
            ),
        )
    val expenses: StateFlow<List<DemoExpense>> = _expenses.asStateFlow()

    private var nextId = 8

    fun add(
        title: String,
        category: String,
        amountInr: Double,
    ) {
        _expenses.update {
            listOf(DemoExpense(nextId++, title, category, amountInr, "Today", ExpenseStatus.DRAFT)) + it
        }
    }

    fun submitDrafts() {
        _expenses.update { list ->
            list.map { if (it.status == ExpenseStatus.DRAFT) it.copy(status = ExpenseStatus.SUBMITTED) else it }
        }
    }
}

/** ₹ with Indian digit grouping (1,23,456) — mirrors the app's INR formatting. */
fun formatInr(amount: Double): String {
    val paise = (amount * 100 + 0.5).toLong()
    val rupees = paise / 100
    val fraction = (paise % 100).toString().padStart(2, '0')
    val digits = rupees.toString()
    val grouped =
        if (digits.length <= 3) {
            digits
        } else {
            val head = digits.dropLast(3)
            val tail = digits.takeLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
        }
    return "₹$grouped.$fraction"
}

fun formatKm(km: Double): String {
    val tenths = (km * 10 + 0.5).toLong()
    return "${tenths / 10}.${tenths % 10} km"
}
