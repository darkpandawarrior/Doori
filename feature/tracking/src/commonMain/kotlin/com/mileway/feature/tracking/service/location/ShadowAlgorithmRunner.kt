package com.mileway.feature.tracking.service.location

import com.siddharth.kmp.location.algo.AlgorithmId
import com.siddharth.kmp.location.algo.AlgorithmState
import com.siddharth.kmp.location.algo.DeviceEnvelope
import com.siddharth.kmp.location.algo.Fix
import com.siddharth.kmp.location.algo.MileageAlgorithm
import com.siddharth.kmp.location.algo.SegmentedTripAlgorithm
import com.siddharth.kmp.location.algo.SessionContext
import com.siddharth.kmp.location.algo.TieredGpsAlgorithm

/**
 * Runs one or more toolkit algorithms alongside the live [LocationProcessor] and records what they
 * would have produced, without changing a single number the user sees.
 *
 * The reason this exists rather than a straight swap: replacing a distance algorithm in a
 * reimbursement app is not a refactor, it is a change to what people get paid. The only responsible
 * way to make it is to run both over the same real drives, compare, and switch on evidence. Bench
 * traces cannot substitute — the failure modes that matter (urban canyon, parking drift, tunnels,
 * a phone that slept) only appear in the field.
 *
 * Off by default. Shipping this changes nothing until [enabled] is deliberately set.
 */
class ShadowAlgorithmRunner(
    private val enabled: Boolean = false,
    envelope: DeviceEnvelope = DeviceEnvelope.Default,
    private val candidates: Map<AlgorithmId, MileageAlgorithm> =
        mapOf(
            AlgorithmId.TieredGps to TieredGpsAlgorithm(envelope = envelope),
            SegmentedTripAlgorithm.Id to SegmentedTripAlgorithm(envelope = envelope),
        ),
) {
    private var started = false

    fun reset(startedAtMs: Long) {
        if (!enabled) return
        val session = SessionContext(startedAtMs = startedAtMs)
        candidates.values.forEach { it.reset(session) }
        started = true
    }

    /**
     * Feed a fix that the live pipeline has already accepted for processing.
     *
     * Must never throw into the caller: this is an observer bolted onto the path that records a
     * user's drive, and a diagnostic that can break tracking is worse than no diagnostic.
     */
    fun onFix(fix: GpsFix) {
        if (!enabled || !started) return
        val mapped = fix.toAlgoFix()
        candidates.values.forEach { algo ->
            runCatching { algo.process(mapped) }
        }
    }

    /**
     * End the journey and read the results.
     *
     * flush() is not optional: [SegmentedTripAlgorithm] holds the attribution of a candidate stop
     * until it is confirmed or refuted, so skipping it silently under-counts the tail of every trip
     * — and only for the windowed algorithm, which would bias the very comparison this class exists
     * to make.
     */
    fun finish(): List<ShadowResult> {
        if (!enabled || !started) return emptyList()
        started = false
        return candidates.map { (id, algo) ->
            runCatching { algo.flush() }
            ShadowResult(id, algo.snapshot())
        }
    }
}

/** What one shadow algorithm produced over a journey. */
data class ShadowResult(
    val algorithmId: AlgorithmId,
    val state: AlgorithmState,
) {
    val cleanedKm: Double get() = state.cleanedM / 1000.0

    /**
     * Difference against the figure the live pipeline actually reported, in kilometres.
     *
     * Positive means the shadow algorithm counted more. Reported rather than judged: which one is
     * *right* is a question for the reference-trip corpus, not for this class.
     */
    fun deltaKmVersus(liveCleanedKm: Double): Double = cleanedKm - liveCleanedKm
}

/**
 * Map a pipeline fix onto the toolkit's [Fix].
 *
 * `speedMps` is passed through deliberately: it is the GNSS chipset's Doppler-derived speed, which
 * is roughly 10-100x more accurate than differencing consecutive positions, and it is independent
 * of the position error the algorithm is trying to filter. [SegmentedTripAlgorithm] prefers it for
 * exactly that reason, so dropping it here would quietly disable that behaviour.
 *
 * A zero speed is treated as "not reported" rather than "stationary": GpsFix defaults the field to
 * 0f, so a genuine absence and a genuine standstill are indistinguishable at this boundary, and
 * claiming a measurement that was never taken is the worse error.
 */
internal fun GpsFix.toAlgoFix(): Fix =
    Fix(
        lat = lat,
        lng = lng,
        timeMs = timeMs,
        accuracyM = accuracyM.toDouble(),
        speedMps = speedMps.toDouble().takeIf { it > 0.0 },
        bearingDeg = bearingDeg.toDouble().takeIf { it != 0.0 },
        altitudeM = altitudeM.takeIf { it != 0.0 },
        isMock = isMock,
        provider = provider,
    )
