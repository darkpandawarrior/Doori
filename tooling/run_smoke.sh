#!/usr/bin/env bash
# P7.6: runs the Phase-7 device-gated smoke coverage — Maestro flows for the App Shortcuts/QS-tile
# entry points, plus an ADB-driven AppFunctions invoke/verify — against whatever device/emulator
# `adb`/`maestro` are currently pointed at (an API-36 emulator to exercise AppFunctions; any recent
# emulator for the shortcut/tile flow). Not a CI job on its own — a documented, runnable script for
# whoever has an emulator up, since neither Maestro nor `adb shell cmd app_function` is available
# in this sandboxed run environment (see PLAN_V23 P7.6 acceptance: land the flows + document).
#
# Usage: ./tooling/run_smoke.sh
set -euo pipefail

PACKAGE="com.mileway"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

require() { command -v "$1" >/dev/null 2>&1 || { echo "error: '$1' not found on PATH" >&2; exit 1; }; }

require adb
require maestro

if ! adb get-state >/dev/null 2>&1; then
  echo "error: no device/emulator attached (adb get-state failed)" >&2
  exit 1
fi

echo "== Maestro: tracking-smoke (App Shortcuts / deep link) =="
maestro test "$REPO_ROOT/maestro/android/tracking-smoke.yaml"

echo "== Maestro: appfunctions-smoke (foreground the app) =="
maestro test "$REPO_ROOT/maestro/android/appfunctions-smoke.yaml"

echo "== ADB: list registered App Functions =="
FUNCTIONS_JSON="$(adb shell cmd app_function list-app-functions)"
echo "$FUNCTIONS_JSON" | grep "$PACKAGE" >/dev/null || {
  echo "error: no App Functions registered for $PACKAGE" >&2
  echo "$FUNCTIONS_JSON" >&2
  exit 1
}

# Read the exact registered function IDs from the device rather than guessing the KSP-generated
# identifier format (it's an implementation detail of the AppFunctions compiler plugin).
find_function_id() {
  echo "$FUNCTIONS_JSON" | grep -o "\"id\"[^,}]*$1[^,}]*" | head -1 | sed -E 's/.*"id"\s*:\s*"([^"]+)".*/\1/'
}
START_ID="$(find_function_id startTrackingTrip)"
STOP_ID="$(find_function_id stopTrackingTrip)"
SUMMARY_ID="$(find_function_id getTodaySummary)"
[[ -n "$START_ID" && -n "$STOP_ID" && -n "$SUMMARY_ID" ]] || {
  echo "error: could not resolve one or more MileageAppFunctions ids from list-app-functions output" >&2
  echo "$FUNCTIONS_JSON" >&2
  exit 1
}

echo "== ADB: invoke startTrackingTrip ($START_ID) =="
adb shell cmd app_function execute-app-function \
  --package "$PACKAGE" --function "$START_ID" --parameters '{}'

echo "== ADB: invoke getTodaySummary ($SUMMARY_ID) — assert isTracking=true side effect =="
adb shell cmd app_function execute-app-function \
  --package "$PACKAGE" --function "$SUMMARY_ID" --parameters '{}' | tee /tmp/doori-today-summary.json
grep -q '"isTracking":true' /tmp/doori-today-summary.json || {
  echo "error: getTodaySummary did not reflect the startTrackingTrip side effect" >&2
  exit 1
}

echo "== ADB: invoke stopTrackingTrip ($STOP_ID) — cleanup =="
adb shell cmd app_function execute-app-function \
  --package "$PACKAGE" --function "$STOP_ID" --parameters '{}'

echo "All Phase-7 smoke checks passed."
