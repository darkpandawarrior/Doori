package com.miletracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.miletracker.core.data.database.buildMileTrackerDatabase
import com.miletracker.core.data.model.display.SurfaceSnapshot
import com.miletracker.core.data.model.display.SurfaceSnapshotProducer
import kotlinx.coroutines.flow.first
import kotlin.math.round

// Fixed palette (the app's matrix-dark surface + green accent) — plain Glance colors keep the widget free
// of the Material-You/glance-material3 surface so it renders identically across hosts.
private val SurfaceColor = Color(0xFF0E1116)
private val AccentColor = Color(0xFF4ADE80)
private val OnSurfaceColor = Color(0xFFE6E6E6)

/**
 * G11: a home-screen [GlanceAppWidget] summarising today's / this-week's mileage — the missing **consumer**
 * of the shared [SurfaceSnapshot] (the producer + its test already exist in :core:data).
 *
 * On each update it folds the completed-track list (Room) through [SurfaceSnapshotProducer] and renders the
 * result with Glance. (When L.1 lands a persisted SnapshotPublisher, this can switch to reading the published
 * snapshot instead of touching the DB directly — the render layer stays the same.)
 */
class MileageSummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshot = loadSnapshot(context)
        provideContent {
            MileageSummaryContent(snapshot)
        }
    }

    private suspend fun loadSnapshot(context: Context): SurfaceSnapshot {
        val db = buildMileTrackerDatabase(context)
        return try {
            val completed = db.savedTrackDao().getCompletedTracks().first()
            SurfaceSnapshotProducer.produce(
                completedTracks = completed,
                isTracking = db.savedTrackDao().getActiveTrack() != null,
                nowEpochMs = System.currentTimeMillis(),
            )
        } finally {
            db.close()
        }
    }
}

/**
 * Stateless render of a [SurfaceSnapshot]. Public so the Glance render test can drive it directly with a
 * fixed snapshot (no DB), matching the "test trivially" contract of the shared model.
 */
@Composable
fun MileageSummaryContent(snapshot: SurfaceSnapshot) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(SurfaceColor)
                .cornerRadius(16.dp)
                .padding(16.dp),
    ) {
        Text(
            text = "Mileway",
            style = TextStyle(color = ColorProvider(AccentColor), fontWeight = FontWeight.Bold, fontSize = 16.sp),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Today   ${format1(snapshot.todayDistanceKm)} km · ${snapshot.todayTrips} trips",
            style = TextStyle(color = ColorProvider(OnSurfaceColor), fontSize = 14.sp),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Week    ${format1(snapshot.weekDistanceKm)} km · ${snapshot.weekTrips} trips",
            style = TextStyle(color = ColorProvider(OnSurfaceColor), fontSize = 14.sp),
        )
        Spacer(GlanceModifier.height(4.dp))
        // L.1: weekly-goal progress from the enriched snapshot.
        Text(
            text = "Goal    ${(snapshot.weekGoalProgress * 100).toInt()}% of ${format1(snapshot.weekGoalKm)} km",
            style = TextStyle(color = ColorProvider(OnSurfaceColor), fontSize = 13.sp),
        )
        if (snapshot.actionRequiredCount > 0) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "⚑ ${snapshot.actionRequiredCount} to submit",
                style = TextStyle(color = ColorProvider(AccentColor), fontWeight = FontWeight.Medium, fontSize = 13.sp),
            )
        }
        if (snapshot.isTracking) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = if (snapshot.isPaused) "‖ Paused" else "● Tracking now",
                style = TextStyle(color = ColorProvider(AccentColor), fontWeight = FontWeight.Medium, fontSize = 13.sp),
            )
        }
    }
}

private fun format1(value: Double): String {
    val scaled = round(value * 10.0) / 10.0
    return scaled.toString()
}

/** Registers [MileageSummaryWidget] with the platform (declared in the manifest). */
class MileageSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MileageSummaryWidget()
}
