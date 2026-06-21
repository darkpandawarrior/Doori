#!/usr/bin/env bash
# Run the same checks as the GitHub Actions CI and quality gate locally.
# Mirrors the exact Gradle tasks used in .github/workflows/ci.yml and quality.yml.
set -euo pipefail

echo "==> ktlint"
./gradlew ktlintCheck

echo "==> detekt"
./gradlew detekt

echo "==> unit tests (noGms variant, same as CI)"
./gradlew testNoGmsDebugUnitTest

echo "==> kover coverage report + floor (noGms variant)"
./gradlew :app:koverXmlReportNoGmsDebugCoverage :app:koverVerifyNoGmsDebugCoverage

echo "==> dependency guard"
./gradlew dependencyGuard

echo ""
echo "All quality gates passed."
