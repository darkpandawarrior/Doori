package com.mileway.feature.whatsnew.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// PLAN_V36 P5 — APK bloat is the real ceiling (spec §4: ≤500KB/image, ≤2MB/GIF budget).
private const val MAX_MEDIA_BYTES = 2 * 1024 * 1024

/**
 * PLAN_V36 P5 — every [WhatsNewCatalog] media path (its `files/` prefix stripped) must exist
 * under `commonMain/composeResources/files/` and stay inside the APK-size budget.
 *
 * Lives in `androidHostTest` (plain JVM, `java.io.File` allowed) rather than `commonTest`: this
 * module has no Robolectric, so exercising the real `Res.readBytes`/`Res.getUri` resolution path
 * here throws ("`android.util.Log` is not mocked" — the stub `android.jar` used by AGP KMP host
 * tests, not a real device or Robolectric shadow). The actual runtime resolution is proven by the
 * V36.P5 gate's `unzip -l app-noGms-debug.apk | grep composeResources.*whatsnew` packaging check
 * instead; this test only guards against a typo'd/renamed catalog path.
 */
class WhatsNewMediaFilesTest {
    private val resourcesRoot = File("src/commonMain/composeResources/files")

    @Test
    fun `every catalog media path exists under composeResources files and is within budget`() {
        WhatsNewCatalog.entries.forEach { entry ->
            entry.media.forEach { media ->
                val relativePath = media.path.removePrefix("files/")
                val file = File(resourcesRoot, relativePath)
                assertTrue(file.exists(), "${entry.id}: ${media.path} does not exist at ${file.path}")
                assertTrue(
                    file.length() <= MAX_MEDIA_BYTES,
                    "${entry.id}: ${media.path} is ${file.length()} bytes, over the $MAX_MEDIA_BYTES budget",
                )
            }
        }
    }
}
