package com.mileway

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.widget.MileageSummaryContent
import org.junit.Test

/**
 * G11: Glance render test for the home-screen widget. Uses the host-side Glance unit-test environment
 * (`runGlanceAppWidgetUnitTest`, no emulator) to compose [MileageSummaryContent] from a fixed
 * [SurfaceSnapshot] and assert the summary text renders — proving the SurfaceSnapshot *consumer* works.
 */
class MileageSummaryWidgetTest {
    @Test
    fun rendersSnapshotSummary() =
        runGlanceAppWidgetUnitTest {
            provideComposable {
                MileageSummaryContent(
                    SurfaceSnapshot(
                        todayDistanceKm = 12.3,
                        todayTrips = 2,
                        weekDistanceKm = 45.6,
                        weekTrips = 7,
                        isTracking = true,
                    ),
                )
            }

            // Full rendered strings (match under either exact or contains matcher semantics).
            onNode(hasText("Mileway")).assertExists()
            onNode(hasText("Today   12.3 km · 2 trips")).assertExists()
            onNode(hasText("Week    45.6 km · 7 trips")).assertExists()
            onNode(hasText("● Tracking now")).assertExists()
        }
}
