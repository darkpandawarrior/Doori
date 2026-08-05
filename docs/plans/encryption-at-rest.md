# Plan: encryption at rest for `mileway.db`

**Status:** open, unimplemented. Draft PR — do not merge as-is; the code is not written.
**Opened:** 2026-08-01, from a claim audit.
**Blocked on:** one product decision (see [Decision required](#decision-required)).

## Why this exists

A claim audit on 2026-08-01 found that the `security-review` skill's checklist and the
always-on `secure-storage-only` instinct both asserted that Room here is *"encrypted with
SQLCipher"* and that biometric auth is bound to a `CryptoObject`. Neither is true. The
`sqlcipher-not-wired` and `no-cryptoobject` probes in `skills/claim-audit/claims.json` confirm
it: there is no SQLCipher coordinate, no `openHelperFactory`, and no `CryptoObject` anywhere.

The corrections were applied to the skill and the instinct so nothing asserts it any more. But
"stop claiming it" is only half the fix — the *reason* it kept getting claimed is that it is a
reasonable thing for this app to have. This plan is the other half, so the gap is tracked as
work rather than quietly dropped.

## What the database actually holds

From `core/data/.../MilewayDatabase.kt`, the DAOs that make this worth doing:

| DAO | Sensitivity |
|---|---|
| `passportDetailsDao` | Passport number, nationality, expiry — identity documents |
| `signatureDao` | Captured handwritten signatures |
| `locationDao` | Full raw location history, the highest-resolution personal data here |
| `savedTrackDao` | Reconstructed journeys — home and workplace inferable |
| `voucherDao`, `draftExpenseDao` | Financial records |
| `sessionDao` | Session state |
| `vehicleDetailsDao`, `mockAccountDao` | Vehicle registration, account identifiers |

An unencrypted `mileway.db` on a rooted or backed-up device exposes all of it.

## The thing that makes this non-trivial

**`openHelperFactory` does not exist on this code path.** Nearly every SQLCipher-on-Room guide
online targets Room 2.x on Android with `SupportSQLiteOpenHelper.Factory`. This project is not
that:

```kotlin
// core/data/src/androidMain/.../MilewayDatabaseBuilder.kt
Room.databaseBuilder<MilewayDatabase>(context, name = "mileway.db")
    .setDriver(BundledSQLiteDriver())   // ← androidx.sqlite driver API, not SupportSQLite
```

Room KMP uses the `androidx.sqlite` `SQLiteDriver` interface. `net.zetetic:android-database-
sqlcipher` implements the *old* Support API and does not satisfy `SQLiteDriver`. Anyone who
picks this up and follows a standard tutorial will waste an afternoon before hitting that wall.

There is a second constraint: `buildMilewayDatabase` has actuals in `androidMain` **and**
`appleMain` (shared by iOS and watchOS), plus desktop and a wasmJs preview. An Android-only
encryption story silently leaves every other target in plaintext while the README implies
otherwise — which is how the original false claim got written in the first place.

## Options

### A. Field-level encryption using what is already vendored — recommended

`external/kmp-toolkit` already ships `security/KeystoreCrypto.kt`: AES-256-GCM against a
non-exportable `AndroidKeyStore` key, IV prepended, documented for exactly this purpose. It is
already on the dependency graph, so this adds **no new coordinate** and does not touch the
guardrail in `CLAUDE.md`.

Encrypt the sensitive *columns* — passport fields, signature blobs, raw lat/long — via a Room
`@TypeConverter` backed by an `expect`/`actual` crypto seam, with `KeystoreCrypto` as the
Android actual and Keychain-anchored equivalents elsewhere.

- **Pro:** no new dependency; works per-target; encrypts precisely what matters; the
  `:contract`/`:stub` seam is untouched; migration is column-by-column and reversible.
- **Con:** does not encrypt the whole file, so table names, row counts and indices stay visible.
  Queries on encrypted columns cannot use `WHERE`/`ORDER BY` meaningfully — check whether any
  DAO filters or sorts on the columns chosen before committing to the list.

### B. Whole-file encryption via a SQLCipher `SQLiteDriver`

Wrap SQLCipher behind a custom `androidx.sqlite.SQLiteDriver`. Nothing off-the-shelf does this
today, so it is a real implementation, not a wiring exercise.

- **Pro:** everything encrypted including schema and indices; the honest version of the claim
  that was being made.
- **Con:** new dependency → **needs explicit approval** per `CLAUDE.md` guardrails, and it
  changes the `:app:dependencyGuard` baseline and the licence surface of a portfolio repo.
  Android-only unless an equivalent is found for Apple targets. Migrating an existing plaintext
  `mileway.db` across 47 shipped migrations needs its own carefully-tested path.

### C. SQLite3 Multiple Ciphers

A KMP-friendlier whole-file option worth evaluating if B is chosen — same approval requirement.

## Decision required

**Full-file encryption (B/C) or field-level (A)?** Everything downstream depends on it, and B/C
trip the new-dependency guardrail. Recommendation: **A**, because it needs no new coordinate,
covers every target, and protects the data that actually matters. Revisit if a VAPT requirement
ever demands whole-file encryption explicitly.

### RULED 2026-08-05: A stands — but the implementation sketch below is NOT sufficient as written

Adversarially reviewed by nine non-Anthropic models (`openrouter ensemble --tier frontier`,
$0.067) precisely because this plan was authored by Claude, so Claude agreeing with it proves
nothing. Every objection was then verified against this repo. Result:

**REFUTED — the loudest objection does not apply here.** Multiple labs led with a "bricking
scenario": cloud backup restores `mileway.db` to a new device while the non-exportable
AndroidKeyStore key does not survive, permanently orphaning every encrypted column; plus Keystore
invalidation when the lock screen or biometric enrolment changes. Checked: `AndroidManifest.xml:42`
already sets `android:allowBackup="false"`, so the DB never enters Auto Backup, and
`setUserAuthenticationRequired` is *not* enabled on the vendored `KeystoreCrypto` key, so
enrolment changes do not invalidate it.

**Note this is NOT an argument for B.** The flaw they described is about *where the key lives*,
not *what is encrypted* — SQLCipher under a device-bound key has identical exposure. Their
recommendation to switch options does not follow from their own strongest argument.

**SURVIVED — three amendments required before any code is written:**

1. **iOS backup exclusion is a real, open gap.** `allowBackup` is Android-only. Nothing verified
   sets `isExcludedFromBackup` on the database file URL for the Apple targets, so the orphaned-key
   scenario the labs described *is* live on iOS even though Android is covered. Settle this before
   the first encrypted column ships, not after.
2. **"Must be idempotent" (step 4) is an aspiration, not a design.** Room migrations run raw SQL
   and bypass `@TypeConverter`, so a mid-migration process kill leaves a table with mixed
   plaintext and ciphertext rows — and AES-GCM cannot distinguish "not yet encrypted" from
   "corrupt", it just throws on the tag check. Detecting per-row state needs an explicit
   mechanism: a versioned ciphertext envelope (magic prefix + scheme version) so a value can be
   classified before decryption, or a sidecar progress table. Pick one and write it down.
3. **Step 5's instrumented test is theater.** Asserting the raw DB file no longer contains a known
   plaintext string passes even when the scheme is broken. Replace it with tests that assert what
   actually matters: decrypt-after-restore, behaviour when the key is missing or rotated, and that
   a partially-migrated database converges on the next launch.

**Also noted, unresolved by design:** step 3's escape hatch — moving a filtered/sorted column to
deterministic encryption — trades away IND-CPA and exposes passport-style values to frequency
analysis. That tradeoff must be written down per column, not decided ad hoc during implementation.

## Implementation sketch (assumes A)

1. Add an `expect fun encryptField(plain: String): String` / `decryptField` seam in
   `core:data`, actualised per target — Android delegates to `KeystoreCrypto`.
2. Room `@TypeConverter` pair applying it, applied only to the audited column list.
3. Audit every DAO query for `WHERE`/`ORDER BY`/`LIKE` on a candidate column **first**; anything
   that filters or sorts on one either stays plaintext or moves to a deterministic-encryption
   scheme with its tradeoffs written down.
4. `MIGRATION_48_49` re-writing existing rows through the converter. Must be idempotent and
   must handle a partially-migrated database after a mid-migration kill.
5. Tests: round-trip per converter; a migration test proving existing plaintext rows survive;
   an instrumented test asserting the raw file no longer contains a known passport string.
6. Update `README.md` and `security-review` **only after** it is real — and add a claim to
   `skills/claim-audit/claims.json` probing for the converter, so the new statement is
   mechanically defended the way the old one was not.

## Gate

```bash
./gradlew assembleNoGmsDebug && ./gradlew testNoGmsDebugUnitTest
./gradlew ktlintCheck detekt && ./gradlew :app:dependencyGuard
```

Plus the offline check from `CLAUDE.md`: airplane mode, track a trip, kill and relaunch,
confirm the record persisted and decrypts.

## Do not

- Do not update the README, the `security-review` skill or the `secure-storage-only` instinct
  to claim encryption before the code exists and the gate is green. That inversion is the exact
  failure this plan came from.
- Do not add a SQLCipher coordinate without explicit approval.
- Do not encrypt a column that a DAO filters or sorts on without reading step 3.
