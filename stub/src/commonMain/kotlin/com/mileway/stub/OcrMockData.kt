package com.mileway.stub

import kotlin.math.abs

/** Key-value fields a receipt OCR pass would extract from an uploaded image. */
data class ReceiptExtraction(
    val amount: Double,
    /** ISO-8601 date (yyyy-MM-dd), derived from the seed, never from the clock. */
    val date: String,
    val merchant: String,
)

/** Per-file outcome of a batch receipt upload. Mirrors the four real-world outcomes. */
enum class OcrBatchStatus {
    SUCCESS,
    DUPLICATE,
    NOT_RECEIPT,
    FAILED,
}

/**
 * Deterministic OCR mocks. All variation is keyed off a caller-provided seed string
 * (typically the uploaded file name) via a stable 31-polynomial hash, so the same
 * input always produces the same extraction/status on every platform and every run.
 */
object OcrMockData {
    /**
     * Odometer readings that differ from the device value by more than this many
     * units are treated as a discrepancy the user must confirm.
     */
    const val ODOMETER_DISCREPANCY_THRESHOLD = 20

    /** Injected server-side offset for the discrepancy path (> threshold by design). */
    const val ODOMETER_DISCREPANCY_OFFSET = 37

    private val merchants =
        listOf(
            "City Fuel Station",
            "Highway Diner",
            "Metro Parking Co.",
            "Grand Hotel & Suites",
            "QuickMart Convenience",
            "Airport Taxi Services",
            "Office Supplies Depot",
            "Riverside Cafe",
        )

    /**
     * Deterministic receipt key-value extraction for the given [fileName] (or any
     * seed string). Amount lands in 100.00–999.90, date in a fixed demo year so
     * the result never drifts with the wall clock.
     */
    fun receiptExtractionFor(fileName: String): ReceiptExtraction {
        val hash = stableHash(fileName)
        val amount = 100.0 + (hash % 9000) / 10.0
        val month = (hash % 12) + 1
        val day = (hash % 28) + 1
        val date = "2026-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        return ReceiptExtraction(
            amount = amount,
            date = date,
            merchant = merchants[hash % merchants.size],
        )
    }

    /**
     * Deterministic batch status for the given [seed], weighted towards SUCCESS:
     * hash bucket 0–6 → SUCCESS, 7 → DUPLICATE, 8 → NOT_RECEIPT, 9 → FAILED.
     */
    fun batchStatusFor(seed: String): OcrBatchStatus =
        when (stableHash(seed) % 10) {
            7 -> OcrBatchStatus.DUPLICATE
            8 -> OcrBatchStatus.NOT_RECEIPT
            9 -> OcrBatchStatus.FAILED
            else -> OcrBatchStatus.SUCCESS
        }

    /**
     * Mock "server" odometer reading for the triple-reading verification flow
     * (device camera / user entry / server record). Returns [deviceReading]
     * unchanged normally, but injects a deterministic +[ODOMETER_DISCREPANCY_OFFSET]
     * discrepancy whenever `deviceReading % 7 == 0` so the discrepancy UI path is
     * reachable on demand (e.g. enter 14000 to trigger it, 14001 to avoid it).
     */
    fun serverReadingFor(deviceReading: Int): Int =
        if (deviceReading % 7 == 0) {
            deviceReading + ODOMETER_DISCREPANCY_OFFSET
        } else {
            deviceReading
        }

    /** True when the device/server readings differ by more than the threshold. */
    fun isDiscrepancy(
        deviceReading: Int,
        serverReading: Int,
    ): Boolean = abs(deviceReading - serverReading) > ODOMETER_DISCREPANCY_THRESHOLD

    /**
     * Stable non-negative 31-polynomial string hash. Implemented locally (rather
     * than relying on `hashCode()`) so mock rotation is identical on every
     * Kotlin target and never changes underneath the tests.
     */
    private fun stableHash(seed: String): Int {
        var h = 0
        for (c in seed) h = 31 * h + c.code
        return h and 0x7FFFFFFF
    }
}
