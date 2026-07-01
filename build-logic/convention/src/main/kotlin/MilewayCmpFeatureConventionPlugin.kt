import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for a feature module in the CMP (Compose Multiplatform) fleet.
 *
 * Builds on [MilewayKmpComposeConventionPlugin] and pre-wires the standard dep set shared
 * by every feature: Compose runtime/UI/Material3/icons, Koin (core/compose/viewmodel), JetBrains
 * navigation-compose, lifecycle-viewmodel, and kotlinx-datetime in commonMain; plus the standard
 * androidMain complements (activity-compose, lifecycle-*-compose, koin-android, coroutines-android).
 *
 * Each feature build.gradle.kts only needs to declare:
 *   - `android { namespace / compileSdk / minSdk }`
 *   - Module-specific project() dependencies (core:ui, core:data, etc.)
 *   - Any feature-unique library deps not in this baseline
 */
class MilewayCmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("mileway.kmp.compose")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain") {
                dependencies {
                    // Compose Multiplatform baseline
                    implementation(libs.findLibrary("runtime").get())
                    implementation(libs.findLibrary("ui").get())
                    implementation(libs.findLibrary("material3").get())
                    implementation(libs.findLibrary("foundation").get())
                    implementation(libs.findLibrary("material.icons.extended").get())
                    implementation(libs.findLibrary("ui.tooling.preview.mp").get())
                    // Koin
                    implementation(libs.findLibrary("koin.core").get())
                    implementation(libs.findLibrary("koin.compose").get())
                    implementation(libs.findLibrary("koin.compose.viewmodel").get())
                    // ViewModel + Navigation + DateTime
                    implementation(libs.findLibrary("lifecycle.viewmodel").get())
                    implementation(libs.findLibrary("jb.navigation.compose").get())
                    implementation(libs.findLibrary("kotlinx.datetime").get())
                }
            }
            sourceSets.getByName("androidMain") {
                dependencies {
                    implementation(libs.findLibrary("core.ktx").get())
                    implementation(libs.findLibrary("activity.compose").get())
                    implementation(libs.findLibrary("lifecycle.viewmodel.compose").get())
                    implementation(libs.findLibrary("lifecycle.runtime.compose").get())
                    implementation(libs.findLibrary("koin.android").get())
                    implementation(libs.findLibrary("kotlinx.coroutines.android").get())
                }
            }
        }
    }
}
