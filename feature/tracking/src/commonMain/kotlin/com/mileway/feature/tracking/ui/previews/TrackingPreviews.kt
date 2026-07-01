package com.mileway.feature.tracking.ui.previews

import androidx.compose.runtime.Composable
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.feature.tracking.ui.screens.SetupGuideScreen
import com.mileway.feature.tracking.ui.screens.TrackLoadingScreen
import com.mileway.feature.tracking.ui.screens.TrackSettingsScreen
import com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen

// ---------------------------------------------------------------------------
// Phase 9.1, Tracking feature preview matrix.
//
// All previews are DI-free: screens that accept state accept it via params;
// screens that own internal state (TrackSettingsScreen, SetupGuideScreen)
// are stateless at the call-site and need only lambdas.
// ---------------------------------------------------------------------------

// ── TrackLoadingScreen ───────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewTrackLoadingDefault() {
    PreviewSurface {
        TrackLoadingScreen(
            message = "Working on your request…",
        )
    }
}

@PreviewMatrix
@Composable
fun PreviewTrackLoadingCustomMessage() {
    PreviewSurface {
        TrackLoadingScreen(
            message = "Submitting your mileage expense…",
            subStatuses = listOf("Uploading route…", "Calculating reimbursement…", "Done!"),
        )
    }
}

// ── TrackingSuccessScreen ────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewTrackingSuccessClean() {
    PreviewSurface {
        TrackingSuccessScreen(
            distanceKm = 12.4,
            reimbursableAmount = 148.80,
            vehicleName = "Car",
            startTime = 1750348800000L,
            endTime = 1750352400000L,
            transactionId = "TXN-20260619-042",
            submissionStatus = "SUCCESS",
            violationCount = 0,
            violationMessage = null,
            voucherNumber = null,
            voucherAmount = 0.0,
            onTrackNewJourney = {},
            onViewExpense = {},
            onCreateVoucher = {},
        )
    }
}

@PreviewMatrix
@Composable
fun PreviewTrackingSuccessWithViolation() {
    PreviewSurface {
        TrackingSuccessScreen(
            distanceKm = 35.0,
            reimbursableAmount = 420.0,
            vehicleName = "Bike",
            startTime = 1750348800000L,
            endTime = 1750366800000L,
            transactionId = "TXN-20260619-099",
            submissionStatus = "POLICY_VIOLATION",
            violationCount = 2,
            violationMessage = "Distance exceeds daily limit of 30 km.",
            voucherNumber = null,
            voucherAmount = 0.0,
            onTrackNewJourney = {},
            onViewExpense = {},
            onCreateVoucher = {},
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewTrackingSuccessWithVoucher() {
    PreviewSurface {
        TrackingSuccessScreen(
            distanceKm = 8.2,
            reimbursableAmount = 98.40,
            vehicleName = "Car",
            startTime = 1750348800000L,
            endTime = 1750350600000L,
            transactionId = "TXN-20260619-101",
            submissionStatus = "SUCCESS",
            violationCount = 0,
            violationMessage = null,
            voucherNumber = "VCH-2026-0082",
            voucherAmount = 98.40,
            onTrackNewJourney = {},
            onViewExpense = {},
            onCreateVoucher = {},
        )
    }
}

// ── TrackSettingsScreen ──────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewTrackSettingsScreen() {
    PreviewSurface {
        TrackSettingsScreen(onBack = {})
    }
}

// ── SetupGuideScreen ─────────────────────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewSetupGuideScreen() {
    PreviewSurface {
        SetupGuideScreen(
            onBack = {},
            onOpenTrackSettings = {},
        )
    }
}
