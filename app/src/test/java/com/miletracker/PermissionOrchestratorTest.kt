package com.miletracker

import com.miletracker.core.platform.AppPermission
import com.miletracker.core.platform.PermissionOrchestrator
import com.miletracker.core.platform.PermissionResult
import com.miletracker.core.platform.PermissionsProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UX.3: the permission orchestrator is pure logic over a [PermissionsProvider], so a fake provider drives it
 * end to end — already-granted skipping, sequential requesting, and the terminal allGranted / denied summary.
 */
class PermissionOrchestratorTest {

    private class FakeProvider(
        val granted: MutableSet<AppPermission> = mutableSetOf(),
        val requestResults: Map<AppPermission, PermissionResult> = emptyMap(),
        val requested: MutableList<AppPermission> = mutableListOf(),
    ) : PermissionsProvider {
        override suspend fun isGranted(permission: AppPermission): Boolean = permission in granted

        override suspend fun request(permission: AppPermission): PermissionResult {
            requested += permission
            val result = requestResults[permission] ?: PermissionResult.Denied
            if (result == PermissionResult.Granted) granted += permission
            return result
        }
    }

    private val sequence = listOf(AppPermission.LOCATION, AppPermission.NOTIFICATIONS, AppPermission.CAMERA)

    @Test
    fun `runAll grants the whole sequence when the provider grants each request`() = runTest {
        val provider = FakeProvider(requestResults = sequence.associateWith { PermissionResult.Granted })
        val orchestrator = PermissionOrchestrator(provider, sequence)

        orchestrator.runAll()

        val state = orchestrator.state.value
        assertTrue(state.isComplete)
        assertTrue(state.allGranted)
        assertEquals(sequence, provider.requested)
    }

    @Test
    fun `skipAlreadyGranted advances past granted permissions without prompting`() = runTest {
        val provider = FakeProvider(granted = mutableSetOf(AppPermission.LOCATION))
        val orchestrator = PermissionOrchestrator(provider, sequence)

        orchestrator.skipAlreadyGranted()

        // LOCATION recorded as granted (no prompt); flow now parked on NOTIFICATIONS.
        assertEquals(AppPermission.NOTIFICATIONS, orchestrator.state.value.current?.permission)
        assertTrue(provider.requested.isEmpty())
    }

    @Test
    fun `requestCurrent records a denial and advances`() = runTest {
        val provider = FakeProvider(requestResults = mapOf(AppPermission.LOCATION to PermissionResult.DeniedAlways))
        val orchestrator = PermissionOrchestrator(provider, sequence)

        val result = orchestrator.requestCurrent()

        assertEquals(PermissionResult.DeniedAlways, result)
        assertEquals(AppPermission.NOTIFICATIONS, orchestrator.state.value.current?.permission)
    }

    @Test
    fun `denied summary lists the ungranted permissions after runAll`() = runTest {
        val provider =
            FakeProvider(
                requestResults =
                    mapOf(
                        AppPermission.LOCATION to PermissionResult.Granted,
                        AppPermission.NOTIFICATIONS to PermissionResult.Denied,
                        AppPermission.CAMERA to PermissionResult.DeniedAlways,
                    ),
            )
        val orchestrator = PermissionOrchestrator(provider, sequence)

        orchestrator.runAll()

        assertFalse(orchestrator.state.value.allGranted)
        assertEquals(listOf(AppPermission.NOTIFICATIONS, AppPermission.CAMERA), orchestrator.state.value.denied)
    }
}
