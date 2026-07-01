import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for a Kotlin Multiplatform library targeting Android + iOS.
 *
 * Applies the Kotlin Multiplatform + AGP KMP-library plugins and declares the iOS targets, so each
 * consuming module only declares its `android { namespace/compileSdk/minSdk }` block + source sets.
 */
class MilewayKmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
        }
        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()
        }
    }
}
