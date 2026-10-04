package com.mileway.core.data.domain.claim

/**
 * Reads a golden fixture's raw text from `contract/src/commonTest/resources/golden/<name>`.
 * `:server:test`'s GoldenFixtureTest reads the same physical files via a resources.srcDir pointed
 * at this module (server/build.gradle.kts), not a copy — one committed fixture set, two consumers.
 */
expect fun readGoldenFixture(name: String): String
