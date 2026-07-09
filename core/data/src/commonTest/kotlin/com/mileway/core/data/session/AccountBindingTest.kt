package com.mileway.core.data.session

import com.mileway.core.data.model.db.SavedTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V22 P3.3: pure-function coverage for [doesSessionBelongTo]/[isSessionFromDifferentAccount],
 * the boolean-mismatch ownership check every trip's `started_by_*` pointer is validated against.
 */
class AccountBindingTest {
    private val matchingIdentity =
        SignedInIdentity(
            accountId = "ACC-001",
            employeeCode = "EMP-1234",
            accountEmail = "demo@mileway.app",
            tenant = "DEMO-TENANT",
        )

    private val matchingBinding =
        TripOwnershipBinding(
            accountId = "ACC-001",
            employeeCode = "EMP-1234",
            accountEmail = "demo@mileway.app",
            tenant = "DEMO-TENANT",
        )

    @Test
    fun `binding matching every identity field belongs to that identity`() {
        assertTrue(doesSessionBelongTo(matchingBinding, matchingIdentity))
        assertFalse(isSessionFromDifferentAccount(matchingBinding, matchingIdentity))
    }

    @Test
    fun `binding with a different accountId does not belong to the current identity`() {
        val binding = matchingBinding.copy(accountId = "ACC-002")
        assertFalse(doesSessionBelongTo(binding, matchingIdentity))
        assertTrue(isSessionFromDifferentAccount(binding, matchingIdentity))
    }

    @Test
    fun `binding with a different employeeCode does not belong to the current identity`() {
        val binding = matchingBinding.copy(employeeCode = "EMP-9999")
        assertFalse(doesSessionBelongTo(binding, matchingIdentity))
    }

    @Test
    fun `binding with a different email does not belong to the current identity`() {
        val binding = matchingBinding.copy(accountEmail = "someone-else@mileway.app")
        assertFalse(doesSessionBelongTo(binding, matchingIdentity))
    }

    @Test
    fun `binding with a different tenant does not belong to the current identity`() {
        val binding = matchingBinding.copy(tenant = "OTHER-TENANT")
        assertFalse(doesSessionBelongTo(binding, matchingIdentity))
    }

    @Test
    fun `blank binding fields are treated as unknown, not a mismatch`() {
        val binding =
            TripOwnershipBinding(
                accountId = null,
                employeeCode = "",
                accountEmail = "",
                tenant = "",
            )
        assertTrue(doesSessionBelongTo(binding, matchingIdentity))
    }

    @Test
    fun `null identity fields are treated as unknown, not a mismatch`() {
        val identity =
            SignedInIdentity(
                accountId = null,
                employeeCode = null,
                accountEmail = null,
                tenant = "DEMO-TENANT",
            )
        assertTrue(doesSessionBelongTo(matchingBinding, identity))
    }

    @Test
    fun `TripOwnershipBinding from() reads a SavedTrack's started_by_* columns`() {
        val track =
            SavedTrack(
                routeId = "route-1",
                name = "Journey route-1",
                startedByAccountId = "ACC-001",
                startedByEmployeeCode = "EMP-1234",
                startedByAccountEmail = "demo@mileway.app",
                startedByTenant = "DEMO-TENANT",
                startLatitude = 0.0, startLongitude = 0.0,
                endLatitude = 0.0, endLongitude = 0.0,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = 0L, endTime = -1L,
                distance = 0.0, duration = 0L,
            )

        val binding = TripOwnershipBinding.from(track)

        assertEquals(matchingBinding, binding)
        assertTrue(doesSessionBelongTo(binding, matchingIdentity))
    }

    // ── PLAN_V24 P7.3: act-on-behalf session isolation ──────────────────────────

    private val baseSession =
        SessionState(employeeCode = "EMP-MGR", email = "manager@mileway.app", tenant = "DEMO-TENANT")

    private val acting =
        DelegationState(
            isActing = true,
            actingName = "Priya Sharma",
            actingEmail = "priya.sharma@mileway.app",
            actingCode = "EMP-2101",
        )

    @Test
    fun `a trip stamped while acting belongs to the delegate, not the manager's base account`() {
        val actingIdentity = effectiveSignedInIdentity(accountId = null, session = baseSession, delegation = acting)
        val baseIdentity = effectiveSignedInIdentity(accountId = null, session = baseSession, delegation = DelegationState())

        // A trip stamped with the effective (acting) identity's ownership pointer.
        val actingTrip =
            TripOwnershipBinding(
                accountId = null,
                employeeCode = actingIdentity.employeeCode!!,
                accountEmail = actingIdentity.accountEmail!!,
                tenant = actingIdentity.tenant,
            )

        assertTrue(doesSessionBelongTo(actingTrip, actingIdentity), "delegate's trip belongs to the delegate")
        assertFalse(doesSessionBelongTo(actingTrip, baseIdentity), "delegate's trip must NOT belong to the base manager")
    }

    @Test
    fun `a trip the manager stamped does not belong to the acting delegate identity`() {
        val actingIdentity = effectiveSignedInIdentity(accountId = null, session = baseSession, delegation = acting)
        val baseIdentity = effectiveSignedInIdentity(accountId = null, session = baseSession, delegation = DelegationState())

        val baseTrip =
            TripOwnershipBinding(
                accountId = null,
                employeeCode = baseIdentity.employeeCode!!,
                accountEmail = baseIdentity.accountEmail!!,
                tenant = baseIdentity.tenant,
            )

        assertTrue(doesSessionBelongTo(baseTrip, baseIdentity))
        assertFalse(doesSessionBelongTo(baseTrip, actingIdentity), "base manager's trip must NOT belong to the delegate")
    }
}
