import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * B.2a: opt-in Compose compiler metrics + stability reports.
 *
 * Off by default (zero cost on normal builds). Pass `-Pcompose.metrics` to emit per-module composable
 * metrics (`build/compose-metrics`) and stability reports (`build/compose-reports`) for every module on the
 * Compose convention plugins — used to spot unstable parameters / unnecessary recompositions.
 *
 * Example: `./gradlew assembleNoGmsDebug -Pcompose.metrics`, then read the per-module
 * `build/compose-reports/<module>-composables.txt` stability reports.
 */
internal fun Project.configureComposeCompilerMetrics() {
    if (!providers.gradleProperty("compose.metrics").isPresent) return

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
    }
}
