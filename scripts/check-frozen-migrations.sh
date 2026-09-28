#!/usr/bin/env bash
# CI gate: MilewayDatabase versions <= 48 are frozen. core/data/schemas/.../48.json is the exported
# Room baseline (see MilewayDatabase.kt's exportSchema = true); MigrationTestHelper can only prove a
# migration is correctness-preserving if the schema it migrates FROM/TO is never retroactively
# edited after shipping. A real v48 database already exists on real devices, so editing what a
# shipped `Migration(N, N+1)` with `N+1 <= 48` does after the fact silently corrupts every device
# that already ran it.
#
# Fails if any changed line (inside a *Migration*.kt file, diffed against the base ref) falls
# *inside* a `Migration(N, M)` block where `M <= 48` -- not just a literal edit of the declaration
# line itself, since editing the migrate() body without touching the `Migration(N, M)` line is the
# same one-way-door violation. Adding a brand-new MIGRATION_48_49 (or later) is unaffected.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

FROZEN_CEILING=48
BASE_REF="${1:-origin/main}"

if ! git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  echo "frozen-migration guard: base ref '$BASE_REF' not found in this checkout, skipping (nothing to diff against)."
  exit 0
fi

MERGE_BASE="$(git merge-base "$BASE_REF" HEAD 2>/dev/null || echo "$BASE_REF")"

CHANGED_MIGRATION_FILES="$(git diff --name-only "$MERGE_BASE" HEAD -- '*Migration*.kt' || true)"

if [ -z "$CHANGED_MIGRATION_FILES" ]; then
  exit 0
fi

# Given a blob file, print one "<lineNumber> <targetVersionOrNONE>" per line, tracking each
# `object : Migration(N, M) { ... }` block's actual extent (opened by the Migration(...) line,
# closed by its own object-closing brace, a bare 4-space-indented "    }" -- distinct from the
# 8-space-indented "        }" that closes migrate()'s function body one line earlier). A line
# textually AFTER the last migration's closing brace (e.g. a brand-new migration appended at EOF)
# must read NONE, not "still inside the previous block" -- that was the false-positive this fixes.
migration_line_map() {
  awk '
    {
      line = $0
      if (line ~ /object[ \t]*:[ \t]*Migration\([0-9]+, ?[0-9]+\)/) {
        s = line
        sub(/.*Migration\(/, "", s)
        sub(/\).*/, "", s)
        gsub(/[ \t]/, "", s)
        split(s, parts, ",")
        cur = parts[2]
        active = 1
      }
      print NR, (active == 1 ? cur : "NONE")
      if (active == 1 && line ~ /^    \}[ \t]*$/) {
        active = 0
      }
    }
  ' "$1"
}

# Given a precomputed line map (from migration_line_map) and a 1-based line number, print the
# target version that line falls inside, or nothing if it's outside any migration block.
enclosing_target() {
  local map_file="$1" line="$2"
  awk -v n="$line" '$1 == n && $2 != "NONE" { print $2 }' "$map_file"
}

violations=""
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

for f in $CHANGED_MIGRATION_FILES; do
  old_blob="$tmpdir/old"
  new_blob="$tmpdir/new"
  old_map="$tmpdir/old.map"
  new_map="$tmpdir/new.map"
  git show "$MERGE_BASE:$f" > "$old_blob" 2>/dev/null || : > "$old_blob"
  git show "HEAD:$f" > "$new_blob" 2>/dev/null || : > "$new_blob"
  migration_line_map "$old_blob" > "$old_map"
  migration_line_map "$new_blob" > "$new_map"

  while IFS= read -r hunk; do
    # e.g. "@@ -14,0 +15,2 @@" -> old_start=14 old_count=0 new_start=15 new_count=2
    old_range="$(echo "$hunk" | { grep -oE -- '-[0-9]+(,[0-9]+)?' || true; } | head -1)"
    new_range="$(echo "$hunk" | { grep -oE -- '\+[0-9]+(,[0-9]+)?' || true; } | head -1)"
    old_start="${old_range#-}"; old_start="${old_start%%,*}"
    old_count="$(echo "$old_range" | { grep -oE ',[0-9]+' || true; } | tr -d ',')"; old_count="${old_count:-1}"
    new_start="${new_range#+}"; new_start="${new_start%%,*}"
    new_count="$(echo "$new_range" | { grep -oE ',[0-9]+' || true; } | tr -d ',')"; new_count="${new_count:-1}"

    target=""
    if [ "$old_count" != "0" ] && [ -s "$old_blob" ]; then
      target="$(enclosing_target "$old_map" "$old_start")"
    fi
    if { [ -z "$target" ] || [ "$target" -gt "$FROZEN_CEILING" ] 2>/dev/null; } && [ "$new_count" != "0" ] && [ -s "$new_blob" ]; then
      new_target="$(enclosing_target "$new_map" "$new_start")"
      [ -n "$new_target" ] && target="$new_target"
    fi

    if [ -n "$target" ] && [ "$target" -le "$FROZEN_CEILING" ] 2>/dev/null; then
      violations="${violations}${f} (hunk ${hunk}) -- inside Migration targeting v${target}"$'\n'
    fi
  done <<< "$(git diff -U0 "$MERGE_BASE" HEAD -- "$f" | grep -E '^@@')"
done

if [ -n "$violations" ]; then
  echo "::error::Frozen-schema guard: a shipped Room migration targeting version <= ${FROZEN_CEILING} was edited. Shipped migrations are one-way-door: add a NEW Migration(${FROZEN_CEILING}, $((FROZEN_CEILING + 1))) instead of changing history." >&2
  echo "$violations" >&2
  exit 1
fi
