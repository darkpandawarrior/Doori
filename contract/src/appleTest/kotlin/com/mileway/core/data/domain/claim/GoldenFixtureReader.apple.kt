package com.mileway.core.data.domain.claim

// ponytail: no CI task test-compiles or runs commonTest on a native (ios/watchos) target today —
// ios.yml only compiles :shared/:sharedWatch main sources, and testAndroidHostTest only covers the
// android+desktop hostTest targets. Wire a real NSBundle/embedded-resource read here if a native
// test run is ever added to CI; until then this satisfies the expect/actual contract so `./gradlew
// build`/`allTests` still link, without pretending native fixture-reading is proven.
actual fun readGoldenFixture(name: String): String =
    throw NotImplementedError("Golden fixtures are not read on native targets (no CI test run there yet): $name")
