package com.mileway.feature.tracking.service.location

import com.siddharth.kmp.appshell.GeoPoint

/**
 * Maps a platform [GeoPoint] into the tracking pipeline's [GpsFix].
 *
 * **Why this lives in commonMain rather than next to its iOS caller.** CoreLocation reports a
 * *negative* `speed` and `course` to mean "not available" — it does not use null, and `GeoPoint`'s
 * own defaults (`-1f` / `-1.0`) exist for exactly that reason. Letting those through would push a
 * negative speed into the jitter gate and `DynamicIntervalCalculator`, and a negative bearing into
 * the quality signal. That guard is real logic and deserves a test.
 *
 * It previously sat as a private function in `IosTrackingController` (iosMain), where nothing could
 * test it — the repo has no `iosTest` source set anywhere, and adding one to this module is not
 * cheap: Kotlin/Native rejects backtick test names containing `,`/`(`/`)`, which this module's
 * existing commonTest uses in 39 places across 22 files, so enabling `iosTest` here would mean
 * renaming tests unrelated to the change. Both [GeoPoint] and [GpsFix] are commonMain types, so
 * moving the mapping down is the smaller and more honest fix — and it matches the convention already
 * documented in `InjectableDocumentAiAnalyzer` and `TextGenerator` ("kept in commonMain so it's
 * unit-testable").
 *
 * Altitude and accuracy are passed through untouched: a negative altitude is meaningful (below sea
 * level), unlike the speed/course sentinels.
 */
internal fun GeoPoint.toGpsFix(): GpsFix =
    GpsFix(
        lat = latitude,
        lng = longitude,
        timeMs = timestampMillis,
        speedMps = if (speedMetersPerSecond < 0f) 0f else speedMetersPerSecond,
        accuracyM = accuracyMeters,
        bearingDeg = if (courseDegrees < 0.0) 0f else courseDegrees.toFloat(),
        altitudeM = altitudeMeters,
        provider = "ios",
    )
