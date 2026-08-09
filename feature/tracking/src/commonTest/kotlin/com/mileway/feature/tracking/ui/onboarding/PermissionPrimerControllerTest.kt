package com.mileway.feature.tracking.ui.onboarding

import com.mileway.core.platform.PermissionTierId
import com.siddharth.kmp.appshell.AppPermission
import com.siddharth.kmp.appshell.PermissionResult
import com.siddharth.kmp.appshell.PermissionsProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Pure state-machine coverage for [PermissionPrimerController]: permission-state derivation (the five
 * honest [PrimerOutcome]s), the required-then-optional request order, and — the requirement this task
 * called out by name — that a permanently-denied required permission routes to Settings instead of being
 * re-requested. All against a fake [PermissionsProvider], no platform/Compose types.
 */
class PermissionPrimerControllerTest {
    private class FakeProvider(
        val granted: MutableSet<AppPermission> = mutableSetOf(),
        val requestResults: MutableMap<AppPermission, PermissionResult> = mutableMapOf(),
    ) : PermissionsProvider {
        val requestCalls = mutableListOf<AppPermission>()

        override suspend fun isGranted(permission: AppPermission): Boolean = permission in granted

        override suspend fun request(permission: AppPermission): PermissionResult {
            requestCalls += permission
            val result = requestResults[permission] ?: PermissionResult.Denied
            if (result == PermissionResult.Granted) granted += permission
            return result
        }
    }

    @Test
    fun `intro then required tier before any optional tier - the correct request order`() =
        runTest {
            val provider = FakeProvider(requestResults = mutableMapOf(AppPermission.LOCATION to PermissionResult.Granted))
            val controller = PermissionPrimerController(provider)

            controller.start()
            assertEquals(PrimerStage.Intro, controller.stage.value)

            controller.beginRequesting()
            val firstAsk = assertIs<PrimerStage.Requesting>(controller.stage.value)
            assertEquals(PermissionTierId.LOCATION_FINE, firstAsk.tier.id)

            controller.requestCurrent()
            val secondAsk = assertIs<PrimerStage.Requesting>(controller.stage.value)
            assertEquals(PermissionTierId.BACKGROUND_LOCATION, secondAsk.tier.id)
        }

    @Test
    fun `granting every tier including background classifies FullyGranted`() =
        runTest {
            val provider =
                FakeProvider(
                    requestResults =
                        mapOf(
                            AppPermission.LOCATION to PermissionResult.Granted,
                            AppPermission.LOCATION_BACKGROUND to PermissionResult.Granted,
                            AppPermission.NOTIFICATIONS to PermissionResult.Granted,
                            AppPermission.ACTIVITY_RECOGNITION to PermissionResult.Granted,
                        ).toMutableMap(),
                )
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()
            repeat(4) { controller.requestCurrent() } // walk all four tiers

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.FullyGranted, done.outcome)
        }

    @Test
    fun `required granted but background skipped classifies ForegroundOnly`() =
        runTest {
            val provider = FakeProvider(requestResults = mutableMapOf(AppPermission.LOCATION to PermissionResult.Granted))
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()
            controller.requestCurrent() // LOCATION_FINE -> Granted, now on BACKGROUND_LOCATION
            controller.skipCurrent() // BACKGROUND_LOCATION -> Skipped, now on NOTIFICATIONS
            controller.skipCurrent() // NOTIFICATIONS -> Skipped
            controller.skipCurrent() // ACTIVITY_RECOGNITION -> Skipped

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.ForegroundOnly, done.outcome)
        }

    @Test
    fun `single denial of the required tier classifies Denied, not PermanentlyDenied`() =
        runTest {
            val provider = FakeProvider() // no requestResults entry -> defaults to Denied
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()

            controller.requestCurrent()

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.Denied, done.outcome)
            assertEquals(1, provider.requestCalls.count { it == AppPermission.LOCATION })
        }

    @Test
    fun `second consecutive denial routes to Settings instead of asking a third time`() =
        runTest {
            val provider = FakeProvider() // always denies
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()

            controller.requestCurrent() // 1st denial -> Done(Denied)
            controller.retry() // user taps "Try Again" -> fresh ladder, denial count preserved
            assertIs<PrimerStage.Requesting>(controller.stage.value)
            controller.requestCurrent() // 2nd denial -> Done(PermanentlyDenied)

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.PermanentlyDenied, done.outcome)
            // Exactly two real system-dialog calls — a third would mean the primer is looping a
            // prompt the OS will silently no-op (or worse, that never appears at all).
            assertEquals(2, provider.requestCalls.count { it == AppPermission.LOCATION })
        }

    @Test
    fun `recheckAfterSettings without an actual grant stays on the same denied outcome, not the next tier`() =
        runTest {
            val provider = FakeProvider() // always denies, and Settings visit changes nothing
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()
            controller.requestCurrent() // Done(Denied)

            controller.recheckAfterSettings()

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.Denied, done.outcome)
        }

    @Test
    fun `recheckAfterSettings resumes the ladder once the required permission was actually granted in Settings`() =
        runTest {
            val provider = FakeProvider()
            val controller = PermissionPrimerController(provider)
            controller.start()
            controller.beginRequesting()
            controller.requestCurrent() // 1st denial -> Done(Denied)

            provider.granted += AppPermission.LOCATION // user granted it from system Settings
            controller.recheckAfterSettings()

            val next = assertIs<PrimerStage.Requesting>(controller.stage.value)
            assertEquals(PermissionTierId.BACKGROUND_LOCATION, next.tier.id)
        }

    @Test
    fun `policy-restricted overrides denial classification`() =
        runTest {
            val provider = FakeProvider()
            val controller = PermissionPrimerController(provider, isPolicyRestricted = { true })
            controller.start()
            controller.beginRequesting()

            controller.requestCurrent()

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.RestrictedByPolicy, done.outcome)
        }

    @Test
    fun `start skips straight to Done when everything is already granted, with no Intro shown`() =
        runTest {
            val provider = FakeProvider(granted = AppPermission.entries.toMutableSet())
            val controller = PermissionPrimerController(provider)

            controller.start()

            val done = assertIs<PrimerStage.Done>(controller.stage.value)
            assertEquals(PrimerOutcome.FullyGranted, done.outcome)
        }
}
