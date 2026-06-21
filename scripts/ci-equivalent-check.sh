#!/usr/bin/env bash
# Run the same checks as the GitHub Actions quality gate locally.
set -euo pipefail

echo "==> ktlint"
./gradlew ktlintCheck

echo "==> detekt"
./gradlew detekt

echo "==> unit tests"
./gradlew testDebugUnitTest

echo "==> kover coverage report"
./gradlew koverXmlReport

echo ""
echo "All quality gates passed."
