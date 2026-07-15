package com.mileway.core.platform

import com.siddharth.kmp.appshell.AppPermission
import com.siddharth.kmp.appshell.PermissionResult
import com.siddharth.kmp.appshell.PermissionsProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wave-3 permission-onboarding: tier ordering/classification, skip-impact copy, OEM-hint mapping, and the
 * resume re-check state machine — all pure logic over a fake [PermissionsProvider], no platform types.
 */
class PermissionOnboardingFlowTest {
    private class FakeProvider(
        val granted: MutableSet<AppPermission> = mutableSetOf(),
        val requestResults: MutableMap<AppPermission, PermissionResult> = mutableMapOf(),
    ) : PermissionsProvider {
        override suspend fun isGranted(permission: AppPermission): Boolean = permission in granted

        override suspend fun request(permission: AppPermission): PermissionResult {
            val result = requestResults[permission] ?: PermissionResult.Denied
            if (result == PermissionResult.Granted) granted += permission
            return result
        }
    }

    @Test
    fun `tiers are ordered location-fine then background-location then notifications then activity-recognition`() {
        assertEquals(
            listOf(
                PermissionTierId.LOCATION_FINE,
                PermissionTierId.BACKGROUND_LOCATION,
                PermissionTierId.NOTIFICATIONS,
                PermissionTierId.ACTIVITY_RECOGNITION,
            ),
            defaultPermissionTiers.map { it.id },
        )
    }

    @Test
    fun `only location-fine is required, the rest are optional`() {
        val required = defaultPermissionTiers.filter { it.required }.map { it.id }
        assertEquals(listOf(PermissionTierId.LOCATION_FINE), required)
    }

    @Test
    fun `every skippable tier has non-blank skip-impact copy`() {
        defaultPermissionTiers.filter { !it.required }.forEach { tier ->
            assertTrue(tier.skipImpact.isNotBlank(), "expected skip-impact copy for ${tier.id}")
        }
    }

    @Test
    fun `oem hint mapping resolves known manufacturers case-insensitively`() {
        assertEquals(OemBatteryHints.hintFor("xiaomi"), OemBatteryHints.hintFor("Xiaomi"))
        assertTrue(OemBatteryHints.hintFor("Samsung")!!.contains("Samsung"))
        assertTrue(OemBatteryHints.hintFor("OnePlus")!!.isNotBlank())
    }

    @Test
    fun `oem hint mapping returns null for an unknown manufacturer`() {
        assertNull(OemBatteryHints.hintFor("Acme Phone Co"))
    }

    @Test
    fun `skipAlreadyGranted advances past the required tier when already granted`() =
        runTest {
            val provider = FakeProvider(granted = mutableSetOf(AppPermission.LOCATION))
            val flow = PermissionOnboardingFlow(provider)

            flow.skipAlreadyGranted()

            assertEquals(PermissionTierId.BACKGROUND_LOCATION, flow.state.value.current?.id)
            assertTrue(flow.state.value.requiredSatisfied)
        }

    @Test
    fun `skipCurrent records Skipped for an optional tier and advances`() =
        runTest {
            val provider = FakeProvider(granted = mutableSetOf(AppPermission.LOCATION))
            val flow = PermissionOnboardingFlow(provider)
            flow.skipAlreadyGranted()

            flow.skipCurrent()

            assertEquals(TierOutcome.Skipped, flow.state.value.outcomes[PermissionTierId.BACKGROUND_LOCATION])
            assertEquals(PermissionTierId.NOTIFICATIONS, flow.state.value.current?.id)
        }

    @Test
    fun `skipCurrent is a no-op on the required tier`() =
        runTest {
            val provider = FakeProvider()
            val flow = PermissionOnboardingFlow(provider)

            flow.skipCurrent()

            assertEquals(PermissionTierId.LOCATION_FINE, flow.state.value.current?.id)
            assertTrue(flow.state.value.outcomes.isEmpty())
        }

    @Test
    fun `requestCurrent records Denied and advances when the provider denies`() =
        runTest {
            val provider = FakeProvider(requestResults = mutableMapOf(AppPermission.LOCATION to PermissionResult.Denied))
            val flow = PermissionOnboardingFlow(provider)

            val outcome = flow.requestCurrent()

            assertEquals(TierOutcome.Denied, outcome)
            assertFalse(flow.state.value.requiredSatisfied)
            assertEquals(PermissionTierId.BACKGROUND_LOCATION, flow.state.value.current?.id)
        }

    @Test
    fun `resume re-check upgrades a previously skipped tier once it is granted in settings`() =
        runTest {
            val provider = FakeProvider(granted = mutableSetOf(AppPermission.LOCATION))
            val flow = PermissionOnboardingFlow(provider)
            flow.skipAlreadyGranted()
            flow.skipCurrent() // BACKGROUND_LOCATION -> Skipped, current now NOTIFICATIONS

            // User went to system settings and enabled background location there.
            provider.granted += AppPermission.LOCATION_BACKGROUND
            flow.recheck()

            assertEquals(TierOutcome.Granted, flow.state.value.outcomes[PermissionTierId.BACKGROUND_LOCATION])
            // NOTIFICATIONS still undecided, so the ladder resumes exactly there.
            assertEquals(PermissionTierId.NOTIFICATIONS, flow.state.value.current?.id)
        }

    @Test
    fun `resume re-check completes the flow once every tier is granted`() =
        runTest {
            val provider = FakeProvider(granted = AppPermission.entries.toMutableSet())
            val flow = PermissionOnboardingFlow(provider)

            flow.recheck()

            assertTrue(flow.state.value.isComplete)
            assertTrue(flow.state.value.requiredSatisfied)
        }
}
