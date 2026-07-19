#!/usr/bin/env bash
# Pre-commit guard: blocks a commit if the staged diff matches any pattern in
# .ralph/leak-patterns.txt (gitignored, local-only — one regex per line).
# This script is tracked/public and intentionally contains no sensitive
# strings itself; see .ralph/REFERENCE.md for what the patterns protect.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
PATTERNS_FILE="$REPO_ROOT/.ralph/leak-patterns.txt"

if [ ! -f "$PATTERNS_FILE" ]; then
  exit 0
fi

pattern="$(grep -v '^\s*#' "$PATTERNS_FILE" | grep -v '^\s*$' | paste -sd'|' -)"

if [ -z "$pattern" ]; then
  exit 0
fi

hits="$(git diff --cached -U0 | grep -E '^\+' | grep -Ev '^\+\+\+' | grep -E "$pattern" || true)"

if [ -n "$hits" ]; then
  echo "commit blocked: staged changes match a locally-configured leak pattern (.ralph/leak-patterns.txt)." >&2
  echo "matched lines:" >&2
  echo "$hits" >&2
  echo "" >&2
  echo "If this file is meant to be local-only, keep it covered by .gitignore instead of force-adding it." >&2
  exit 1
fi
