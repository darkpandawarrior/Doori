package com.mileway.core.data.domain.policy.rates

import com.mileway.core.data.domain.policy.MonthlyCarPerquisiteMirror
import kotlinx.serialization.json.Json

/**
 * Offline monthly perquisites, not a statutory per-kilometre rate. Mileage remains company policy.
 * Applies to an employer-owned or hired car, mixed official and personal use, employer-paid expenses.
 */
object IndiaCarPerquisites {
    /** Secondary-source verification is retained verbatim; no gazette verification is claimed. */
    const val json =
        """
        {
          "kind": "MONTHLY",
          "currency": "INR",
          "sourceTitle": "Income-tax Rules, 2026 (G.S.R. 198(E), Notification 22/2026, 20 March 2026)",
          "sourceUrl": "https://kpmg.com/xx/en/our-insights/gms-flash-alert/2026/flash-alert-2026-051.html",
          "retrievedOn": "2026-10-04",
          "verification": "secondary sources (Mercans statutory alert; KPMG GMS flash alert 2026-051); gazette text not fetched",
          "versions": [
            {
              "effectiveFrom": "2026-04-01",
              "upTo1600CcMinor": 500000,
              "above1600CcMinor": 700000,
              "chauffeurMinor": 300000
            }
          ]
        }
        """

    /** Separate monthly model prevents these figures from feeding mileage schedules. */
    val mirror: MonthlyCarPerquisiteMirror by lazy { Json.decodeFromString<MonthlyCarPerquisiteMirror>(json) }
}
