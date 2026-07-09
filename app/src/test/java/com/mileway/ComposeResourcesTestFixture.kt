package com.mileway

import java.io.File

/**
 * Test-only shim so Roborazzi renders can resolve Compose Multiplatform string resources
 * (`Res.string.*`) under Robolectric.
 *
 * Background: the bleeding-edge Compose Multiplatform 1.12.0-beta01 resources Gradle plugin's
 * `CopyResourcesToAndroidAssetsTask` is broken under the current AGP/Gradle (its `outputDirectory`
 * is never configured — the task fails with "Value not set" when invoked). The upshot is that
 * `core:ui`'s generated `composeResources` (`values/strings.commonMain.cvr`, `values-hi/…`) are NOT
 * copied into the app's Android assets — not for the unit-test asset merge, and not even into the
 * real APK. So `stringResource(Res.string.*)` throws `MissingResourceException` at render time.
 * This is a genuine build regression from the dependency bump that needs a real build-level fix
 * (pin the CMP resources plugin back one step, or wait for the beta to fix the task); this shim only
 * unblocks the screenshot record so the README showcase can regenerate.
 *
 * The `DefaultAndroidResourceReader` looks up a resource in three places, in order:
 * `androidContext.assets` → the instrumentation context's assets → `classLoader.getResourceAsStream`.
 * We drop the freshly-generated `.cvr` files (they exist on disk from the resource-generator task)
 * into both a classpath-resources root (so the third lookup finds them) and the merged-assets dir
 * (so the first lookup finds them). Idempotent; a no-op if the generated sources aren't present.
 */
internal object ComposeResourcesTestFixture {
    // core:ui is the only module with `compose.resources { packageOfResClass = ... }`.
    private const val RES_NAMESPACE = "com.mileway.core.ui.resources"

    private val repoRoot: File by lazy {
        val moduleDir = File(System.getProperty("user.dir") ?: ".")
        if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
    }

    fun install() {
        val src = File(
            repoRoot,
            "core/ui/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources",
        )
        if (!src.isDirectory) return

        // Classpath-resources root the unit-test runtime classloader scans (last-resort lookup), and
        // the Robolectric merged-assets dir (first lookup). Writing to whichever exists is enough.
        val targets = listOf(
            File(
                repoRoot,
                "app/build/intermediates/java_res/noGmsDebugUnitTest/processNoGmsDebugUnitTestJavaRes/" +
                    "out/composeResources/$RES_NAMESPACE",
            ),
            File(
                repoRoot,
                "app/build/intermediates/assets/noGmsDebug/mergeNoGmsDebugAssets/composeResources/$RES_NAMESPACE",
            ),
        )
        for (target in targets) {
            if (target.exists() && File(target, "values/strings.commonMain.cvr").exists()) continue
            target.parentFile?.mkdirs()
            runCatching { src.copyRecursively(target, overwrite = true) }
        }
    }
}
