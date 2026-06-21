@file:Suppress("ktlint:standard:property-naming")

package com.miletracker.core.ui.previews

/**
 * DI-free sample data for use in @Preview composables.
 * Hardcoded so no Koin/Hilt context is required during preview rendering.
 */
object SampleData {
    object Trip {
        const val routeId = "DEMO-20260619-001"
        const val vehicleLabel = "Car"
        const val distanceKm = 12.4
        const val reimbursableAmount = 148.80
        const val startAddress = "MG Road, Pune"
        const val endAddress = "Hinjewadi Phase 1, Pune"
        const val startTimeMs = 1750348800000L // 2026-06-19 09:00 IST
        const val endTimeMs = 1750352400000L // 2026-06-19 10:00 IST
    }

    object Profile {
        const val name = "Siddharth Pandalai"
        const val email = "sid@example.com"
        const val employeeId = "EMP-1042"
        const val orgName = "Acme Corp"
    }

    object Approval {
        const val approverName = "Priya Sharma"
        const val amount = 650.0
        const val status = "Pending"
        const val reason = "Client visit — Hinjewadi"
    }

    object CheckIn {
        const val locationName = "Hinjewadi Phase 1"
        const val lat = 18.5912
        const val lng = 73.7389
        const val timestampMs = 1750352400000L
    }
}
