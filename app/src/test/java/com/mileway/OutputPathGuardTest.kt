package com.mileway

import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Guards against hardcoded `/Users/<name>/...` absolute paths anywhere in repo source.
 *
 * WHY THIS EXISTS: the iOS screenshot harnesses hardcoded
 * `/Users/darkpandawarrior/Repos/Mileway/docs/screenshots` as their output-directory fallback.
 * When the repo moved to `Repos/Android/Mileway` that path went dead — but the writer called
 * `createDirectory(withIntermediateDirectories: true)`, which silently CREATED the phantom
 * `/Users/darkpandawarrior/Repos/Mileway/` folder and wrote captures into it instead of failing.
 * Zero errors, for months, and nobody happened to look in that directory.
 *
 * A literal `/Users/<name>/...` path only resolves on the one machine (and one moment, before the
 * next `mv`) it was written on. On a teammate's laptop or a CI runner it is simply wrong, and
 * "wrong" silently becomes "quietly writes somewhere nobody checks" the instant the writing code
 * also auto-creates missing directories — exactly what happened here. The fix is always an
 * environment variable or a path derived from something durable (the source file's own location,
 * the build's working directory), never a literal absolute path baked into source.
 */
class OutputPathGuardTest {

    private val absolutePathPattern = Regex("""/Users/[^"'\s]+""")
    private val sourceExtensions = setOf("kt", "kts", "swift", "java")
    private val excludedDirNames = setOf("build", ".git", "external")

    // Real repo scan: this is expected to be RED whenever a genuine violation exists anywhere in
    // source. That is correct behavior, not a broken test — see class doc. Do not silence a
    // failure here by narrowing the scan; fix (or flag) the offending file instead.
    @Test
    fun `no hardcoded slash-Users slash paths in repo source`() {
        val violations = scan(findRepoRoot()).filterNot { it.file == "app/src/test/java/com/mileway/OutputPathGuardTest.kt" }
        check(violations.isEmpty()) {
            buildString {
                appendLine("${violations.size} hardcoded /Users/ path(s) found in repo source:")
                violations.forEach { appendLine("  ${it.file}:${it.line}: ${it.text}") }
                appendLine()
                appendLine(
                    "WHY THIS FAILS THE BUILD: a literal /Users/<name>/... path resolves only on the " +
                        "machine it was written on. It breaks silently (or worse, auto-creates a " +
                        "phantom output folder and writes there unnoticed — see " +
                        "createDirectory(withIntermediateDirectories:true) in the iOS screenshot " +
                        "harnesses, the exact bug this guard exists to catch) on any other machine, " +
                        "CI runner, or after the next repo move. Use an environment variable " +
                        "(e.g. SCREENSHOT_OUT_DIR) or a path derived from something durable instead " +
                        "of a literal absolute path.",
                )
            }
        }
    }

    // Proves the detector fires correctly, entirely against a throwaway temp fixture — never
    // touches real repo files (other agents are editing this repo concurrently; this guard must
    // not risk their in-flight work to prove itself).
    @Test
    fun `flags a hardcoded slash-Users slash path in a fixture file`() {
        withTempDir { dir ->
            File(dir, "Fixture.swift").writeText(
                "let outDir = \"/Users/someone/Repos/Whatever/docs/screenshots\"\n",
            )
            val violations = scan(dir)
            check(violations.size == 1) { "expected exactly 1 violation, found: $violations" }
            check(violations.single().let { it.file == "Fixture.swift" && it.line == 1 }) { violations }
        }
    }

    @Test
    fun `does not flag a fixture file with no hardcoded path`() {
        withTempDir { dir ->
            File(dir, "Fixture.swift").writeText(
                "let outDir = ProcessInfo.processInfo.environment[\"SCREENSHOT_OUT_DIR\"] ?? \"docs/screenshots\"\n",
            )
            check(scan(dir).isEmpty())
        }
    }

    private data class Violation(val file: String, val line: Int, val text: String)

    private fun scan(root: File): List<Violation> =
        root.walkTopDown()
            .onEnter { it.name !in excludedDirNames }
            .filter { it.isFile && it.extension in sourceExtensions }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { idx, line ->
                    if (absolutePathPattern.containsMatchIn(line)) {
                        Violation(file.relativeTo(root).path, idx + 1, line.trim())
                    } else {
                        null
                    }
                }
            }
            .toList()

    private fun withTempDir(block: (File) -> Unit) {
        val dir = createTempDirectory(prefix = "output-path-guard-fixture-").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun findRepoRoot(): File {
        // Unit tests run with the module dir (app/) as the working dir under Gradle; walk up to
        // the directory that owns settings.gradle.kts rather than hardcoding a relative depth.
        val startDir = System.getProperty("user.dir")!!
        var dir = File(startDir).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("could not locate repo root walking up from $startDir")
        }
        return dir
    }
}
