package com.mileway.core.data.domain.policy.rates

import com.mileway.core.data.domain.policy.MileageRateMirror
import kotlinx.serialization.json.Json

/** Offline HMRC Approved Mileage Allowance Payments mirror for cars and vans. */
object HmrcMileageRates {
    /** The first band counts cumulative business miles across all vehicles in the tax year. */
    const val json =
        """
        {
          "sourceTitle": "HMRC Approved Mileage Allowance Payments: cars and vans",
          "sourceUrl": "https://www.gov.uk/expenses-and-benefits-business-travel-mileage/rules-for-tax",
          "retrievedOn": "2026-10-04",
          "currency": "GBP",
          "annualPeriod": "UK_TAX_YEAR",
          "versions": [
            {
              "effectiveFrom": "2025-04-06",
              "authority": "HMRC tax year 2025/26",
              "schedule": {
                "distanceUnit": "MILE",
                "firstRateThousandthsMinor": 45000,
                "firstBandDistanceUnits": 10000,
                "aboveBandRateThousandthsMinor": 25000
              }
            },
            {
              "effectiveFrom": "2026-04-06",
              "authority": "HMRC tax year 2026/27",
              "schedule": {
                "distanceUnit": "MILE",
                "firstRateThousandthsMinor": 55000,
                "firstBandDistanceUnits": 10000,
                "aboveBandRateThousandthsMinor": 25000
              }
            }
          ]
        }
        """

    /** Parsed cited data, without resource loading or a network request. */
    val mirror: MileageRateMirror by lazy { Json.decodeFromString<MileageRateMirror>(json) }
}
