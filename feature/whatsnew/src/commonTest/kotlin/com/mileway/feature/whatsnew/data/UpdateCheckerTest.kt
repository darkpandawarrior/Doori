package com.mileway.feature.whatsnew.data

import com.mileway.feature.whatsnew.model.UpdateInfo
import com.mileway.feature.whatsnew.model.UpdateUrgency
import com.siddharth.kmp.appshell.AppUpdateManager
import com.siddharth.kmp.appshell.UpdateAvailability
import com.siddharth.kmp.appshell.UpdateConfig
import com.siddharth.kmp.appshell.UpdateMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fake standing in for the real Play-Core/App-Store-backed actual injected via Koin. */
private class FakeAppUpdateManager(
    private val availability: UpdateAvailability = UpdateAvailability.NotAvailable,
) : AppUpdateManager {
    var lastConfig: UpdateConfig? = null
    var startedMode: UpdateMode? = null
    var completedFlexible = false

    override suspend fun checkForUpdate(config: UpdateConfig): UpdateAvailability {
        lastConfig = config
        return availability
    }

    override fun startUpdate(mode: UpdateMode) {
        startedMode = mode
    }

    override suspend fun completeFlexibleUpdate() {
        completedFlexible = true
    }
}

class UpdateCheckerTest {
    private val info =
        UpdateInfo(
            latestBuildCode = 100,
            minSupportedBuildCode = 50,
            latestMarketingVersion = "2026.8.36",
            releaseNotes = "Bug fixes and improvements.",
            storeUrl = "https://play.google.com/store/apps/details?id=com.mileway",
        )

    @Test
    fun `null info yields no requirement`() =
        runTest {
            val checker = UpdateChecker(FakeAppUpdateManager(), currentBuildCode = 10, info = { null })
            assertNull(checker.check())
        }

    @Test
    fun `FORCED surfaces without needing the store to confirm availability`() =
        runTest {
            val manager = FakeAppUpdateManager(availability = UpdateAvailability.NotAvailable)
            val checker = UpdateChecker(manager, currentBuildCode = 10, info = { info })
            assertEquals(UpdateUrgency.FORCED, checker.check()?.urgency)
        }

    @Test
    fun `OPTIONAL is suppressed to NONE when the store has nothing installable`() =
        runTest {
            val manager = FakeAppUpdateManager(availability = UpdateAvailability.NotAvailable)
            val checker = UpdateChecker(manager, currentBuildCode = 75, info = { info })
            assertEquals(UpdateUrgency.NONE, checker.check()?.urgency)
        }

    @Test
    fun `OPTIONAL surfaces once the store confirms an installable update`() =
        runTest {
            val manager =
                FakeAppUpdateManager(
                    availability = UpdateAvailability.Available(availableVersionCode = 100L, mode = UpdateMode.FLEXIBLE),
                )
            val checker = UpdateChecker(manager, currentBuildCode = 75, info = { info })
            assertEquals(UpdateUrgency.OPTIONAL, checker.check()?.urgency)
        }

    @Test
    fun `NONE never queries the store at all`() =
        runTest {
            val manager = FakeAppUpdateManager()
            val checker = UpdateChecker(manager, currentBuildCode = 100, info = { info })
            assertEquals(UpdateUrgency.NONE, checker.check()?.urgency)
            assertNull(manager.lastConfig)
        }

    @Test
    fun `readyToRestart reflects a Downloaded availability`() =
        runTest {
            val manager = FakeAppUpdateManager(availability = UpdateAvailability.Downloaded)
            val checker = UpdateChecker(manager, currentBuildCode = 100, info = { info })
            assertTrue(checker.readyToRestart())
        }

    @Test
    fun `readyToRestart is false otherwise`() =
        runTest {
            val checker = UpdateChecker(FakeAppUpdateManager(), currentBuildCode = 100, info = { info })
            assertFalse(checker.readyToRestart())
        }

    @Test
    fun `completeFlexibleUpdate and startUpdate delegate to the platform manager`() =
        runTest {
            val manager = FakeAppUpdateManager()
            val checker = UpdateChecker(manager, currentBuildCode = 100, info = { info })
            checker.completeFlexibleUpdate()
            checker.startUpdate(UpdateMode.FLEXIBLE)
            assertTrue(manager.completedFlexible)
            assertEquals(UpdateMode.FLEXIBLE, manager.startedMode)
        }
}
