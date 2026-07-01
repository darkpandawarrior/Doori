package com.mileway

import com.mileway.core.network.model.TrackingContextType
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.stub.DemoMockData
import com.mileway.stub.ProfileMockData
import com.mileway.stub.VendorMockData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the vendor directory, frequent routes, tracking contexts and rich
 * profile mock data are well-formed and internally consistent.
 */
class VendorAndProfileMockTest {

    // Rough bounding box around the demo city (Pune) used by the mock coordinates.
    private val latRange = 18.35..18.75
    private val lngRange = 73.65..74.10

    // ── Vendors ───────────────────────────────────────────────────────────────

    @Test
    fun `vendor centers are within city bounds with positive radii`() {
        val centers = VendorMockData.vendorCenters()
        assertEquals(7, centers.size, "expected 7 vendor centers")
        centers.forEach { c ->
            assertTrue(c.lat in latRange, "${c.id} lat ${c.lat} outside city bounds")
            assertTrue(c.lng in lngRange, "${c.id} lng ${c.lng} outside city bounds")
            assertTrue(c.radiusMeters > 0, "${c.id} radius must be positive")
        }
    }

    @Test
    fun `vendor centers have unique ids and non-blank names and addresses`() {
        val centers = VendorMockData.vendorCenters()
        assertEquals(centers.size, centers.map { it.id }.toSet().size, "vendor ids must be unique")
        centers.forEach { c ->
            assertTrue(c.name.isNotBlank(), "${c.id} name must not be blank")
            assertTrue(!c.address.isNullOrBlank(), "${c.id} address must not be blank")
            assertTrue(!c.city.isNullOrBlank(), "${c.id} city must not be blank")
        }
    }

    // ── Frequent routes ───────────────────────────────────────────────────────

    @Test
    fun `frequent routes have non-blank fields and sane values`() {
        val routes = VendorMockData.frequentRoutes()
        assertEquals(3, routes.size, "expected 3 frequent routes")
        assertEquals(routes.size, routes.map { it.id }.toSet().size, "route ids must be unique")
        routes.forEach { r ->
            assertTrue(r.id.isNotBlank(), "route id must not be blank")
            assertTrue(r.label.isNotBlank(), "${r.id} label must not be blank")
            assertTrue(r.fromName.isNotBlank(), "${r.id} fromName must not be blank")
            assertTrue(r.toName.isNotBlank(), "${r.id} toName must not be blank")
            assertTrue(r.distanceKm > 0, "${r.id} distance must be positive")
            assertTrue(r.timesUsed > 0, "${r.id} timesUsed must be positive")
        }
    }

    // ── Tracking contexts ─────────────────────────────────────────────────────

    @Test
    fun `tracking contexts cover trip, petty-cash and event with unique ids`() {
        val contexts = VendorMockData.trackingContexts()
        assertEquals(3, contexts.size, "expected 3 tracking contexts")
        assertEquals(contexts.size, contexts.map { it.id }.toSet().size, "context ids must be unique")
        val types = contexts.map { it.type }.toSet()
        assertTrue(TrackingContextType.TRIP in types, "missing trip context")
        assertTrue(TrackingContextType.PETTY_CASH in types, "missing petty-cash context")
        assertTrue(TrackingContextType.EVENT in types, "missing event context")
        contexts.forEach { c -> assertTrue(c.title.isNotBlank(), "${c.id} title must not be blank") }
    }

    // ── Profile completion ────────────────────────────────────────────────────

    @Test
    fun `completion percent matches the sum of category counts`() {
        val completion = ProfileMockData.completion()
        assertEquals(6, completion.categories.size, "expected 6 completion categories")
        val done = completion.categories.sumOf { it.done }
        val total = completion.categories.sumOf { it.total }
        completion.categories.forEach { c ->
            assertTrue(c.done in 0..c.total, "${c.name}: done ${c.done} must be within 0..${c.total}")
        }
        assertEquals(done * 100 / total, completion.percent, "headline percent must match counts")
        assertEquals(72, completion.percent, "demo completion should sit at 72%")
    }

    // ── Profile identity ──────────────────────────────────────────────────────

    @Test
    fun `rich profile identity matches the demo user config`() {
        val profile = ProfileMockData.primaryProfile()
        val configProfile = DemoMockData.userConfig().profile
        assertEquals(configProfile?.name, profile.name)
        assertEquals(configProfile?.email, profile.email)
        assertEquals(configProfile?.code, profile.employeeCode)
        assertTrue(profile.role.isNotBlank(), "role must not be blank")
        assertTrue(profile.organization.isNotBlank(), "organization must not be blank")
        assertTrue(profile.manager.isNotBlank(), "manager must not be blank")
    }

    // ── Sessions & accounts ───────────────────────────────────────────────────

    @Test
    fun `sessions list exactly one current session as the most recent`() {
        val sessions = ProfileMockData.sessions()
        assertEquals(3, sessions.size, "expected 3 sessions")
        assertEquals(1, sessions.count { it.isCurrent }, "exactly one session must be current")
        val current = sessions.first { it.isCurrent }
        assertEquals(
            sessions.maxOf { it.lastActiveMillis },
            current.lastActiveMillis,
            "current session must be the most recently active"
        )
        sessions.forEach { s ->
            assertTrue(s.deviceName.isNotBlank(), "deviceName must not be blank")
            assertTrue(s.platform.isNotBlank(), "platform must not be blank")
            assertTrue(s.lastActiveMillis > 0, "lastActiveMillis must be positive")
        }
    }

    @Test
    fun `accounts are switchable, unique and include the primary identity`() {
        val accounts = ProfileMockData.accounts()
        assertEquals(3, accounts.size, "expected 3 demo accounts")
        assertEquals(accounts.size, accounts.map { it.id }.toSet().size, "account ids must be unique")
        accounts.forEach { a ->
            assertTrue(a.displayName.isNotBlank(), "${a.id} displayName must not be blank")
            assertTrue(a.employeeCode.isNotBlank(), "${a.id} employeeCode must not be blank")
            assertTrue(a.organization.isNotBlank(), "${a.id} organization must not be blank")
        }
        assertTrue(
            accounts.any { it.employeeCode == ProfileMockData.primaryProfile().employeeCode },
            "one switchable account must match the primary employee code"
        )
    }

    // ── Repository delegation ─────────────────────────────────────────────────

    @Test
    fun `fake profile repository serves the mock profile surfaces`() {
        val repository = FakeProfileRepository()
        assertEquals(ProfileMockData.primaryProfile(), repository.richProfile())
        assertEquals(ProfileMockData.completion(), repository.completion())
        assertEquals(ProfileMockData.sessions(), repository.sessions())
        assertEquals(ProfileMockData.accounts(), repository.accounts())
    }

    @Test
    fun `mock providers are deterministic across calls`() {
        assertEquals(VendorMockData.vendorCenters(), VendorMockData.vendorCenters())
        assertEquals(VendorMockData.frequentRoutes(), VendorMockData.frequentRoutes())
        assertEquals(VendorMockData.trackingContexts(), VendorMockData.trackingContexts())
        assertEquals(ProfileMockData.completion(), ProfileMockData.completion())
        assertEquals(ProfileMockData.sessions(), ProfileMockData.sessions())
    }
}
