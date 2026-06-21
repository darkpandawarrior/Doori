package com.miletracker.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G2: generates the app's baseline profile by driving the critical cold-start path
 * **Home → Track Miles → start → stop** on a connected device or GMD. Run via
 * `./gradlew :app:generateNoGmsReleaseBaselineProfile`; the output replaces the hand-written
 * `app/src/main/baseline-profiles/baseline-prof.txt`, which `profileinstaller` then installs AOT.
 *
 * Not part of the unit-test gate — generation is inherently an instrumented/device step.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = PACKAGE_NAME) {
            // Cold start to the launcher (Home).
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // Home → Track Miles. Selectors are best-effort: the startup profile is captured regardless,
            // and on a real device these drive the journey-tracking path the profile should cover.
            device.findObject(By.descContains("Track"))?.click()
                ?: device.findObject(By.textContains("Track"))?.click()
            device.wait(Until.hasObject(By.textContains("Start")), UI_TIMEOUT)
            device.waitForIdle()

            // Start → stop a journey.
            device.findObject(By.textContains("Start"))?.click()
            device.waitForIdle()
            device.findObject(By.textContains("Stop"))?.click()
            device.waitForIdle()
        }

    private companion object {
        const val PACKAGE_NAME = "com.miletracker"
        const val UI_TIMEOUT = 5_000L
    }
}
