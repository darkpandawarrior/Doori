import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for an Android (non-KMP) Compose library module.
 *
 * Applies the AGP library + Compose-compiler plugins and the shared android config (compileSdk 37,
 * minSdk 30, Java 11, Compose enabled). Consuming modules declare only `android { namespace = "..." }`
 * + their dependencies. Used by the `:feature:*` modules until they move to KMP in Phase 3.
 */
class MilewayAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        // B.2a: opt-in Compose compiler metrics/reports (-Pcompose.metrics).
        configureComposeCompilerMetrics()
        extensions.configure<LibraryExtension> {
            compileSdk = 37
            defaultConfig { minSdk = 30 }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            buildFeatures {
                compose = true
                buildConfig = false  // libraries almost never need BuildConfig
            }
            testOptions {
                // AGP 9.5.0-alpha06 registers generate<Variant>ComposePreviewRunfiles for every
                // module with compose = true, and that registration hard-fails on AGP's default
                // isIncludeAndroidResources = false, because Compose Preview needs compiled Android
                // resources to resolve layout XML and theme attributes. The shared
                // SharedAndroidLibraryConventionPlugin already carries this; the three modules on
                // THIS local plugin bypass it, which is how :core:maps-krossmap still failed.
                // NOTE: a behaviour change - unit tests in these modules now run against compiled
                // Android resources.
                unitTests {
                    isIncludeAndroidResources = true
                }
            }
        }
    }
}
