pluginManagement {
    includeBuild("build-logic")
    includeBuild("external/kmp-build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Storytale (multiplatform component gallery) publishes dev builds here, not yet on Maven Central.
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS (was FAIL_ON_PROJECT_REPOS): the Kotlin/Wasm toolchain adds Node/Yarn/Binaryen
    // distribution repos at project level for :app-web-preview's wasmJs target; FAIL would reject
    // them. Same rationale + ivy blocks as external/kmp-toolkit/settings.gradle.kts.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Storytale runtime artifacts (org.jetbrains.compose.storytale) resolve from JetBrains Space.
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        // Node.js / Yarn / Binaryen distributions for the Kotlin/Wasm toolchain (wasmJs browser()).
        ivy {
            name = "Node.js Distributions"
            url = uri("https://nodejs.org/dist")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy {
            name = "Yarn Distributions"
            url = uri("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy {
            name = "Binaryen Distributions"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "Mileway"

includeBuild("external/kmp-toolkit") {
    dependencySubstitution {
        substitute(module("com.siddharth.kmp:location")).using(project(":location"))
        substitute(module("com.siddharth.kmp:common")).using(project(":common"))
        substitute(module("com.siddharth.kmp:offline-outbox")).using(project(":offline-outbox"))
        // PLAN_V33 A3: KtorMilewayNetworkApi's HttpClientFactory (BaseUrlProvider/createHttpClient).
        substitute(module("com.siddharth.kmp:network")).using(project(":network"))
        substitute(module("com.siddharth.kmp:mvi-core")).using(project(":mvi-core"))
        substitute(module("com.siddharth.kmp:result")).using(project(":result"))
        substitute(module("com.siddharth.kmp:app-shell")).using(project(":app-shell"))
        substitute(module("com.siddharth.kmp:ai")).using(project(":ai"))
        substitute(module("com.siddharth.kmp:security")).using(project(":security"))
        // PLAN_V34 P2/A6: AuthTokenStore's refresh-token persistence (SecureSettingsFactory).
        substitute(module("com.siddharth.kmp:settings")).using(project(":settings"))
    }
}

include(":app")
include(":contract")
include(":server")
include(":shared")
include(":sharedWatch")
include(":core:ui")
include(":core:maps")
include(":core:maps-krossmap")
include(":core:maps-maplibre")
include(":core:common")
include(":core:media")
include(":core:ai")
include(":core:forms")
include(":core:platform")
include(":core:data")
include(":core:network")
include(":core:security")
include(":feature:tracking")
include(":feature:logging")
include(":feature:media")
include(":feature:profile")
include(":feature:approvals")
include(":feature:payables")
include(":feature:travel")
include(":feature:agent")
include(":feature:cards")
include(":feature:advances")
include(":feature:payments")
include(":feature:events")
include(":feature:whatsnew")
include(":stub")
include(":wear")
include(":baselineprofile")
include(":widget")
include(":desktopApp")
include(":app-web-preview")
