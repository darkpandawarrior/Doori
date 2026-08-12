package com.mileway.core.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The mechanical half of "make the wrong thing hard".
 *
 * Documentation asks features not to hard-code colours; this makes the build say no. It counts
 * every raw `Color(0x…)` literal outside `core/ui/.../theme/` and fails if the total goes **up**.
 *
 * Deliberately a ratchet, not a ban:
 *  - migration lowers the number, so five agents sweeping in parallel never collide on it;
 *  - a new hard-coded colour raises it, and fails immediately, in whichever module added it.
 *
 * When you migrate a batch, drop [BASELINE] to the number the failure message prints. It only ever
 * goes down. Reaching 0 means the ban is real and this test can become an equality assertion.
 *
 * ponytail: a grep-based ratchet, not a detekt rule. A custom detekt rule needs type resolution and
 * its own module to be worth anything; this is 40 lines and catches the same class of regression.
 * Upgrade path: if the ratchet starts producing false positives (generated code, a legitimate
 * platform-boundary literal), promote it to a detekt `ForbiddenMethodCall` on the Color constructor
 * with a proper baseline file.
 */
class RawColorRatchetTest {
    private companion object {
        /**
         * Raw `Color(0x…)` literals outside the theme package, measured 2026-08-10 across
         * 76 files. The starting point of the migration; lower it as batches land.
         */
        const val BASELINE = 313

        /** Assembled at runtime so this file does not count as one of its own violations. */
        val NEEDLE = "Color(" + "0x"

        val EXCLUDED_PATH_FRAGMENTS =
            listOf(
                // Layer 1 lives here. Raw hexes are not only allowed, they are the point.
                "/core/ui/src/commonMain/kotlin/com/mileway/core/ui/theme/",
                // Vendored composite builds — not ours to migrate.
                "/external/",
                "/build/",
            )
    }

    @Test
    fun rawColourLiteralsOnlyEverGoDown() {
        val root = repoRoot() ?: return // Not running from a checkout; nothing to assert.

        val offenders =
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it to it.path.replace(File.separatorChar, '/') }
                .filter { (_, path) -> "/src/" in path }
                .filterNot { (_, path) -> EXCLUDED_PATH_FRAGMENTS.any { it in path } }
                .filterNot { (file, _) -> file.name == "RawColorRatchetTest.kt" }
                .mapNotNull { (file, path) ->
                    val count = file.readText().windowedCount(NEEDLE)
                    if (count > 0) path.removePrefix(root.path.replace(File.separatorChar, '/')) to count else null
                }
                .sortedByDescending { it.second }
                .toList()

        val total = offenders.sumOf { it.second }
        val worst = offenders.take(10).joinToString("\n") { "  ${it.second}\t${it.first}" }

        assertTrue(
            total <= BASELINE,
            """
            Hard-coded colours went UP: $total (baseline $BASELINE).

            A raw Color(0x…) cannot follow the active design direction, so any screen holding one
            renders identically under all ten themes. Ask for a meaning instead:
              MilewayRoles.money / distance / approved / pending / rejected / policyViolation /
              offlineQueued / activeTracking / destructive / informational / inactive / premium
            Chrome and surfaces come from MaterialTheme.colorScheme; a per-feature accent comes from
            MilewayDomainTheme(MilewayDomain.X). See core/ui/.../theme/LAYERS.md.

            Worst files:
            $worst
            """.trimIndent(),
        )

        // Report progress so a migrating agent can lower BASELINE without re-deriving the number.
        println("[raw-colour ratchet] $total literals in ${offenders.size} files (baseline $BASELINE)")
    }

    private fun String.windowedCount(needle: String): Int {
        var i = indexOf(needle)
        var n = 0
        while (i >= 0) {
            n++
            i = indexOf(needle, i + needle.length)
        }
        return n
    }

    private fun repoRoot(): File? =
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
}
