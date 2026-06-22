import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for a Kotlin Multiplatform **Compose** library targeting Android + iOS.
 *
 * Builds on [MileTrackerKmpLibraryConventionPlugin] (Kotlin MPP + AGP KMP-library + iOS targets) and adds
 * the Compose Multiplatform + Compose compiler plugins. Consuming modules declare only their
 * `android { namespace/compileSdk/minSdk }` block + source sets.
 */
class MileTrackerKmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("miletracker.kmp.library")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        // B.2a: opt-in Compose compiler metrics/reports (-Pcompose.metrics).
        configureComposeCompilerMetrics()
    }
}
