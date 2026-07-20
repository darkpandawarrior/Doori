# `:app-web-preview` — wasmJs preview shell

A browser-playable Mileway demo for the portfolio site, embedded the same way Kursi's web build
is (an iframe over a static folder). Not a product target — a curated three-screen preview
(dashboard, live tracking, expense log) running entirely in-memory in the browser.

## Why a separate shell (and not the real app compiled to wasm)

Room KMP publishes no wasm target, so `:core:data` — and everything above it (`:core:ui`, every
feature module) — can never compile to wasmJs. Two consequences:

- **The design system is reused at source level.** `core/ui`'s theme package (DesignTokens,
  MilewayTheme, MilewaySemanticColors, typography — dependency-clean: Compose + MaterialKolor
  only) is compiled directly into this module via a `srcDir` + explicit file allowlist in
  `build.gradle.kts`. Same tokens, same curated theme variants, zero duplication.
- **The data layer is faked, not reused.** Mileway's repositories are concrete Room-DAO-backed
  classes (there is no repository-interface seam today), so this module ships small in-memory
  stand-ins (`DemoEngine.kt`) that mirror the real shapes: a deterministic port of
  `feature:tracking`'s `SimulatedLocationSource` (seeded RNG + synthetic clock) feeding the
  *production* `KalmanSmoother` from `com.siddharth.kmp:location` (already wasm-enabled), and an
  expense store with the real Draft → Submitted → Approved lifecycle.

## Build

```bash
./gradlew :app-web-preview:wasmJsBrowserDistribution
# output: app-web-preview/build/dist/wasmJs/productionExecutable/
```

Quick local check (wasm needs an HTTP server, file:// won't work):

```bash
python3 -m http.server 8080 -d app-web-preview/build/dist/wasmJs/productionExecutable
# open http://localhost:8080
```

## Embedding in cv-siddharth

Copy the dist folder into the portfolio repo (no sync automation exists — same manual flow as
`public/kursi-app/`):

```bash
cp -r app-web-preview/build/dist/wasmJs/productionExecutable/. \
      ../../Interview/cv-siddharth/public/mileway-app/
```

Then in cv-siddharth:

1. Point the Mileway project's Web platform entry in `src/data/profile.ts` at
   `liveUrl: "/mileway-app/index.html"` (`mileway` is already in `LIVE_WEB_PROJECTS`).
2. Add a `vercel.json` header block mirroring the Kursi one so the ~13 MB of wasm gets
   `Cache-Control: immutable`: source `/mileway-app/(.*)\.wasm`.

The shipped `index.html` already satisfies `LiveEmbed`'s paint-detection contract: it tags the
Compose-injected canvas with `id="ComposeTarget"` once it appears, which is what the portfolio
polls to fade the iframe in.
