#!/usr/bin/env bash
# FLA.1 — single-source version bump (biciradar-style).
#
# VERSION holds the semantic versionName; BUILD_NUMBER is a monotonically increasing counter.
# The Gradle versionCode is computed as VERSION_CODE_BASE + BUILD_NUMBER (see app/build.gradle.kts),
# so this script is the ONE place versions change.
#
# Usage: scripts/bump_version.sh --major|--minor|--patch
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION_FILE="$ROOT/VERSION"
BUILD_NUMBER_FILE="$ROOT/BUILD_NUMBER"

[ -f "$VERSION_FILE" ] || echo "1.0.0" > "$VERSION_FILE"
[ -f "$BUILD_NUMBER_FILE" ] || echo "0" > "$BUILD_NUMBER_FILE"

version="$(tr -d '[:space:]' < "$VERSION_FILE")"
build="$(tr -d '[:space:]' < "$BUILD_NUMBER_FILE")"
IFS='.' read -r major minor patch <<< "$version"

case "${1:-}" in
  --major) major=$((major + 1)); minor=0; patch=0 ;;
  --minor) minor=$((minor + 1)); patch=0 ;;
  --patch) patch=$((patch + 1)) ;;
  *) echo "usage: $(basename "$0") --major|--minor|--patch" >&2; exit 1 ;;
esac

new_version="${major}.${minor}.${patch}"
new_build=$((build + 1))

echo "$new_version" > "$VERSION_FILE"
echo "$new_build" > "$BUILD_NUMBER_FILE"

# versionCode base is mirrored in app/build.gradle.kts (VERSION_CODE_BASE).
echo "VERSION=$new_version  BUILD_NUMBER=$new_build  (versionCode = 1 + $new_build = $((1 + new_build)))"
