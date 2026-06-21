package com.miletracker

import com.miletracker.core.common.deeplink.DeepLinkRouter
import com.miletracker.feature.logging.ui.navigation.LoggingRoutes
import com.miletracker.feature.profile.ui.navigation.ProfileRoutes
import com.miletracker.feature.tracking.ui.navigation.TrackingRoutes
import com.miletracker.ui.AppGraph
import com.miletracker.ui.toAppRoute
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** DL.4 — resolved DeepLinkTarget → concrete nav route. */
class DeepLinkRoutingTest {
    private fun route(uri: String) = DeepLinkRouter.resolve(uri).toAppRoute()

    @Test
    fun `section links map to section graphs`() {
        assertEquals(AppGraph.HOME, route("miletracker://home"))
        assertEquals(AppGraph.TRACK, route("miletracker://track"))
        assertEquals(AppGraph.LOG, route("miletracker://log"))
        assertEquals(AppGraph.PROFILE, route("miletracker://profile"))
    }

    @Test
    fun `sub-destinations map to nested routes`() {
        assertEquals(TrackingRoutes.CHECK_IN_HISTORY, route("miletracker://track/checkin"))
        assertEquals(LoggingRoutes.EXPENSE_HISTORY, route("miletracker://log/expense"))
        assertEquals(ProfileRoutes.SETTINGS, route("miletracker://profile/settings"))
    }

    @Test
    fun `referral lands on profile`() {
        assertEquals(AppGraph.PROFILE, route("miletracker://referral?code=ABC"))
    }

    @Test
    fun `unknown link maps to null`() {
        assertNull(route("miletracker://nope"))
    }
}
