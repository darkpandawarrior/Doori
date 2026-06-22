import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * H.8: convention plugin bundling the generic JVM unit-test stack so modules stop hand-rolling the same
 * `testImplementation(...)` list. Adds JUnit, MockK, coroutines-test, Turbine, and Koin-test to the
 * `testImplementation` configuration. Module-/screenshot-specific extras (Robolectric, Roborazzi, Room
 * testing, Compose UI test, Glance testing, etc.) stay in the consuming module's own build file.
 *
 * Apply with `id("miletracker.test")` on any module that has a `src/test` JVM unit-test source set.
 */
class MileTrackerTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun testImpl(alias: String) =
            libs.findLibrary(alias).ifPresent { lib ->
                dependencies { add("testImplementation", lib.get()) }
            }

        testImpl("junit")
        testImpl("mockk")
        testImpl("kotlinx-coroutines-test")
        testImpl("turbine")
        testImpl("koin-test")
        testImpl("koin-test-junit4")
    }
}
