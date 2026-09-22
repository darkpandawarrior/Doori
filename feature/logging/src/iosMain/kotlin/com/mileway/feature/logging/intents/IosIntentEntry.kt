package com.mileway.feature.logging.intents

import com.mileway.core.common.AppLog
import com.mileway.core.data.di.coreDataModule
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.di.iosAppModule
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.watch.WatchFacade
import org.koin.mp.KoinPlatform
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * P7.1: the headless Kotlin entry point iOS App Intents call into. Intents can run standalone
 * (Siri/Shortcuts invocation before the app's own UI/Koin graph has ever started — e.g. cold
 * `MilwayViewController()` was never called), so this lazily starts the same Koin graph
 * [com.mileway.feature.tracking.IosTrackingEntry.MilwayViewController] uses (idempotent —
 * [ensureKoinStarted] is a no-op once running), then delegates to [WatchFacade]/[ExpenseRepository] —
 * the same shared, already-tested seam Android's `MileageAppFunctions` (P7.5) binds to. Kept in
 * `feature:logging` (not `feature:tracking`) because it needs [ExpenseRepository], and
 * `feature:logging` already depends on `feature:tracking` (never the reverse).
 */
object IosIntentEntry {
    private fun ensureKoinStarted() {
        if (runCatching { KoinPlatform.getKoin() }.isSuccess) return
        AppLog.init()
        initKoin(modules = listOf(coreDataModule, coreUiModule, iosAppModule, trackingModule, loggingModule))
    }

    /** Starts a new tracking session, or a no-op if one is already active. */
    suspend fun startTracking() {
        ensureKoinStarted()
        KoinPlatform.getKoin().get<WatchFacade>().startTracking()
    }

    /** Stops the currently active tracking session, or a no-op if none is active. */
    suspend fun stopTracking() {
        ensureKoinStarted()
        KoinPlatform.getKoin().get<WatchFacade>().stopTracking()
    }

    /**
     * Records a new expense. Mirrors Android AppFunctions' `MileageAppFunctions.logExpense`
     * (P7.5): [category] is matched case-insensitively against [ExpenseCategory.label]/name,
     * falling back to [ExpenseCategory.OTHER]. Returns the new record's id.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun logExpense(
        category: String,
        amountRupees: Double,
        merchantName: String,
        note: String,
    ): String {
        ensureKoinStarted()
        val resolvedCategory =
            ExpenseCategory.entries.find {
                it.label.equals(category, ignoreCase = true) || it.name.equals(category, ignoreCase = true)
            } ?: ExpenseCategory.OTHER
        val record =
            ExpenseRecord(
                id = "EXP-INTENT-${Uuid.random()}",
                category = resolvedCategory,
                merchantName = merchantName.ifBlank { "Unspecified merchant" },
                amountRupees = amountRupees,
                status = ExpenseStatus.PENDING,
                dateMs =
                    kotlin.time.Clock.System
                        .now()
                        .toEpochMilliseconds(),
                note = note,
            )
        KoinPlatform.getKoin().get<ExpenseRepository>().insert(record)
        return record.id
    }
}
