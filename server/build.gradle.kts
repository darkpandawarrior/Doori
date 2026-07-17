import org.gradle.api.tasks.testing.logging.TestLogEvent

// PLAN_V33 B1: :server — a plain kotlin("jvm") Ktor backend (NOT a KMP module; nothing here
// targets Android/iOS/watchOS). Depends on exactly one Mileway project — :contract — for the
// shared wire DTOs (SubmitMilesRequestK, ExpenseSubmissionResponse, ...), so client and server
// serialize/deserialize the identical Kotlin classes. Scope for this task: skeleton + persistence
// wiring + health/echo routes only — no auth, no miles/location/vehicle routes yet (later tasks).
plugins {
    // Versionless: org.jetbrains.kotlin.jvm is already on the root classpath (kotlin-dsl in
    // build-logic/convention pulls it in), so declaring a version here trips Gradle's "already on
    // the classpath with an unknown version" duplicate-plugin check — same reason
    // :baselineprofile applies com.android.test versionless.
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.mileway.server.ApplicationKt")
}

// Wave-2 §A: stamp FINGERPRINT into a runtime resource so /version can report it — same computed
// value as :app/:wear (gradle/versioning.gradle.kts), just packaged as a resource instead of a
// BuildConfig field since this is a plain JVM app, not an Android module.
apply(from = rootProject.file("gradle/versioning.gradle.kts"))

// Read at project scope: inside the tasks.register { } lambda, `extra[...]` resolves against the
// task, not the project.
val milewayFingerprint = extra["mileway.fingerprint"] as String

val generateVersionResource =
    tasks.register("generateVersionResource") {
        val outputDir = layout.buildDirectory.dir("generated/resources/version")
        val fingerprint = milewayFingerprint
        outputs.dir(outputDir)
        doLast {
            outputDir.get().asFile.apply { mkdirs() }
                .resolve("version.properties")
                .writeText("fingerprint=$fingerprint\n")
        }
    }

sourceSets {
    main {
        resources.srcDir(generateVersionResource)
    }
}

dependencies {
    api(project(":contract"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    // PLAN_V34 P2/B2: authenticate("jwt") route guard + login/refresh JWT issuance.
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.auth.jwt.jvm)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)

    // H2 lives on the main runtime classpath (not testImplementation): the server defaults to an
    // in-memory H2 instance whenever JDBC_URL isn't set, so local/no-DB runs need it at runtime too.
    implementation(libs.h2database)
    // Real deploys point JDBC_URL at Postgres (see docker-compose.yml); driver ships alongside H2
    // so swapping the env var is the only step, no rebuild needed.
    implementation(libs.postgresql)

    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host.jvm)
}

tasks.test {
    // Tests must not need a real Postgres — they exercise the H2 in-memory default only.
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}
