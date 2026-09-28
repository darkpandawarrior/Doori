package com.mileway.core.data.domain.claim

actual fun readGoldenFixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/golden/$name")) { "Missing golden fixture: $name" }
        .bufferedReader()
        .readText()
