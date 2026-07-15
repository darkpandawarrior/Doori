package com.mileway.core.platform

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CF.4: the telemetry kill switch — [NapierCrashReporter.setEnabled] must drop every call while
 * disabled and resume passing them through once re-enabled. A recording [Antilog] stands in for the
 * real Napier backend so the assertions don't depend on log output formatting.
 *
 * The [LoggingAnalyticsHelper] half of this kill-switch test moved with it to
 * `com.siddharth.kmp.appshell.LoggingAnalyticsHelperTest` (:app-shell) — that class no longer lives here.
 */
class TelemetryKillSwitchTest {
    private class RecordingAntilog : Antilog() {
        val messages = mutableListOf<String>()

        override fun performLog(
            priority: LogLevel,
            tag: String?,
            throwable: Throwable?,
            message: String?,
        ) {
            message?.let { messages.add(it) }
        }
    }

    private val recorder = RecordingAntilog()

    @BeforeTest
    fun setUp() {
        Napier.base(recorder)
    }

    @AfterTest
    fun tearDown() {
        Napier.takeLogarithm()
    }

    @Test
    fun disabled_crash_reporter_drops_breadcrumbs_and_exceptions() {
        val reporter = NapierCrashReporter()
        reporter.setEnabled(false)

        reporter.log("breadcrumb")
        reporter.recordException(IllegalStateException("boom"))
        reporter.setCustomKey("k", "v")

        assertTrue(recorder.messages.isEmpty())
    }

    @Test
    fun enabled_crash_reporter_passes_breadcrumbs_through() {
        val reporter = NapierCrashReporter()

        reporter.log("breadcrumb")

        assertEquals(1, recorder.messages.size)
        assertTrue(recorder.messages.single().contains("breadcrumb"))
    }
}
