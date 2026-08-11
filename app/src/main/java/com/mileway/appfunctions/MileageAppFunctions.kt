package com.mileway.appfunctions

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.mileway.core.data.watch.SnapshotCache
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.tracking.watch.WatchFacade
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TAG = "MileageAppFunctions"

/**
 * P7.5: the result of [MileageAppFunctions.logExpense] — deliberately its own
 * [AppFunctionSerializable] rather than exposing [ExpenseRecord] directly, since
 * [ExpenseRecord.category] carries a Compose [androidx.compose.ui.graphics.vector.ImageVector]
 * (an unsupported AppFunctions parameter/return type). Every property here uses only
 * AppFunctions-supported primitive/String types.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class LoggedExpense(
    /** The newly created expense record's unique identifier. */
    val id: String,
    /** The expense category it was filed under. */
    val category: String,
    /** The amount in rupees. */
    val amountRupees: Double,
    /** Lifecycle status right after logging: always "PENDING" unless the amount needs approval. */
    val status: String,
    /** True when this amount exceeds the auto-approval threshold and now needs manager approval. */
    val requiresApproval: Boolean,
)

/**
 * P7.5: today's tracking + expense activity, the on-device-agent-facing counterpart of the
 * Glance/WidgetKit cached [com.mileway.core.data.watch.WatchSyncPayload] (mileage) plus today's
 * expense total from [ExpenseRepository] (not part of that payload, which is mileage-only).
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TodaySummary(
    /** Distance tracked today, in kilometers. */
    val distanceKm: Double,
    /** True if a trip is currently being tracked. */
    val isTracking: Boolean,
    /** Total amount of expenses logged today, in rupees. */
    val expensesTodayRupees: Double,
    /** Number of expenses logged today. */
    val expenseCountToday: Int,
)

/**
 * Mileway's [AppFunction]s: a small, LLM-invokable surface over the same offline domain calls the
 * app's own UI makes — [WatchFacade] (already the shared start/stop/summary seam both Wear OS and
 * the Quick Settings tile bind to, see `feature:tracking`'s `WatchFacade.kt`) for tracking, and
 * [ExpenseRepository] for logging an expense. Registered via [com.mileway.MilewayApplication]
 * implementing `AppFunctionConfiguration.Provider` (Koin resolves the dependencies; AppFunctions'
 * KSP-generated code looks up this enclosing-class instance through that factory, not through
 * constructor injection at call time).
 *
 * No destructive/irreversible action is exposed without confirmation (skill constraint):
 * stopping a trip and logging an expense are both easily correctable afterward from the app's own
 * UI, so neither requires an extra confirmation step here.
 */
class MileageAppFunctions(
    private val watchFacade: WatchFacade,
    private val expenseRepository: ExpenseRepository,
    private val snapshotCache: SnapshotCache,
) {
    /**
     * Starts tracking a new mileage trip.
     * Required workflow: call [getTodaySummary] first if the user wants to know whether a trip is
     * already active — calling this while one is active leaves the existing trip running unchanged.
     *
     * @param appFunctionContext The execution context.
     *
 * ponytail: appFunctionContext is unused here but mandated by the AppFunctions contract — it
 * MUST be the first parameter of every @AppFunction method (see the appfunctions skill).
 */
    @Suppress("UnusedParameter")
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startTrackingTrip(appFunctionContext: AppFunctionContext) {
        withContext(Dispatchers.IO) {
            runCatching { watchFacade.startTracking() }
                .onFailure {
                    Napier.e(tag = TAG, message = "startTrackingTrip failed", throwable = it)
                    throw AppFunctionAppUnknownException("Could not start tracking: ${it.message}")
                }
        }
    }

    /**
     * Stops the currently active mileage trip, if any.
     * This is a no-op (does not throw) when no trip is currently active.
     *
     * @param appFunctionContext The execution context.
     */
    @Suppress("UnusedParameter")
    @AppFunction(isDescribedByKDoc = true)
    suspend fun stopTrackingTrip(appFunctionContext: AppFunctionContext) {
        withContext(Dispatchers.IO) {
            runCatching { watchFacade.stopTracking() }
                .onFailure {
                    Napier.e(tag = TAG, message = "stopTrackingTrip failed", throwable = it)
                    throw AppFunctionAppUnknownException("Could not stop tracking: ${it.message}")
                }
        }
    }

    /**
     * Records a new expense with the given category, amount and optional merchant/note.
     * Amounts above ₹5,000 are automatically flagged as needing manager approval; the returned
     * [LoggedExpense.requiresApproval] reflects this — no further confirmation is needed from the
     * user for the recording step itself.
     *
     * @param appFunctionContext The execution context.
     * @param category One of: Food, Travel, Accommodation, Office Supplies, Communication,
     * Medical, Other. An unrecognized value is treated as "Other".
     * @param amountRupees The expense amount in rupees. Must be greater than zero.
     * @param merchantName The merchant or vendor name. If blank, a generic placeholder is used.
     * @param note An optional free-text note about the expense.
     * @return The [LoggedExpense] that was recorded, including its generated id and approval status.
     */
    @Suppress("UnusedParameter")
    @OptIn(ExperimentalUuidApi::class)
    @AppFunction(isDescribedByKDoc = true)
    suspend fun logExpense(
        appFunctionContext: AppFunctionContext,
        category: String,
        amountRupees: Double,
        merchantName: String = "",
        note: String = "",
    ): LoggedExpense {
        if (amountRupees <= 0.0) {
            throw AppFunctionInvalidArgumentException("amountRupees must be greater than zero.")
        }
        return withContext(Dispatchers.IO) {
            val resolvedCategory = category.toExpenseCategory()
            val record =
                ExpenseRecord(
                    id = "EXP-AF-${Uuid.random()}",
                    category = resolvedCategory,
                    merchantName = merchantName.ifBlank { "Unspecified merchant" },
                    amountRupees = amountRupees,
                    status = ExpenseStatus.PENDING,
                    dateMs = System.currentTimeMillis(),
                    note = note,
                )
            expenseRepository.insert(record)
            LoggedExpense(
                id = record.id,
                category = resolvedCategory.label,
                amountRupees = record.amountRupees,
                status = record.status.name,
                requiresApproval = record.requiresApproval,
            )
        }
    }

    /**
     * Returns today's tracking and expense activity: distance tracked today, whether a trip is
     * currently active, and today's logged expense total/count.
     *
     * @param appFunctionContext The execution context.
     * @return The current [TodaySummary].
     */
    @Suppress("UnusedParameter")
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTodaySummary(appFunctionContext: AppFunctionContext): TodaySummary =
        withContext(Dispatchers.IO) {
            val payload = snapshotCache.read()
            val todayStartMs = todayStartEpochMs()
            val expensesToday = expenseRepository.getAll().filter { it.dateMs >= todayStartMs }
            TodaySummary(
                distanceKm = payload?.todayKm ?: 0.0,
                isTracking = payload?.isTracking ?: false,
                expensesTodayRupees = expensesToday.sumOf { it.amountRupees },
                expenseCountToday = expensesToday.size,
            )
        }
}

private fun String.toExpenseCategory(): ExpenseCategory =
    ExpenseCategory.entries.find { it.label.equals(this, ignoreCase = true) || it.name.equals(this, ignoreCase = true) }
        ?: ExpenseCategory.OTHER

private fun todayStartEpochMs(): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
}
