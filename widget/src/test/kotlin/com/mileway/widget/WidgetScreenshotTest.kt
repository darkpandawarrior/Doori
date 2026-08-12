package com.mileway.widget

import android.app.Activity
import android.app.Application
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.widget.WidgetPalette
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * showcase/Widget.1: host-renders [MileageSummaryContent]'s Glance tree to a real [RemoteViews]
 * (via [GlanceRemoteViews], the same composition path the widget host uses), inflates it into a
 * plain Android [View], and captures it with Roborazzi — no on-device AppWidgetHost needed.
 *
 * Output: docs/screenshots/widget_glance.png
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w320dp-h180dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetScreenshotTest {

    @Test
    fun mileageSummaryWidget() {
        // D2 FIX (2026-08-09): this line used to be
        //   System.setProperty("roborazzi.test.record", "true")
        // which forced RECORD mode unconditionally, so every run overwrote the baseline and a
        // visual regression was literally unrepresentable — the test rewrote the evidence and
        // passed. That is why captures went stale and wrong for months with nothing alerting.
        // Verify is now the default; record only when explicitly asked.
        //
        // Via the ENV VAR, not the -P property this comment used to name: a Gradle property does
        // not reach a forked test JVM, so the documented record path was unreachable. Found by
        // deleting this capture and re-running with -P — the file simply never came back.
        //   ROBORAZZI_RECORD=true ./gradlew screenshotTest
        if (System.getenv("ROBORAZZI_RECORD") == "true") {
            System.setProperty("roborazzi.test.record", "true")
        }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val model =
            WidgetUiModel(
                todayLabel = "Today   12.4 km",
                weekLabel = "Week    58.7 km · 4 trips",
                statusLabel = "● Tracking now",
                isTracking = true,
            )

        val remoteViews =
            runBlocking {
                GlanceRemoteViews()
                    .compose(context, DpSize(220.dp, 120.dp)) {
                        // Explicitly the app's shipped theme. Defaulting here would capture
                        // the Ember fallback and the gallery would keep advertising a widget
                        // nobody has, which is the drift this whole change removes.
                        MileageSummaryContent(model, shippedWidgetColors())
                    }
                    .remoteViews
            }

        // captureRoboImage requires a View attached to an Activity's window; a bare host Activity
        // (never shown, no theme dependency beyond Robolectric's default) satisfies that.
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val hostView: View = remoteViews.apply(activity, root)
        root.addView(hostView)
        activity.setContentView(root)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(660, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 660, 360)

        val screenshotsDir = repoScreenshotsDir()
        root.captureRoboImage(File(screenshotsDir, "widget_glance.png").absolutePath)
    }

    private fun repoScreenshotsDir(): File {
        val moduleDir = File(System.getProperty("user.dir") ?: ".")
        val repoRoot = if (moduleDir.name == "widget") moduleDir.parentFile else moduleDir
        return File(repoRoot, "docs/screenshots").also { it.mkdirs() }
    }
}

/**
 * The colours the widget ships with, derived from the app's default variant the same way
 * `ThemeWidgetPaletteSource` does at runtime. `:widget` cannot see `:core:ui`, so the two values it
 * needs are stated here — and `WidgetPaletteFollowsThemeTest` in `:app` asserts the runtime source
 * agrees with every variant's spec, so this cannot quietly disagree with the app.
 */
private fun shippedWidgetColors(): WidgetColors =
    WidgetColors.from(
        WidgetPalette(
            surface = 0xFFF7F3EA, // Paper canvas
            accent = 0xFF1E3A5F, // Paper ink-navy
            live = 0xFFB3261E,
            onSurface = 0xFF241F1A,
            stale = 0xFF6E6353,
        ),
    )
