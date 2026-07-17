#!/usr/bin/env bash
# FLA.1 → Wave-2 §A: single-source version bump.
#
# Three files at the repo root are the source of truth:
#   VERSION       legacy semver, kept for continuity (no longer drives Gradle versionName/Code)
#   BUILD_NUMBER  legacy monotonic counter (kept for continuity, same reason)
#   MILESTONE     NEW — integer, the one field this script actually needs to bump for a release
#
# The three computed values Gradle/CI actually use (FINGERPRINT/MARKETING/BUILDCODE — see
# gradle/versioning.gradle.kts and docs/RELEASE.md) are derived from MILESTONE + the live git
# commit count + today's date. They are NEVER hand-typed and NEVER written to a file — recomputed
# fresh on every build. That's why --commit (the default) doesn't write anything: commit count is
# already live, there is nothing to bump.
#
# Usage:
#   scripts/bump_version.sh --milestone        # MILESTONE += 1 (the actual release-cut step)
#   scripts/bump_version.sh --major|--minor|--patch   # legacy VERSION/BUILD_NUMBER bump, unchanged
#   scripts/bump_version.sh [--commit]         # print the current computed values, write nothing
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION_FILE="$ROOT/VERSION"
BUILD_NUMBER_FILE="$ROOT/BUILD_NUMBER"
MILESTONE_FILE="$ROOT/MILESTONE"

[ -f "$VERSION_FILE" ] || echo "1.0.0" > "$VERSION_FILE"
[ -f "$BUILD_NUMBER_FILE" ] || echo "0" > "$BUILD_NUMBER_FILE"
[ -f "$MILESTONE_FILE" ] || echo "1" > "$MILESTONE_FILE"

print_computed() {
  local milestone commit_count fingerprint marketing build_code
  milestone="$(tr -d '[:space:]' < "$MILESTONE_FILE")"
  commit_count="$(git -C "$ROOT" rev-list --count HEAD)"
  fingerprint="$(date +%Y.%m.%V).${milestone}.${commit_count}"
  marketing="$(date +%Y.%-m).${milestone}"
  build_code=$((1 + commit_count))
  echo "MILESTONE=$milestone  FINGERPRINT=$fingerprint  MARKETING=$marketing  BUILDCODE=$build_code"
}

case "${1:---commit}" in
  --milestone)
    new_milestone=$(($(tr -d '[:space:]' < "$MILESTONE_FILE") + 1))
    echo "$new_milestone" > "$MILESTONE_FILE"
    print_computed
    ;;
  --commit)
    print_computed
    ;;
  --major|--minor|--patch)
    version="$(tr -d '[:space:]' < "$VERSION_FILE")"
    build="$(tr -d '[:space:]' < "$BUILD_NUMBER_FILE")"
    IFS='.' read -r major minor patch <<< "$version"
    case "$1" in
      --major) major=$((major + 1)); minor=0; patch=0 ;;
      --minor) minor=$((minor + 1)); patch=0 ;;
      --patch) patch=$((patch + 1)) ;;
    esac
    new_version="${major}.${minor}.${patch}"
    new_build=$((build + 1))
    echo "$new_version" > "$VERSION_FILE"
    echo "$new_build" > "$BUILD_NUMBER_FILE"
    echo "VERSION=$new_version  BUILD_NUMBER=$new_build (legacy, no longer drives Gradle versionName/Code)"
    ;;
  *)
    echo "usage: $(basename "$0") --milestone | --major|--minor|--patch | --commit" >&2
    exit 1
    ;;
esac
