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

modules=$(grep -c '^include(' "$SETTINGS")
features=$(grep -c '^include(":feature:' "$SETTINGS")
cores=$(grep -c '^include(":core:' "$SETTINGS")
shots=$(find "$SHOTS_DIR" -maxdepth 1 -name '*.png' | wc -l | tr -d ' ')
db=$(grep -oE 'version = [0-9]+' "$DB_FILE" | grep -oE '[0-9]+' | head -1)

stats="<!-- AUTOGEN:stats -->
> **At a glance** — **${modules}-module** clean architecture (${features} feature · ${cores} core), Room schema **v${db}**, **${shots}** host-rendered Roborazzi screenshots (JVM, no emulator). *Numbers auto-generated from \`settings.gradle.kts\` by \`scripts/gen-readme.sh\`.*
<!-- /AUTOGEN:stats -->"

replace_block() {   # $1=tag  $2=replacement (marker lines included)
  TAG="$1" REPL="$2" perl -0777 -i -pe '
    s/<!-- AUTOGEN:\Q$ENV{TAG}\E -->.*?<!-- \/AUTOGEN:\Q$ENV{TAG}\E -->/$ENV{REPL}/s;
  ' "$README"
}

replace_block "stats" "$stats"
echo "[gen-readme] modules=$modules features=$features cores=$cores shots=$shots db=v$db"
