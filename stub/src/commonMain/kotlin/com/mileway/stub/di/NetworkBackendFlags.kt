package com.mileway.stub.di

import com.mileway.core.network.api.MilewayNetworkApi

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
 */
object NetworkBackendFlags {
    var useRealBackend: Boolean = false
}
