package com.mileway.core.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle
import com.siddharth.kmp.common.UiText

/**
 * P28.DETAIL.1 proof: TrackDetailScreen's entire content — gradient summary hero, 2-column metrics
 * grid (with the amount/vehicle row's data-driven visibility preserved), attachments, and its five
 * action buttons — expressed purely through [buildDetail]. Nothing here is left over: the only
 * field the ten-type set can't express is the bespoke hero card, which is exactly what [DetailField.Slot]
 * is for. Feeds [DetailScreenSamplePreview]; structure is asserted by DetailSpecTest.
 */
fun sampleTrackDetailSpec(hasReimbursableAmount: Boolean = true): DetailSpec =
    buildDetail {
        section(id = "summary") {
            // The gradient hero (status chip + health badge + big distance figure + date) is
            // bespoke visual design, not one of the ten field types — a Slot renders it verbatim.
            slot(id = "summary_hero") { SampleSummaryHero() }
        }
        section(id = "metrics") {
            // `id` and the Metric varargs are passed positionally — Kotlin requires positional
            // arguments before any named ones, and `metricGrid`'s trailing `columns`/`visible`
            // params sit after the vararg so they must be named when supplied.
            metricGrid(
                "metrics_distance_duration",
                DetailField.MetricGrid.Metric(UiText.of("Distance"), UiText.of("128.4 km"), Icons.Default.Straighten),
                DetailField.MetricGrid.Metric(UiText.of("Duration"), UiText.of("2h 14m"), Icons.Default.Timer),
            )
            metricGrid(
                "metrics_speed_gps",
                DetailField.MetricGrid.Metric(UiText.of("Avg speed"), UiText.of("58 km/h"), Icons.Default.Speed),
                DetailField.MetricGrid.Metric(UiText.of("GPS points"), UiText.of("412"), Icons.Default.Place),
            )
            // Mirrors TrackDetailScreen's own `if (track.reimbursableAmount > 0 || ...)` guard —
            // the caller evaluates its own data condition and passes the result as `visible`.
            metricGrid(
                "metrics_amount_vehicle",
                DetailField.MetricGrid.Metric(UiText.of("Amount"), UiText.of("₹640"), Icons.Default.CheckCircle),
                DetailField.MetricGrid.Metric(UiText.of("Vehicle"), UiText.of("Four Wheeler Petrol"), Icons.Default.Map),
                visible = hasReimbursableAmount,
            )
        }
        section(id = "attachments") {
            attachments(
                id = "attachments_list",
                title = UiText.of("Attachments"),
                attachments =
                    listOf(
                        DetailField.AttachmentList.Attachment(label = UiText.of("Odo start")) { SampleAttachmentThumbnail() },
                        DetailField.AttachmentList.Attachment(label = UiText.of("Odo end")) { SampleAttachmentThumbnail() },
                        DetailField.AttachmentList.Attachment { SampleAttachmentThumbnail() },
                    ),
            )
        }
        section(id = "actions") {
            actionRow(
                "primary_actions",
                DetailAction(id = "export", label = UiText.of("Export data"), icon = Icons.Default.Download, onClick = {}),
                DetailAction(id = "view_map", label = UiText.of("View route map"), icon = Icons.Default.Map, onClick = {}),
                DetailAction(id = "insights", label = UiText.of("Trip insights"), icon = Icons.Default.Insights, onClick = {}),
                DetailAction(id = "hw_events", label = UiText.of("Hardware events"), icon = Icons.Default.History, onClick = {}),
                DetailAction(id = "route_points", label = UiText.of("Route points"), icon = Icons.Default.Place, onClick = {}),
            )
        }
    }

@Composable
private fun SampleSummaryHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedLg,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.raised),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(DesignTokens.topBarGradientBrush()).padding(DesignTokens.Spacing.xl),
        ) {
            StatusChip("Saved", StatusTone.Info)
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text("128.4 km", style = MaterialTheme.typography.displaySmall.dataStyle(), fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SampleAttachmentThumbnail() {
    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {}
}

@PreviewLightDark
@Composable
private fun DetailScreenSamplePreview() {
    PreviewSurface {
        DetailScreen(spec = sampleTrackDetailSpec())
    }
}
