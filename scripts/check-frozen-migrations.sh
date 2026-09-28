#!/usr/bin/env bash
# CI gate: MilewayDatabase versions <= 48 are frozen. core/data/schemas/.../48.json is the exported
# Room baseline (see MilewayDatabase.kt's exportSchema = true); MigrationTestHelper can only prove a
# migration is correctness-preserving if the schema it migrates FROM/TO is never retroactively
# edited after shipping. A real v48 database already exists on real devices — changing what
# `Migration(N, N+1)` with `N+1 <= 48` does after the fact silently corrupts every device that
# already ran it.
#
# Fails if the diff against the base ref touches a `Migration(N, M)` declaration line where
# `M <= 48`, inside any *Migration*.kt file. Adding a brand-new MIGRATION_48_49 (or later) is
# unaffected — only editing an already-shipped migration trips this.
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

violations=""
for f in $CHANGED_MIGRATION_FILES; do
  hits="$(git diff -U0 "$MERGE_BASE" HEAD -- "$f" \
    | grep -E '^[+-]' \
    | grep -Ev '^(\+\+\+|---)' \
    | grep -E 'Migration\([0-9]+, ?[0-9]+\)' || true)"
  [ -z "$hits" ] && continue
  while IFS= read -r line; do
    target="$(echo "$line" | grep -oE 'Migration\([0-9]+, ?[0-9]+\)' | grep -oE '[0-9]+' | tail -1)"
    if [ -n "$target" ] && [ "$target" -le "$FROZEN_CEILING" ]; then
      violations="${violations}${f}: ${line}"$'\n'
    fi
  done <<< "$hits"
done

if [ -n "$violations" ]; then
  echo "::error::Frozen-schema guard: a shipped Room migration targeting version <= ${FROZEN_CEILING} was edited. Shipped migrations are one-way-door: add a NEW Migration(${FROZEN_CEILING}, $((FROZEN_CEILING + 1))) instead of changing history." >&2
  echo "$violations" >&2
  exit 1
fi
