package com.miletracker.navgraph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.github.skydoves.navgraph.annotations.NavPreview

// ---------------------------------------------------------------------------
// Representative stand-ins, DI-free, hardcoded data.
// @NavDestination links each composable to its AppKey destination.
// @NavEdge declares which destinations are reachable from this screen.
// @NavPreview ties a @Preview to a destination for thumbnail rendering.
// ---------------------------------------------------------------------------

@NavEdge(to = TrackMiles::class)
@NavEdge(to = LogMiles::class)
@NavEdge(to = Approvals::class)
@NavEdge(to = Travel::class)
@NavEdge(to = Profile::class)
@NavEdge(to = AgentChat::class)
@NavDestination(route = Home::class)
@Composable
fun HomeNavScreen() {
    PlaceholderScreen("Home")
}

@NavEdge(to = TripDetail::class)
@NavEdge(to = CheckInHistory::class)
@NavDestination(route = TrackMiles::class)
@Composable
fun TrackMilesNavScreen() {
    PlaceholderScreen("Track Miles")
}

@NavEdge(to = TripDetail::class)
@NavDestination(route = LogMiles::class)
@Composable
fun LogMilesNavScreen() {
    PlaceholderScreen("Log Miles")
}

@NavDestination(route = TripDetail::class)
@Composable
fun TripDetailNavScreen(routeId: String = "DEMO-001") {
    PlaceholderScreen("Trip Detail\n$routeId")
}

@NavDestination(route = CheckInHistory::class)
@Composable
fun CheckInHistoryNavScreen() {
    PlaceholderScreen("Check-In History")
}

@NavDestination(route = Approvals::class)
@Composable
fun ApprovalsNavScreen() {
    PlaceholderScreen("Approvals")
}

@NavDestination(route = Payables::class)
@Composable
fun PayablesNavScreen() {
    PlaceholderScreen("Payables")
}

@NavDestination(route = Travel::class)
@Composable
fun TravelNavScreen() {
    PlaceholderScreen("Travel")
}

@NavDestination(route = Profile::class)
@Composable
fun ProfileNavScreen() {
    PlaceholderScreen("Profile")
}

@NavDestination(route = AgentChat::class)
@Composable
fun AgentChatNavScreen() {
    PlaceholderScreen("Agent Chat")
}

@NavDestination(route = DebugMenu::class)
@Composable
fun DebugMenuNavScreen() {
    PlaceholderScreen("Debug Menu")
}

// ---------------------------------------------------------------------------
// Previews (@NavPreview + @Preview), one per destination.
// ---------------------------------------------------------------------------

@NavPreview(route = Home::class, primary = true)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewHome() { HomeNavScreen() }

@NavPreview(route = TrackMiles::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewTrackMiles() { TrackMilesNavScreen() }

@NavPreview(route = LogMiles::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewLogMiles() { LogMilesNavScreen() }

@NavPreview(route = TripDetail::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewTripDetail() { TripDetailNavScreen() }

@NavPreview(route = CheckInHistory::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewCheckInHistory() { CheckInHistoryNavScreen() }

@NavPreview(route = Approvals::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewApprovals() { ApprovalsNavScreen() }

@NavPreview(route = Payables::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewPayables() { PayablesNavScreen() }

@NavPreview(route = Travel::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewTravel() { TravelNavScreen() }

@NavPreview(route = Profile::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewProfile() { ProfileNavScreen() }

@NavPreview(route = AgentChat::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewAgentChat() { AgentChatNavScreen() }

@NavPreview(route = DebugMenu::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun PreviewDebugMenu() { DebugMenuNavScreen() }

// ---------------------------------------------------------------------------
// Shared placeholder composable used by all stand-ins above.
// ---------------------------------------------------------------------------

@Composable
private fun PlaceholderScreen(label: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
