package com.mileway.feature.media.ocr

/**
 * Pure-Kotlin odometer number extractor.
 *
 * Accepts a list of raw text lines produced by any OCR engine and returns the most
 * plausible odometer reading, or null when no credible number is found.
 *
 * Design contract: NO Android or ML Kit imports, this object is runnable on the
 * plain JVM and is therefore directly unit-testable without Robolectric.
 *
 * Extraction strategy (in priority order):
 *  1. Lines that contain an odometer keyword ("odo", "odometer", "km", "miles") are
 *     searched first; the first 4-7 digit group on such a line is returned.
 *  2. If no labelled line yields a hit, scan all lines for a 4-7 digit group that is
 *     NOT a date, time, price, or pure 4-digit year.
 *  3. Reasonableness gate: reject anything outside 0..9_999_999 (a realistic
 *     vehicle odometer range in km or miles).
 *  4. When multiple candidates survive, prefer the longest digit sequence (more
 *     specific reading is more likely correct).
 *
 * OCR normalisation:
 *  - Common misread characters are substituted before regex matching.
 *    Only the digit-substitutions are applied; word-character substitutions
 *    (O→0, I→1, etc.) are applied ONLY to tokens that look like they might be
 *    numeric, to avoid corrupting real words in the raw text.
 */
object OdometerOcrParser {
    // Odometer-related labels that hint the adjacent number is a reading.
    private val ODO_LABELS = setOf("odo", "odometer", "km", "miles", "mi", "reading", "mileage")

    // Words that indicate a number is NOT an odometer (dates, times, prices, etc.).
    private val NOISE_PREFIXES =
        setOf(
            "$", "€", "£", "₹", "rs", "price", "total", "fuel",
            "litres", "liters", "ltr", "l", "gal", "gallons", "tax", "receipt", "date", "time",
            "trip a", "trip b", "trip",
        )

    // Regex: a sequence of 4 to 7 consecutive digits, optionally with comma/period as
    //        thousands separators (e.g. "12,345" or "12.345").
    //        Space is NOT treated as a separator here, "1 23 456" would not match as one group.
    //        Group 1 captures the raw token; the caller strips separators.
    private val DIGIT_GROUP = Regex("""(?<!\d)([\d,._]{4,10})(?!\d)""")

    // Reasonableness bounds for an odometer reading.
    private const val MIN_READING = 0L
    private const val MAX_READING = 9_999_999L

    /**
     * Main entry point.
     *
     * @param lines  Lines of text as returned by ML Kit (one entry per text block line).
     * @return The extracted odometer string (digits only, no separators), or null.
     */
    fun parse(lines: List<String>): String? {
        if (lines.isEmpty()) return null

        val normalised = lines.map { normaliseLine(it) }

        // Pass 1: labelled lines.
        for (line in normalised) {
            if (containsOdoLabel(line)) {
                val candidate = extractDigitGroup(line)
                if (candidate != null && isPlausible(candidate)) return candidate
            }
        }

        // Pass 2: scan all lines, collect all candidates, pick the longest plausible one.
        val allCandidates = mutableListOf<String>()
        for (line in normalised) {
            if (isNoiseLine(line)) continue
            val candidate = extractDigitGroup(line)
            if (candidate != null && isPlausible(candidate)) {
                allCandidates += candidate
            }
        }

        // Return the longest (most specific) candidate.
        return allCandidates.maxByOrNull { it.length }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Normalise common OCR misreads for characters that appear in digit sequences.
     * Applied to the entire line so that mixed strings like "0d0meter: 04821O" work.
     *
     * Strategy: replace lookalike characters when they are adjacent to at least one digit
     * (either preceded or followed by a digit). This handles:
     *   - mid-sequence:  "048O13"  → "048013"
     *   - trailing:      "04821O"  → "048210"
     *   - leading:       "O48213"  → "048213"
     */
    private fun normaliseLine(raw: String): String {
        // Collapse whitespace variants.
        var s = raw.replace(Regex("\\s+"), " ").trim()
        // Replace lookalike when preceded OR followed by a digit.
        s =
            s.replace(Regex("(?<=\\d)[Oo]|[Oo](?=\\d)"), "0")
                .replace(Regex("(?<=\\d)[Il]|[Il](?=\\d)"), "1")
                .replace(Regex("(?<=\\d)[Ss]|[Ss](?=\\d)"), "5")
                .replace(Regex("(?<=\\d)[Bb]|[Bb](?=\\d)"), "8")
                .replace(Regex("(?<=\\d)[Zz]|[Zz](?=\\d)"), "2")
        return s
    }

    private fun containsOdoLabel(line: String): Boolean {
        val lower = line.lowercase()
        return ODO_LABELS.any { lower.contains(it) }
    }

    private fun isNoiseLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        return NOISE_PREFIXES.any { lower.startsWith(it) || lower.contains(it) }
    }

    /**
     * Extract the most prominent digit group from [line].
     *
     * Strips thousands separators (comma / period / underscore) and returns a clean
     * digits-only string if the result is 4-7 digits long.
     */
    private fun extractDigitGroup(line: String): String? {
        val matches =
            DIGIT_GROUP.findAll(line)
                .map { it.groupValues[1].replace(Regex("[,._]"), "") }
                .filter { it.length in 4..7 && it.all { c -> c.isDigit() } }
                .toList()
        // Prefer the longest match on this line.
        return matches.maxByOrNull { it.length }
    }

    /**
     * Gate: is the parsed number within plausible odometer range?
     */
    private fun isPlausible(digits: String): Boolean {
        val value = digits.toLongOrNull() ?: return false
        return value in MIN_READING..MAX_READING
    }
}
