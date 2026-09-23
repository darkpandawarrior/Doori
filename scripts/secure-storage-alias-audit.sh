#!/usr/bin/env bash
# PLAN_V21 P7.2: audits tracked Kotlin source for orphaned Android Keystore
# secure-storage aliases. Doori has never adopted AndroidKeyStore/
# EncryptedSharedPreferences/security-crypto (grep the version catalog — no
# such dependency exists); all "secure" persistence today is DataStore-backed
# (PinHashStore et al. hash the PIN before it's ever written). So a "dead
# alias" here means literal residue from an earlier migration, not a runtime
# concept this app currently has any live use for.
#
# ponytail: this script IS the cleanup for P7.2 — there is no real Keystore
# alias infra in this repo to reclaim, so closing the acceptance criterion
# means proving that in a repeatable way rather than inventing fake Keystore
# code just to delete it. Upgrade path: if a future task actually introduces
# AndroidKeyStore-backed storage, this script's zero-alias assumption breaks
# on purpose (grep starts matching), which is the signal to design a real
# per-alias lifecycle/reclaim story then.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Every symbol an AndroidKeyStore-backed secure-storage alias would show up
# under if one existed: KeyStore.getInstance("AndroidKeyStore"), a
# KeyGenParameterSpec.Builder(alias, ...), or a raw .setAlias(/keyAlias= call
# outside the app-signing build config (app/build.gradle.kts's release
# signingConfig keyAlias is a build-time signing concern, not runtime secure
# storage, and is excluded).
pattern='AndroidKeyStore|KeyGenParameterSpec|EncryptedSharedPreferences|MasterKey\.Builder'

hits="$(
  git ls-files -- '*.kt' '*.kts' \
    | grep -v '^app/build.gradle.kts$' \
    | xargs grep -lE "$pattern" 2>/dev/null || true
)"

if [ -n "$hits" ]; then
  echo "secure-storage-alias-audit: found Keystore-alias usage outside the reclaim baseline:" >&2
  echo "$hits" >&2
  exit 1
fi

echo "secure-storage-alias-audit: 0 orphaned Keystore aliases (no AndroidKeyStore usage in tracked source)."
