package com.mileway

import com.mileway.core.common.deeplink.DeepLinkRouter
import com.mileway.feature.logging.ui.navigation.LoggingRoutes
import com.mileway.feature.profile.ui.navigation.ProfileRoutes
import com.mileway.feature.tracking.ui.navigation.TrackingRoutes
import com.mileway.ui.AppGraph
import com.mileway.ui.toAppRoute
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** DL.4: resolved DeepLinkTarget → concrete nav route. */
class DeepLinkRoutingTest {
    private fun route(uri: String) = DeepLinkRouter.resolve(uri).toAppRoute()

    @Test
    fun `section links map to section graphs`() {
        assertEquals(AppGraph.HOME, route("mileway://home"))
        assertEquals(AppGraph.TRACK, route("mileway://track"))
        assertEquals(AppGraph.LOG, route("mileway://log"))
        assertEquals(AppGraph.PROFILE, route("mileway://profile"))
    }

    @Test
    fun `sub-destinations map to nested routes`() {
        assertEquals(TrackingRoutes.CHECK_IN_HISTORY, route("mileway://track/checkin"))
        assertEquals(LoggingRoutes.EXPENSE_HISTORY, route("mileway://log/expense"))
        assertEquals(ProfileRoutes.SETTINGS, route("mileway://profile/settings"))
    }

    @Test
    fun `referral lands on profile`() {
        assertEquals(AppGraph.PROFILE, route("mileway://referral?code=ABC"))
    }

    @Test
    fun `unknown link maps to null`() {
        assertNull(route("mileway://nope"))
    }
}
