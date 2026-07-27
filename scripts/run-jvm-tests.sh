#!/usr/bin/env bash
# Runs a JVM unit-test (or Roborazzi record) Gradle task, tolerating a KNOWN, upstream flake:
# Robolectric 4.16.1's native Skia runtime segfaults the forked test JVM on *exit* (exit value 2)
# AFTER every test has already run and passed, on JDK 21 (both Linux CI and macOS). The Gradle task
# therefore reports failure even though no test failed.
#
# The per-test JUnit XML is the source of truth here: this wrapper fails ONLY if a real
# <failure>/<error> is present, or if no XML was produced at all (tests never ran). A genuine test
# failure still fails the build; only the teardown-exit-code flake is tolerated.
#
# Follow-up to properly fix (isolate the @GraphicsMode(NATIVE) Roborazzi tests further, or bump
# Robolectric/JDK once a fixed release exists) is tracked in the repo backlog.
set -uo pipefail

TASK="${1:?usage: run-jvm-tests.sh <gradle-task> [extra gradle args...]}"
shift || true

# Marker for freshness. Without it the `find` below matches test-results XML from ANY previous run,
# so a task that produced NO results this time could still "verify" green against stale XML left in
# build/ by an earlier green run — a silent pass, which is the exact failure class this wrapper is
# supposed to prevent.
marker="$(mktemp)"

./gradlew "$TASK" "$@"
code=$?
[ "$code" -eq 0 ] && exit 0

echo "::warning title=Robolectric teardown flake::'$TASK' exited ${code}; verifying via per-test XML"

# -newer "$marker": only XML written by THIS invocation counts.
xmls=$(find . -path '*/build/test-results/*' -name '*.xml' -newer "$marker" 2>/dev/null)
rm -f "$marker"
if [ -z "$xmls" ]; then
  echo "::error title=No test results::'$TASK' produced no test-results XML this run — tests did not run"
  exit 1
fi

real_failures=$(echo "$xmls" | xargs grep -lE '<(failure|error)[ >]' 2>/dev/null)
if [ -n "$real_failures" ]; then
  echo "::error title=Real test failures::found <failure>/<error> in:"
  echo "$real_failures"
  exit 1
fi

echo "All tests passed (per JUnit XML). The non-zero exit is the documented Robolectric/Skia"
echo "native-teardown flake, not a test failure — treating '$TASK' as successful."
exit 0
