import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the Android application module.
 *
 * Applies the AGP application + Compose-compiler plugins and the shared android config (compileSdk 37,
 * Java 21, Compose enabled). App-specific config (applicationId, minSdk/targetSdk, version, buildTypes,
 * testOptions) stays in the app's own `android { }` block.
 */
class MileTrackerAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        // B.2a: opt-in Compose compiler metrics/reports (-Pcompose.metrics).
        configureComposeCompilerMetrics()
        extensions.configure<ApplicationExtension> {
            compileSdk = 37
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            buildFeatures {
                compose = true
                // BuildConfig disabled by default via gradle.properties; app can opt back in.
                buildConfig = false
            }
        }
    }
}
