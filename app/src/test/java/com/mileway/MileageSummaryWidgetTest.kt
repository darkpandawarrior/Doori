package com.mileway

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import com.mileway.core.data.watch.WatchSyncPayload
import com.mileway.widget.MileageSummaryContent
import com.mileway.widget.toWidgetUiModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * G11/P6.2: Glance render test for the home-screen widget. Uses the host-side Glance unit-test
 * environment (`runGlanceAppWidgetUnitTest`, no emulator) to compose [MileageSummaryContent] from a
 * fixed [com.mileway.widget.WidgetUiModel] and assert the summary text renders — proving the
 * [WatchSyncPayload] *consumer* works.
 */
class MileageSummaryWidgetTest {
    @Test
    fun rendersSnapshotSummary() =
        runGlanceAppWidgetUnitTest {
            provideComposable {
                MileageSummaryContent(
                    WatchSyncPayload(
                        todayKm = 12.3,
                        weekKm = 45.6,
                        tripCount = 7,
                        isTracking = true,
                    ).toWidgetUiModel(),
                )
            }

            // Full rendered strings (match under either exact or contains matcher semantics).
            onNode(hasText("Doori")).assertExists()
            onNode(hasText("Today   12.3 km")).assertExists()
            onNode(hasText("Week    45.6 km · 7 trips")).assertExists()
            onNode(hasText("● Tracking now")).assertExists()
            onNode(hasText("■ Stop")).assertExists()
        }

    @Test
    fun rendersStartButtonWhenNotTracking() =
        runGlanceAppWidgetUnitTest {
            provideComposable {
                MileageSummaryContent(WatchSyncPayload(isTracking = false).toWidgetUiModel())
            }

            onNode(hasText("▶ Start")).assertExists()
        }

    // ── P8.1: a11y — TalkBack must announce the action, not the glyph, for the quick-start toggle. ──

    @Test
    fun `start toggle has an accessible action label when idle`() =
        runGlanceAppWidgetUnitTest {
            provideComposable {
                MileageSummaryContent(WatchSyncPayload(isTracking = false).toWidgetUiModel())
            }

            onNode(hasContentDescriptionEqualTo("Start tracking")).assertExists()
        }

    @Test
    fun `stop toggle has an accessible action label when tracking`() =
        runGlanceAppWidgetUnitTest {
            provideComposable {
                MileageSummaryContent(WatchSyncPayload(isTracking = true).toWidgetUiModel())
            }

            onNode(hasContentDescriptionEqualTo("Stop tracking")).assertExists()
        }

    // ── P6.2: pure state->widget mapper (acceptance: "unit test on the state→widget mapper") ────

    @Test
    fun `mapper formats today and week labels from the payload`() {
        val model = WatchSyncPayload(todayKm = 8.2, weekKm = 18.0, tripCount = 3).toWidgetUiModel()

        assertEquals("Today   8.2 km", model.todayLabel)
        assertEquals("Week    18.0 km · 3 trips", model.weekLabel)
    }

    @Test
    fun `mapper reports tracking status`() {
        val tracking = WatchSyncPayload(isTracking = true, isPaused = false).toWidgetUiModel()
        val paused = WatchSyncPayload(isTracking = true, isPaused = true).toWidgetUiModel()
        val idle = WatchSyncPayload(isTracking = false).toWidgetUiModel()

        assertEquals("● Tracking now", tracking.statusLabel)
        assertEquals("‖ Paused", paused.statusLabel)
        assertNull(idle.statusLabel)
        assertEquals(true, tracking.isTracking)
        assertEquals(false, idle.isTracking)
    }
}
