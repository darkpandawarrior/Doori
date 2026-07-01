package com.mileway.feature.tracking.ui.screens

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mileway.core.ui.theme.MilewayTheme

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackingSuccessScreenPreview() {
    MilewayTheme {
        TrackingSuccessScreen(
            distanceKm = 12.4,
            reimbursableAmount = 185.60,
            vehicleName = "Honda City",
            startTime = 1_700_000_000_000L,
            endTime = 1_700_003_600_000L,
            transactionId = "TXN-20241115-0042",
            submissionStatus = "APPROVED",
            violationCount = 0,
            violationMessage = null,
            voucherNumber = "V-2024-0112",
            voucherAmount = 185.60,
            onTrackNewJourney = {},
            onViewExpense = {},
            onCreateVoucher = {},
        )
    }
}

@Preview(name = "With violations", showBackground = true)
@Composable
private fun TrackingSuccessViolationsPreview() {
    MilewayTheme {
        TrackingSuccessScreen(
            distanceKm = 8.7,
            reimbursableAmount = 130.50,
            vehicleName = "Suzuki Swift",
            startTime = 1_700_000_000_000L,
            endTime = 1_700_001_800_000L,
            transactionId = null,
            submissionStatus = "PENDING",
            violationCount = 2,
            violationMessage = "Speed exceeded 80 km/h for 3 min",
            voucherNumber = null,
            voucherAmount = 0.0,
            onTrackNewJourney = {},
            onViewExpense = {},
            onCreateVoucher = {},
        )
    }
}
