package com.mileway.feature.approvals.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.mileway.core.maps.canvas.CanvasRouteSurface
import com.mileway.core.data.claim.MANAGER_ROLE
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface
import com.mileway.feature.approvals.delegate.DelegateBanner
import com.mileway.feature.approvals.viewmodel.PerLineReviewViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Route evidence is shown before the amount, comment and partial review actions. */
@Composable
internal fun PerLineReviewPanel(
    reportId: String,
    lineId: String,
    onBehalfOf: String?,
    onBack: () -> Unit,
    viewModel: PerLineReviewViewModel = koinViewModel(),
    map: MapSurface = koinInject(),
) {
    val ui by viewModel.state.collectAsState()
    LaunchedEffect(reportId, lineId) { viewModel.open(reportId, lineId) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val review = ui.review
        val line = review?.report?.lines?.find { it.id == lineId }
        if (line is MileageLine) {
            val clean = ui.route.filterNot { it.isMock || it.isPaused || it.isAbnormal }.map { MapCoordinate(it.lat, it.lng) }
            val filtered = ui.route.filter { it.isMock || it.isPaused }.map { MapCoordinate(it.lat, it.lng) }
            val abnormal = ui.route.filter { it.isAbnormal }.map { MapCoordinate(it.lat, it.lng) }
            if (ui.route.isNotEmpty()) {
                map.LiveTrackMap(
                    routeCoords = clean,
                    filteredCoords = filtered,
                    abnormalCoords = abnormal,
                    startCoord = clean.firstOrNull(),
                    endCoord = clean.lastOrNull(),
                    currentLat = ui.route.first().lat,
                    currentLng = ui.route.first().lng,
                    bearing = 0f,
                    autoCenterEnabled = false,
                    playbackCoord = null,
                    showIssueMarkers = true,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
            } else {
                Text(if (ui.loading) "Loading route evidence…" else "No recorded route for this mileage line")
            }
            Text("${line.distanceKm} km · ${line.vehicleKey}")
        }
        onBehalfOf?.let { DelegateBanner(it) }
        line?.let { Text("${it.id}: ${formatReportAmount(it.amountMinor, it.currency)}") }
        Text(if (lineId in review?.rejectedLineIds.orEmpty()) "Rejected line, excluded from payout" else "Line remains in the payable report")
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (review?.report?.state == ReportLifecycleState.SUBMITTED && review.nextRole == MANAGER_ROLE) {
            OutlinedTextField(ui.comment, viewModel::comment, label = { Text("Line review comment (required)") }, enabled = !ui.busy)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.act(ApprovalAction.APPROVE, onBehalfOf) },
                    enabled = !ui.busy && !ui.loading && ui.comment.isNotBlank() && lineId !in review.blockedLineIds,
                ) { Text("Approve line") }
                OutlinedButton(onClick = { viewModel.act(ApprovalAction.REJECT, onBehalfOf) }, enabled = !ui.busy && ui.comment.isNotBlank()) {
                    Text("Reject line")
                }
            }
        }
        OutlinedButton(onClick = onBack) { Text("Back to report") }
    }
}

@Preview
@Composable
private fun MileageReviewMapPreview() {
    MaterialTheme {
        Column {
            CanvasRouteSurface().LiveTrackMap(
                routeCoords = listOf(MapCoordinate(18.52, 73.85), MapCoordinate(18.53, 73.86)),
                filteredCoords = emptyList(),
                abnormalCoords = emptyList(),
                startCoord = MapCoordinate(18.52, 73.85),
                endCoord = MapCoordinate(18.53, 73.86),
                currentLat = 18.52,
                currentLng = 73.85,
                bearing = 0f,
                autoCenterEnabled = false,
                playbackCoord = null,
                showIssueMarkers = true,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
            Text("Mileage line review")
            Text("Rejecting one line leaves the rest approvable")
        }
    }
}
