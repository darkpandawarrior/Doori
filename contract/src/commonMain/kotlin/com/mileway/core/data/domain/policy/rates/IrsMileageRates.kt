package com.mileway.core.data.domain.policy.rates

import com.mileway.core.data.domain.policy.MileageRateMirror
import kotlinx.serialization.json.Json

/** Offline IRS business-mileage mirror. Employer FAVR payment inputs are deliberately absent. */
object IrsMileageRates {
    /** One source of truth for each published figure, available on every commonMain target. */
    const val json =
            """
        {
          "sourceTitle": "IRS standard mileage rates: business use",
          "sourceUrl": "https://www.irs.gov/tax-professionals/standard-mileage-rates",
          "retrievedOn": "2026-10-04",
          "currency": "USD",
          "annualPeriod": "CALENDAR_YEAR",
          "versions": [
            {
              "effectiveFrom": "2025-01-01",
              "authority": "IR-2024-312",
              "schedule": { "distanceUnit": "MILE", "firstRateThousandthsMinor": 70000 }
            },
            {
              "effectiveFrom": "2026-01-01",
              "authority": "Notice 2026-10; IR-2025-128",
              "schedule": { "distanceUnit": "MILE", "firstRateThousandthsMinor": 72500 }
            },
            {
              "effectiveFrom": "2026-07-01",
              "authority": "IR-2026-29",
              "schedule": { "distanceUnit": "MILE", "firstRateThousandthsMinor": 76000 }
            }
          ],
          "favrCostLimits": [
            {
              "effectiveFrom": "2026-01-01",
              "maxStandardAutomobileCostMinor": 6170000,
              "sourceTitle": "IRS Notice 2026-10, section 5; Rev. Proc. 2019-46, section 6.02(6)",
              "sourceUrl": "https://www.irs.gov/pub/irs-drop/n-26-10.pdf"
            }
          ]
        }
        """

    /** Parsed cited data, without resource loading or a network request. */
    val mirror: MileageRateMirror by lazy { Json.decodeFromString<MileageRateMirror>(json) }
}
