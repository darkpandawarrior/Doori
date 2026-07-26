package com.mileway.core.network.api

/**
 * PLAN_V33 A3 (+ iOS pass): flips [MilewayNetworkApi] from the offline `FakeTrackingNetworkApi` to
 * the real Ktor-backed `KtorMilewayNetworkApi` (talking to `:server`, see PLAN_V33 B1-B3). Default
 * is `false` — the app's behavior is unchanged unless something sets this before Koin resolves
 * [MilewayNetworkApi]. `:app`/iOS don't flip it yet; to opt in, set
 * `NetworkBackendFlags.useRealBackend = true` before `initKoin(...)` runs (e.g. at the top of
 * `MilewayApplication.onCreate()` or the iOS entry point, gated behind a debug build check).
 *
 * commonMain (not android-only) so the same flag/instance gates both platforms' `stubModule` — see
 * `stub/androidMain/di/StubModule.kt` and `stub/iosMain/di/StubModule.kt`.
 *
 * Lives in `:core:network`, not `:stub`, deliberately. It governs which [MilewayNetworkApi] gets
 * bound, which is this module's concern — and both readers (`:stub`'s Koin modules and
 * `feature:tracking`'s debug switch) already depend on `:core:network`. Putting it in `:stub` would
 * have forced `feature:tracking` to depend on the mock-data module just to read a flag, which is the
 * kind of edge CLAUDE.md's architecture rules exist to prevent.
 */
object NetworkBackendFlags {
    var useRealBackend: Boolean = false

    /**
     * In-memory mirror of the base URL typed into NetworkLogScreen's debug backend switch
     * (`feature/tracking/.../debug/NetworkLogViewModel.kt`). Only holds a value for the current
     * process — `DataStoreBaseUrlProvider` (the thing that actually persists a base URL through
     * app restarts) is intentionally androidMain/iosMain-only, never commonMain: core:data's
     * watchOS targets have no `com.siddharth.kmp:network` dependency, so a commonMain reference
     * would break them (see core/data/build.gradle.kts's androidMain-dependency comment). That
     * makes `DataStoreBaseUrlProvider.setBaseUrl` unreachable from this commonMain object.
     *
     * [onBaseUrlSubmitted] is the seam a platform DI module wires to make the typed URL durable:
     * e.g. `stub/androidMain/di/StubModule.kt` (and its iOS counterpart) can assign
     * `NetworkBackendFlags.onBaseUrlSubmitted = { url -> get<DataStoreBaseUrlProvider>().setBaseUrl(url) }`
     * once resolved. Until some platform module does that, this only holds the value for the
     * current process.
     */
    var baseUrlOverride: String? = null
    var onBaseUrlSubmitted: (suspend (String) -> Unit)? = null
}
