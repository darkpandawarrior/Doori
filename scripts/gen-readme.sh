#!/usr/bin/env bash
# Regenerates ONLY the <!-- AUTOGEN:x --> … <!-- /AUTOGEN:x --> spans in README.md from
# source-of-truth in code. Hand-written prose outside the markers is never touched.
# Run locally (./scripts/gen-readme.sh) or in CI; edits README.md in place.
# ponytail: awk block-replace over a couple of markers — no templating engine.
set -euo pipefail
cd "$(dirname "$0")/.."

README="README.md"
SETTINGS="settings.gradle.kts"
DB_FILE="core/data/src/commonMain/kotlin/com/mileway/core/data/database/MilewayDatabase.kt"
SHOTS_DIR="docs/screenshots"

# grep -c exits 1 (not 0) when a pattern matches zero lines — under `set -e` that aborts the whole
# script. `|| true` keeps the "0" grep already prints on stdout while swallowing the non-zero exit.

# --- local modules: `include(...)` in this repo's settings ---
local_total=$(grep -c '^include(' "$SETTINGS" || true)
features=$(grep -c '^include(":feature:' "$SETTINGS" || true)
cores=$(grep -c '^include(":core:' "$SETTINGS" || true)

# --- composed modules: substituted from includeBuild(external/kmp-toolkit) ---
# Each `substitute(module("com.siddharth.kmp:X")).using(project(...))` is one composed module
# (location/common/network/mvi-core/... — shared toolkit libs, not part of Mileway's own tree).
composed_total=$(grep -cE 'substitute\(module\("com\.siddharth\.kmp:' "$SETTINGS" || true)

modules=$(( local_total + composed_total ))
shots=$(find "$SHOTS_DIR" -maxdepth 1 -name '*.png' | wc -l | tr -d ' ')
db=$(grep -oE 'version = [0-9]+' "$DB_FILE" | grep -oE '[0-9]+' | head -1)

# --- toolchain badges: read from the version catalog + wrapper, never hand-typed ---
# These drifted before this block existed: the README advertised Kotlin 2.4.20-RC and Compose
# Multiplatform 1.12.0-rc01 while the catalog said 2.4.20 and 1.13.0-alpha01. A badge is a claim
# about the code rendered in the most authoritative-looking way markdown offers, so it is the last
# thing that should be maintained by hand. readme.yml already fails a PR when regenerating produces
# a diff, which turns this into a gate for free.
CATALOG="gradle/libs.versions.toml"
catalog_version() { grep -E "^$1 = " "$CATALOG" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
# shields.io escaping: a literal '-' is written '--', a literal space '%20'.
shield() { printf '%s' "$1" | sed -e 's/-/--/g' -e 's/ /%20/g'; }

kotlin_v=$(catalog_version kotlin)
cmp_v=$(catalog_version compose-multiplatform)
agp_v=$(catalog_version agp)
gradle_v=$(grep -oE 'gradle-[0-9][^-]*(-[a-z0-9.-]+)?-bin' gradle/wrapper/gradle-wrapper.properties | head -1 | sed -e 's/^gradle-//' -e 's/-bin$//')

badges="<!-- AUTOGEN:badges -->
![Kotlin](https://img.shields.io/badge/Kotlin-$(shield "$kotlin_v")-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-$(shield "$cmp_v")-4285F4?logo=jetpackcompose&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-$(shield "$agp_v")-3DDC84?logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-$(shield "$gradle_v")-02303A?logo=gradle&logoColor=white)
<!-- /AUTOGEN:badges -->"

stats="<!-- AUTOGEN:stats -->
> **At a glance**, **${modules}-module** clean architecture: **${local_total} local** (${features} feature · ${cores} core) + **${composed_total} composed** via \`includeBuild(external/kmp-toolkit)\`, Room schema **v${db}**, **${shots}** host-rendered Roborazzi screenshots (JVM, no emulator). *Numbers auto-generated from \`settings.gradle.kts\` by \`scripts/gen-readme.sh\`.*
<!-- /AUTOGEN:stats -->"

replace_block() {   # $1=tag  $2=replacement (marker lines included)
  TAG="$1" REPL="$2" perl -0777 -i -pe '
    s/<!-- AUTOGEN:\Q$ENV{TAG}\E -->.*?<!-- \/AUTOGEN:\Q$ENV{TAG}\E -->/$ENV{REPL}/s;
  ' "$README"
}

replace_block "stats" "$stats"
replace_block "badges" "$badges"
echo "[gen-readme] total=$modules (local=$local_total: ${features}f/${cores}c + composed=$composed_total) shots=$shots db=v$db"
echo "[gen-readme] badges: kotlin=$kotlin_v cmp=$cmp_v agp=$agp_v gradle=$gradle_v"
