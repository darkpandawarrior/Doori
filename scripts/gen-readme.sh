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

stats="<!-- AUTOGEN:stats -->
> **At a glance** — **${modules}-module** clean architecture: **${local_total} local** (${features} feature · ${cores} core) + **${composed_total} composed** via \`includeBuild(external/kmp-toolkit)\`, Room schema **v${db}**, **${shots}** host-rendered Roborazzi screenshots (JVM, no emulator). *Numbers auto-generated from \`settings.gradle.kts\` by \`scripts/gen-readme.sh\`.*
<!-- /AUTOGEN:stats -->"

replace_block() {   # $1=tag  $2=replacement (marker lines included)
  TAG="$1" REPL="$2" perl -0777 -i -pe '
    s/<!-- AUTOGEN:\Q$ENV{TAG}\E -->.*?<!-- \/AUTOGEN:\Q$ENV{TAG}\E -->/$ENV{REPL}/s;
  ' "$README"
}

replace_block "stats" "$stats"
echo "[gen-readme] total=$modules (local=$local_total: ${features}f/${cores}c + composed=$composed_total) shots=$shots db=v$db"
