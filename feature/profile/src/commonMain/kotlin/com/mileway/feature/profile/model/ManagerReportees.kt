package com.mileway.feature.profile.model

import com.mileway.feature.profile.viewmodel.Reportee

/**
 * PLAN_V24 P10.6 — single source of the seeded reportee identities, shared by
 * [DelegateSessionViewModel][com.mileway.feature.profile.viewmodel.DelegateSessionViewModel]
 * (act-on-behalf, P7.3) and
 * [ManagerReporteesViewModel][com.mileway.feature.profile.viewmodel.ManagerReporteesViewModel]
 * (manager tracking view). One list so the two surfaces agree on who a manager's reportees are.
 */
object SeededReportees {
    /** Each a distinct identity so delegated trips stay isolated (see P7.3). */
    val all: List<Reportee> =
        listOf(
            Reportee("Priya Sharma", "priya.sharma@mileway.app", "EMP-2101"),
            Reportee("Rahul Mehra", "rahul.mehra@mileway.app", "EMP-2102"),
            Reportee("Asha Verma", "asha.verma@mileway.app", "EMP-2103"),
            Reportee("Vikram Nair", "vikram.nair@mileway.app", "EMP-2104"),
        )
}

/** One reportee's rolled-up tracking summary shown in the manager list. */
data class ReporteeTripSummary(
    val reportee: Reportee,
    val tripCount: Int,
    val totalKm: Double,
    val pendingApprovals: Int,
    val lastTripLabel: String,
)

/** One seeded trip row shown in a reportee's drill-in detail list. */
data class ReporteeTrip(
    val id: String,
    val dateLabel: String,
    val fromTo: String,
    val distanceKm: Double,
    val status: String,
)

/**
 * Deterministic seeded mock for the manager view. There is no real per-delegate trip store yet
 * (delegated trips stamp identity but aren't queried back per-reportee), so summaries and trip
 * lists are derived purely from the reportee code — stable across runs and testable.
 *
 * ponytail: seeded mock; the upgrade path is real aggregation from SavedTracks-per-delegate once
 * delegated trips persist to Room. Behavior (list + drill-in) is real; only the numbers are seeded.
 */
object ManagerReportees {
    private val ROUTES =
        listOf(
            "Home → Office",
            "Office → Client site",
            "Client site → Office",
            "Office → Warehouse",
            "Warehouse → Home",
            "Office → Airport",
            "Airport → Hotel",
        )
    private val STATUSES = listOf("Approved", "Pending", "Submitted")

    /** Per-reportee summaries, in [SeededReportees.all] order. */
    fun summaries(): List<ReporteeTripSummary> =
        SeededReportees.all.map { reportee ->
            val trips = tripsFor(reportee.code)
            ReporteeTripSummary(
                reportee = reportee,
                tripCount = trips.size,
                totalKm = trips.sumOf { it.distanceKm },
                pendingApprovals = trips.count { it.status == "Pending" },
                lastTripLabel = trips.lastOrNull()?.let { "${it.fromTo} · ${it.dateLabel}" }.orEmpty(),
            )
        }

    /** Deterministic trip list for a reportee [code] (empty for an unknown code). */
    fun tripsFor(code: String): List<ReporteeTrip> {
        val seed = code.lastOrNull()?.digitToIntOrNull() ?: return emptyList()
        val count = 3 + seed
        return (0 until count).map { i ->
            ReporteeTrip(
                id = "$code-T${i + 1}",
                dateLabel = "Jul ${i + 1}, 2026",
                fromTo = ROUTES[(seed + i) % ROUTES.size],
                distanceKm = 6.5 + (seed * 2) + (i * 3.25),
                status = STATUSES[(seed + i) % STATUSES.size],
            )
        }
    }
}
