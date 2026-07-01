package com.mileway.feature.travel.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.feature.travel.model.BookingRecord
import com.mileway.feature.travel.model.TransportMode
import com.mileway.feature.travel.model.TripStatus
import kotlin.math.round

// KMP-safe 2-decimal formatter (avoids JVM-only String.format).
private fun Double.fmt2(): String {
    val scaled = round(this * 100).toLong()
    val whole = scaled / 100
    val frac = (scaled % 100).let { if (it < 0) -it else it }
    return "$whole.${frac.toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------
// Phase 9.1, Travel feature preview matrix.
//
// The travel feature has no screen composables yet; these previews establish
// the design baseline for the upcoming BookingRecord list and detail screens.
// All data is hardcoded, no DI or ViewModel required.
// ---------------------------------------------------------------------------

// ── Sample data ──────────────────────────────────────────────────────────────

private val BASE_MS = 1_781_654_400_000L
private val DAY_MS = 86_400_000L

private val sampleFlightActive =
    BookingRecord(
        id = "BK001",
        mode = TransportMode.FLIGHT,
        origin = "PNQ",
        destination = "BOM",
        carrier = "IndiGo",
        flightOrTrainNumber = "6E-401",
        departureMs = BASE_MS + DAY_MS,
        amountRupees = 3600.0,
        status = TripStatus.ACTIVE,
        gate = "B7",
        boardingTime = "14:30",
    )

private val sampleTrainUpcoming =
    BookingRecord(
        id = "BK003",
        mode = TransportMode.TRAIN,
        origin = "PNQ",
        destination = "BLR",
        carrier = "Indian Railways",
        flightOrTrainNumber = "Deccan Queen",
        departureMs = BASE_MS + 28 * DAY_MS,
        amountRupees = 1200.0,
        status = TripStatus.UPCOMING,
    )

private val sampleFlightCompleted =
    BookingRecord(
        id = "BK005",
        mode = TransportMode.FLIGHT,
        origin = "DEL",
        destination = "PNQ",
        carrier = "IndiGo",
        flightOrTrainNumber = "6E-712",
        departureMs = BASE_MS - 7 * DAY_MS,
        amountRupees = 4900.0,
        status = TripStatus.COMPLETED,
    )

// ── Icon mapper ──────────────────────────────────────────────────────────────

private fun modeIcon(mode: TransportMode): ImageVector =
    when (mode) {
        TransportMode.FLIGHT -> Icons.Filled.AirplanemodeActive
        TransportMode.TRAIN -> Icons.Filled.Train
        TransportMode.BUS -> Icons.Filled.DirectionsBus
        TransportMode.CAB -> Icons.Filled.DirectionsCar
    }

// ── Booking card ─────────────────────────────────────────────────────────────

@Composable
private fun BookingCard(booking: BookingRecord) {
    val (statusColor, statusLabel) =
        when (booking.status) {
            TripStatus.ACTIVE -> Color(0xFF22C55E) to "Active"
            TripStatus.UPCOMING -> Color(0xFF3B82F6) to "Upcoming"
            TripStatus.COMPLETED -> Color(0xFF94A3B8) to "Completed"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = modeIcon(booking.mode),
                        contentDescription = booking.mode.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "${booking.origin}  →  ${booking.destination}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${booking.carrier}  •  ${booking.flightOrTrainNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (booking.gate != null && booking.boardingTime != null) {
                Text(
                    text = "Gate ${booking.gate}  •  Boarding ${booking.boardingTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "₹${booking.amountRupees.fmt2()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Active flight booking ────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewBookingCardActiveFlight() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            BookingCard(booking = sampleFlightActive)
        }
    }
}

// ── Upcoming train booking ───────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewBookingCardUpcomingTrain() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            BookingCard(booking = sampleTrainUpcoming)
        }
    }
}

// ── Full matrix, completed flight ───────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewBookingCardCompletedFlight() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            BookingCard(booking = sampleFlightCompleted)
        }
    }
}

// ── Full matrix, booking list ────────────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewBookingListMatrix() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "My Trips",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            BookingCard(booking = sampleFlightActive)
            BookingCard(booking = sampleTrainUpcoming)
            BookingCard(booking = sampleFlightCompleted)
        }
    }
}
