package com.mileway.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration 36 → 37 (PLAN_V24 P12.5): additive per-account `tour_progress` table — the interactive
 * training tour's step + completed/skipped outcome. Empty on first run (no row = never started); a
 * row is upserted as the user advances/skips/completes the tour. No backend.
 */
val MIGRATION_36_37 =
    object : Migration(36, 37) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tour_progress` (
                    `accountId`   TEXT    NOT NULL PRIMARY KEY,
                    `stepName`    TEXT    NOT NULL,
                    `completed`   INTEGER NOT NULL,
                    `skipped`     INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 35 → 36 (PLAN_V24 P12.8): additive `favourite_routes` table — routes the user pinned
 * from a completed trip (name + quick-start classification default + a distance display cache).
 * Empty on first run; a row is added per pin, deleted on unpin. No backend.
 */
val MIGRATION_35_36 =
    object : Migration(35, 36) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favourite_routes` (
                    `id`            TEXT    NOT NULL PRIMARY KEY,
                    `sourceTrackId` TEXT    NOT NULL,
                    `name`          TEXT    NOT NULL,
                    `purpose`       TEXT    NOT NULL,
                    `distanceKm`    REAL    NOT NULL,
                    `createdAtMs`   INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 34 → 35 (PLAN_V24 P12.7): additive single-row `signature` table — the profile's digital
 * signature PNG path. Empty on first run; a row is written when the user draws + saves a signature,
 * deleted when they clear it. Same single-row shape as `passport_details` / `vehicle_details`.
 */
val MIGRATION_34_35 =
    object : Migration(34, 35) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `signature` (
                    `id`          TEXT    NOT NULL PRIMARY KEY,
                    `imagePath`   TEXT    NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 33 → 34 (PLAN_V24 P12.6): additive `vehicle_audits` table — the per-vehicle self-audit
 * (self-inspection) history. Empty on first run; a row is appended per submitted audit. The verdict
 * is derived at read time by `SimulatedReviewEngine`, so no status column is stored.
 */
val MIGRATION_33_34 =
    object : Migration(33, 34) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicle_audits` (
                    `id`              TEXT    NOT NULL PRIMARY KEY,
                    `vehicleId`       TEXT    NOT NULL,
                    `submittedAtMs`   INTEGER NOT NULL,
                    `checkedItemsCsv` TEXT    NOT NULL,
                    `note`            TEXT    NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 32 → 33 (PLAN_V24 P11.3): the head-home destination store + an auto-classify tag on
 * trips. Additive — a fresh per-account `destination_mode` table plus a nullable `destinationTag`
 * column on `saved_tracks` (existing rows default null). No backend.
 */
val MIGRATION_32_33 =
    object : Migration(32, 33) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `destination_mode` (
                    `accountId`          TEXT    NOT NULL PRIMARY KEY,
                    `placeId`            TEXT,
                    `address`            TEXT,
                    `lat`                REAL,
                    `lng`                REAL,
                    `expiresAt`          INTEGER,
                    `selectedRegionsCsv` TEXT    NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL("ALTER TABLE `saved_tracks` ADD COLUMN `destinationTag` TEXT")
        }
    }

/**
 * Migration 31 → 32 (PLAN_V24 P11.2): the multi-vehicle garage table. Additive — a fresh table, no
 * change to the existing single-row `vehicle_details` store. The seed re-populates on a fresh install.
 */
val MIGRATION_31_32 =
    object : Migration(31, 32) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicles` (
                    `id`                      TEXT    NOT NULL PRIMARY KEY,
                    `brand`                   TEXT    NOT NULL,
                    `model`                   TEXT    NOT NULL,
                    `registrationNumber`      TEXT    NOT NULL,
                    `year`                    INTEGER NOT NULL,
                    `color`                   TEXT    NOT NULL,
                    `seats`                   INTEGER NOT NULL,
                    `vehicleTypeKey`          TEXT    NOT NULL,
                    `photoUri`                TEXT    NOT NULL,
                    `isActive`                INTEGER NOT NULL,
                    `servicesCsv`             TEXT    NOT NULL,
                    `availabilityStartMinute` INTEGER NOT NULL,
                    `availabilityEndMinute`   INTEGER NOT NULL,
                    `availabilityRatePerHour` REAL    NOT NULL,
                    `createdAtMs`             INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 30 → 31 (PLAN_V24 P8.1): additive `payment_wallets` table — external wallet linking
 * (Paytm/Mobikwik-style) via offline OTP. Empty on first run; seeded by
 * `WalletRepository.seedIfEmpty()`, `isLinked` flipped by the link/unlink flow. No real payment SDK.
 */
val MIGRATION_30_31 =
    object : Migration(30, 31) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payment_wallets` (
                    `id`           TEXT    NOT NULL PRIMARY KEY,
                    `providerName` TEXT    NOT NULL,
                    `mobile`       TEXT    NOT NULL,
                    `isLinked`     INTEGER NOT NULL,
                    `balanceMinor` INTEGER NOT NULL,
                    `updatedAtMs`  INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 29 → 30 (PLAN_V24 P7.2): additive device-detail columns on `sessions` (os, appVersion,
 * ip). deviceType is derived from `platform` at read time, so it is not stored. Existing rows get
 * empty defaults; the seed re-populates on a fresh install.
 */
val MIGRATION_29_30 =
    object : Migration(29, 30) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `sessions` ADD COLUMN `os` TEXT NOT NULL DEFAULT ''")
            connection.execSQL("ALTER TABLE `sessions` ADD COLUMN `appVersion` TEXT NOT NULL DEFAULT ''")
            connection.execSQL("ALTER TABLE `sessions` ADD COLUMN `ip` TEXT NOT NULL DEFAULT ''")
        }
    }

/**
 * Migration 28 → 29 (PLAN_V24 P7.1): additive `deletion_request` single-row table — the
 * account-deletion lifecycle (status NONE/REQUESTED/PROCESSING, reason, requestedAt). Empty on first
 * run; a row is created when the user requests deletion, cleared on cancel/completion.
 */
val MIGRATION_28_29 =
    object : Migration(28, 29) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `deletion_request` (
                    `id`            TEXT    NOT NULL PRIMARY KEY,
                    `status`        TEXT    NOT NULL,
                    `reason`        TEXT,
                    `requestedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 27 → 28 (PLAN_V24 P6.2): additive `subscription_plans` (seeded tier catalogue) +
 * `active_subscription` (single-row lifecycle) tables. Both empty on first run — plans seeded by
 * `SubscriptionRepository.seedIfEmpty()`, the active row created by a mock purchase.
 */
val MIGRATION_27_28 =
    object : Migration(27, 28) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `subscription_plans` (
                    `id`                   TEXT    NOT NULL PRIMARY KEY,
                    `name`                 TEXT    NOT NULL,
                    `priceAmount`          REAL    NOT NULL,
                    `period`               TEXT    NOT NULL,
                    `savingsCopy`          TEXT    NOT NULL,
                    `monthlySavingsAmount` REAL    NOT NULL,
                    `featuresCsv`          TEXT    NOT NULL,
                    `tierRank`             INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `active_subscription` (
                    `id`                TEXT    NOT NULL PRIMARY KEY,
                    `planId`            TEXT    NOT NULL,
                    `status`            TEXT    NOT NULL,
                    `startedAtMs`       INTEGER NOT NULL,
                    `renewsAtMs`        INTEGER NOT NULL,
                    `cancelAtPeriodEnd` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 26 → 27 (PLAN_V24 P5.4): additive `campaigns` table — marketing campaigns (status
 * LIVE/UPCOMING/ENDED, one-shot interestCaptured). Empty on first run (seeded by
 * `CampaignRepository.seedIfEmpty()`). See [com.mileway.core.data.model.db.CampaignEntity].
 */
val MIGRATION_26_27 =
    object : Migration(26, 27) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `campaigns` (
                    `id`               TEXT    NOT NULL PRIMARY KEY,
                    `name`             TEXT    NOT NULL,
                    `description`      TEXT    NOT NULL,
                    `badge`            TEXT    NOT NULL,
                    `status`           TEXT    NOT NULL,
                    `mobileExclusive`  INTEGER NOT NULL,
                    `contactEmail`     TEXT    NOT NULL,
                    `interestCaptured` INTEGER NOT NULL,
                    `startedOnMs`      INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 25 → 26 (PLAN_V24 P5.3): additive `reward_cards` table — scratch-card rewards (status
 * UNSCRATCHED/SCRATCHED). Empty on first run (seeded by `RewardsRepository.seedIfEmpty()`).
 * See [com.mileway.core.data.model.db.RewardCardEntity].
 */
val MIGRATION_25_26 =
    object : Migration(25, 26) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reward_cards` (
                    `id`          TEXT    NOT NULL PRIMARY KEY,
                    `title`       TEXT    NOT NULL,
                    `rewardLabel` TEXT    NOT NULL,
                    `credits`     INTEGER NOT NULL,
                    `status`      TEXT    NOT NULL,
                    `grantedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 24 → 25 (PLAN_V24 P5.2): additive `coupons` table — promo codes / coupons (status
 * ACTIVE/EXPIRED/REDEEMED). Empty on first run (seeded by `CouponsRepository.seedIfEmpty()`).
 * See [com.mileway.core.data.model.db.CouponEntity].
 */
val MIGRATION_24_25 =
    object : Migration(24, 25) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `coupons` (
                    `id`          TEXT    NOT NULL PRIMARY KEY,
                    `code`        TEXT    NOT NULL,
                    `title`       TEXT    NOT NULL,
                    `terms`       TEXT    NOT NULL,
                    `expiryLabel` TEXT    NOT NULL,
                    `status`      TEXT    NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 23 → 24 (PLAN_V24 P5.1): additive `referral_txns` table — the referral-program ledger
 * (one row per referred user, status PENDING/SUCCESS/FAILED). Empty on first run (seeded by
 * `ReferralRepository.seedIfEmpty()`). See [com.mileway.core.data.model.db.ReferralTxnEntity].
 */
val MIGRATION_23_24 =
    object : Migration(23, 24) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `referral_txns` (
                    `id`               TEXT    NOT NULL PRIMARY KEY,
                    `refereeName`      TEXT    NOT NULL,
                    `status`           TEXT    NOT NULL,
                    `taskMessage`      TEXT    NOT NULL,
                    `processedMoney`   REAL    NOT NULL,
                    `processedCredits` INTEGER NOT NULL,
                    `userNumRides`     INTEGER NOT NULL,
                    `nextTargetRides`  INTEGER NOT NULL,
                    `nextTargetMoney`  REAL    NOT NULL,
                    `nextTargetCredits` INTEGER NOT NULL,
                    `submittedAtMillis` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 22 → 23 (PLAN_V24 P4.1): additive `documents` table — the verification centre's
 * per-doc requirement + status rows (per the reference app's verification requirements). `doc_url[]`/`doc_info[]`
 * are JSON-encoded TEXT; enum columns store the enum name. Empty on first run (seeded by
 * `DocumentRepository.seedIfEmpty()`). See [com.mileway.core.data.model.db.DocumentEntity].
 */
val MIGRATION_22_23 =
    object : Migration(22, 23) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `documents` (
                    `docType`            TEXT    NOT NULL PRIMARY KEY,
                    `docTypeText`        TEXT    NOT NULL,
                    `requirement`        TEXT    NOT NULL,
                    `status`             TEXT    NOT NULL,
                    `docCount`           INTEGER NOT NULL,
                    `isEditable`         INTEGER NOT NULL,
                    `docUrlsJson`        TEXT    NOT NULL,
                    `reason`             TEXT    NOT NULL,
                    `instructions`       TEXT    NOT NULL,
                    `galleryRestricted`  INTEGER NOT NULL,
                    `category`           TEXT    NOT NULL,
                    `docInfoJson`        TEXT    NOT NULL,
                    `isDocInfoEditable`  INTEGER NOT NULL,
                    `updatedAtMs`        INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 21 → 22 (PLAN_V24 P3.5): additive `emergency_contacts` table — name + phone +
 * dial-code, capped at 5 by the repository. Empty on first run.
 * See [com.mileway.core.data.model.db.EmergencyContactEntity].
 */
val MIGRATION_21_22 =
    object : Migration(21, 22) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `emergency_contacts` (
                    `id`          TEXT    NOT NULL PRIMARY KEY,
                    `name`        TEXT    NOT NULL,
                    `phoneNo`     TEXT    NOT NULL,
                    `countryCode` TEXT    NOT NULL,
                    `createdAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 20 → 21 (PLAN_V24 P3.4): additive `saved_places` table — home/work/other saved
 * addresses (label + free-text address + optional coordinates). Empty on first run; the HOME row,
 * when present, is the canonical home location surfaced alongside `EmployeeProfile.homeLocation`.
 * See [com.mileway.core.data.model.db.SavedPlaceEntity].
 */
val MIGRATION_20_21 =
    object : Migration(20, 21) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `saved_places` (
                    `id`         TEXT    NOT NULL PRIMARY KEY,
                    `type`       TEXT    NOT NULL,
                    `label`      TEXT    NOT NULL,
                    `address`    TEXT    NOT NULL,
                    `latitude`   REAL,
                    `longitude`  REAL,
                    `updatedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 19 → 20 (PLAN_V24 P1.6): additive `phone` column on `mock_accounts` — the persona's
 * registered phone, used for duplicate-account resolution on phone-OTP login. Nullable-safe:
 * defaults to `''` for existing rows (they simply won't match any phone-login).
 */
val MIGRATION_19_20 =
    object : Migration(19, 20) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `mock_accounts` ADD COLUMN `phone` TEXT NOT NULL DEFAULT ''")
        }
    }

/**
 * Migration 18 → 19 (PLAN_V24 P0.1): additive `plugin_overrides` table — the per-account USER
 * layer of the Plugin Registry (the single feature-composition mechanism). Composite key
 * `(accountId, pluginId)` isolates each persona's overrides (respects V23 multi-profile
 * isolation); `value` is a raw string interpreted per plugin descriptor. Empty on first run —
 * no seed, since "no user overrides" is the correct initial state (PRESET/DEFAULT resolve
 * everything). See [com.mileway.core.data.model.db.PluginOverrideEntity].
 *
 * ponytail: later V24 phases add their own additive migrations (20, 21, …) as each lands, rather
 * than the one mega-batch PLAN_V24 P0.6 sketched — keeps one-task-one-commit rollback clean.
 */
val MIGRATION_18_19 =
    object : Migration(18, 19) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plugin_overrides` (
                    `accountId` TEXT NOT NULL,
                    `pluginId`  TEXT NOT NULL,
                    `value`     TEXT NOT NULL,
                    PRIMARY KEY(`accountId`, `pluginId`)
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 17 → 18 (§2.4): additive `odometerAnalysisJson` column on `trip_attachments` — a
 * typed, structured snapshot of the odometer-reading validation (see
 * [com.mileway.core.data.model.OdometerAnalysisSnapshot]) alongside the existing raw
 * [com.mileway.core.data.model.db.TripAttachmentEntity.ocrText]. Nullable, so this is a plain
 * additive ALTER with no backfill for existing rows.
 */
val MIGRATION_17_18 =
    object : Migration(17, 18) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `odometerAnalysisJson` TEXT")
        }
    }

/**
 * Migration 16 → 17 (PLAN_V22 P6.8): additive `support_tickets` table — a real, persisted "My
 * Tickets" store backing `HelpScreen`'s "Contact Support" form, replacing its previous
 * fire-and-forget `snackbarHostState.showSnackbar(...)`-only tap with nothing inspectable
 * afterward. Seeded lazily (no first-run seed — an empty ticket list is the correct initial
 * state, mirroring a fresh support inbox). See [com.mileway.core.data.model.db.SupportTicketEntity].
 */
val MIGRATION_16_17 =
    object : Migration(16, 17) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `support_tickets` (
                    `id`          TEXT    NOT NULL PRIMARY KEY,
                    `subject`     TEXT    NOT NULL,
                    `body`        TEXT    NOT NULL,
                    `createdAtMs` INTEGER NOT NULL,
                    `status`      TEXT    NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 15 → 16 (PLAN_V22 P6.6): additive `connected_accounts` table — a real, persisted
 * "Connected Accounts" list backing Preferences' tile of the same name, replacing its previous
 * `RaisePreferenceMessage("... is a demo placeholder.")` snackbar-only tap. Seeded once (mock
 * cab/passport-style integrations) by `ConnectedAccountsRepository` on first run, mirroring
 * [MIGRATION_14_15]'s seed-on-first-run shape. See
 * [com.mileway.core.data.model.db.ConnectedAccountEntity].
 */
val MIGRATION_15_16 =
    object : Migration(15, 16) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `connected_accounts` (
                    `id`            TEXT    NOT NULL PRIMARY KEY,
                    `providerName`  TEXT    NOT NULL,
                    `category`      TEXT    NOT NULL,
                    `isConnected`   INTEGER NOT NULL,
                    `updatedAtMs`   INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 14 → 15 (PLAN_V22 P6.5): additive `notifications` table — a real, persisted
 * Notification Centre feed, replacing `NotificationCentreScreen`'s `remember { mutableStateOf(
 * NOTIFICATIONS) }` seed (reset on navigation away) and its hardcoded "174 unread" topbar
 * subtitle. Seeded once from [com.mileway.feature.profile.data.NotificationData.all] by
 * `NotificationRepository` on first run, mirroring [MIGRATION_10_11]'s seed-on-first-run shape.
 * See [com.mileway.core.data.model.db.NotificationEntity].
 */
val MIGRATION_14_15 =
    object : Migration(14, 15) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notifications` (
                    `id`           TEXT    NOT NULL PRIMARY KEY,
                    `title`        TEXT    NOT NULL,
                    `body`         TEXT    NOT NULL,
                    `relativeTime` TEXT    NOT NULL,
                    `isUnread`     INTEGER NOT NULL,
                    `type`         TEXT    NOT NULL,
                    `createdAtMs`  INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 13 → 14 (PLAN_V22 P6.4): additive `sessions` table — a real, persisted store of
 * devices the demo account is signed in on, replacing `stub.ProfileMockData.sessions()`'s bare
 * in-memory list so `ActiveSessionsScreen`'s per-session revoke and "Sign out all other sessions"
 * bulk action actually persist across app kill/relaunch. See
 * [com.mileway.core.data.model.db.SessionEntity].
 */
val MIGRATION_13_14 =
    object : Migration(13, 14) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sessions` (
                    `id`               TEXT    NOT NULL PRIMARY KEY,
                    `deviceName`       TEXT    NOT NULL,
                    `platform`         TEXT    NOT NULL,
                    `lastActiveMillis` INTEGER NOT NULL,
                    `isCurrent`        INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 12 → 13 (PLAN_V22 P6.3): additive `delegations` table — real, persisted approval
 * delegations backing `DelegationScreen`'s "My Delegations" list, replacing its
 * `mutableStateListOf` seed that reset on navigation away. This is the approval-delegation
 * concept, not the account-switch/session-delegate concept (see PLAN_V22 §2's Architecture note);
 * it does not touch `mock_accounts` or any account-switch table.
 */
val MIGRATION_12_13 =
    object : Migration(12, 13) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `delegations` (
                    `id`              TEXT    NOT NULL PRIMARY KEY,
                    `delegateName`    TEXT    NOT NULL,
                    `scope`           TEXT    NOT NULL,
                    `expiresAtMillis` INTEGER NOT NULL,
                    `isActive`        INTEGER NOT NULL,
                    `createdAt`       INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 11 → 12 (PLAN_V22 P6.2): additive `vehicle_details` + `passport_details` singleton
 * tables backing the Profile Details screen's new Vehicle/Passport tiles. Both are single-row
 * tables (fixed primary key, same pattern as `draft_expenses`) — one vehicle and one passport per
 * demo profile.
 */
val MIGRATION_11_12 =
    object : Migration(11, 12) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicle_details` (
                    `id`                 TEXT    NOT NULL PRIMARY KEY,
                    `make`               TEXT    NOT NULL,
                    `model`              TEXT    NOT NULL,
                    `registrationNumber` TEXT    NOT NULL,
                    `fuelType`           TEXT    NOT NULL,
                    `seatingCapacity`    INTEGER NOT NULL,
                    `updatedAtMs`        INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `passport_details` (
                    `id`                TEXT    NOT NULL PRIMARY KEY,
                    `passportNumber`    TEXT    NOT NULL,
                    `issuingCountry`    TEXT    NOT NULL,
                    `expiryDateMillis`  INTEGER NOT NULL,
                    `updatedAtMs`       INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 10 → 11 (PLAN_V22 P1.1): additive `mock_accounts` table — a real, persisted,
 * seedable multi-persona account store, replacing `stub.ProfileMockData.accounts()`'s bare
 * in-memory list as the source of truth for account switching. See
 * [com.mileway.core.data.model.db.MockAccountEntity].
 */
val MIGRATION_10_11 =
    object : Migration(10, 11) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mock_accounts` (
                    `accountId`     TEXT    NOT NULL PRIMARY KEY,
                    `displayName`   TEXT    NOT NULL,
                    `employeeCode`  TEXT    NOT NULL,
                    `organization`  TEXT    NOT NULL,
                    `avatarSeed`    TEXT    NOT NULL,
                    `isActive`      INTEGER NOT NULL,
                    `lastLoginAtMs` INTEGER NOT NULL,
                    `createdAtMs`   INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 9 → 10 (P3.4): `vouchers.category` becomes the typed [com.mileway.core.data.model.db
 * .VoucherCategory] enum instead of a free-text `String` — but the underlying column is still
 * `TEXT` storing the exact same label values (`"Travel"`/`"Fuel"`/`"Maintenance"`/`"Other"`) via
 * [com.mileway.core.data.model.db.VoucherCategoryConverters], so no column type or existing row
 * data actually changes. This migration is a documented no-op version bump only, matching the
 * project rule that any entity-shape change to an already-shipped table gets an explicit
 * `Migration(N, N + 1)` object rather than relying on `exportSchema`/destructive fallback.
 */
val MIGRATION_9_10 =
    object : Migration(9, 10) {
        override fun migrate(connection: SQLiteConnection) {
            // No-op: the `category` column's on-disk type/values are unchanged (see class doc).
        }
    }

/**
 * Migration 8 → 9 (P3.3): additive `claimedByVoucherNumber` column on `saved_tracks` — the
 * already-claimed guard so the same completed trip can't fund two separate vouchers (mirrors a
 * common server-side remaining-voucher-count check). Null means "unclaimed"; existing rows
 * default to null, so nothing already-submitted becomes newly claimed by this migration alone.
 */
val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `saved_tracks` ADD COLUMN `claimedByVoucherNumber` TEXT")
        }
    }

/**
 * Migration 7 → 8 (P3.1): shared `vouchers` table — the single Room-backed store both
 * `CreateVoucherViewModel` (feature/tracking) and `VoucherHistoryViewModel` (feature/logging)
 * bind to via Koin, replacing two disconnected in-memory stores. See
 * [com.mileway.core.data.model.db.VoucherEntity].
 */
val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vouchers` (
                    `voucherNumber`        TEXT    NOT NULL PRIMARY KEY,
                    `title`                TEXT    NOT NULL,
                    `category`             TEXT    NOT NULL,
                    `totalAmount`          REAL    NOT NULL,
                    `notes`                TEXT    NOT NULL,
                    `expenseRouteIdsJson`  TEXT    NOT NULL,
                    `status`               TEXT    NOT NULL,
                    `createdAtMs`          INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 6 → 7 (P1.5): single-row `draft_expenses` table so `ExpenseAction.SaveDraft`
 * actually persists across app kill/relaunch instead of only living in `ExpenseViewModel`
 * state. Mirrors `ExpenseFormState`'s fields (see [com.mileway.core.data.model.db.DraftExpenseEntity]).
 */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `draft_expenses` (
                    `draftId`          TEXT    NOT NULL PRIMARY KEY,
                    `categoryName`     TEXT,
                    `amountText`       TEXT    NOT NULL,
                    `merchantName`     TEXT    NOT NULL,
                    `note`             TEXT    NOT NULL,
                    `receiptImagePath` TEXT,
                    `updatedAt`        INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration 5 → 6 (P1.1): agent conversations + messages tables for persistent chat history.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_conversations` (
                    `id`            TEXT    NOT NULL PRIMARY KEY,
                    `title`         TEXT    NOT NULL,
                    `lastMessageMs` INTEGER NOT NULL,
                    `createdAtMs`   INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_messages` (
                    `messageId`       TEXT    NOT NULL PRIMARY KEY,
                    `conversationId`  TEXT    NOT NULL,
                    `text`            TEXT    NOT NULL,
                    `isUser`          INTEGER NOT NULL,
                    `timestampMs`     INTEGER NOT NULL,
                    `feedbackRating`  INTEGER,
                    `feedbackComment` TEXT,
                    `module`          TEXT,
                    `isContextReset`  INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`conversationId`) REFERENCES `agent_conversations`(`id`) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_agent_messages_conversationId` ON `agent_messages` (`conversationId`)",
            )
        }
    }

/**
 * Migration 4 → 5 (D.5): enrich trip_attachments with a file name + multi-pass OCR provenance
 * (confidence + verified flag from [com.mileway.feature.media.ocr.OdometerOcrAggregator]).
 * Plain additive ALTERs; existing rows keep the column defaults.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `file_name` TEXT")
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `ocr_confidence` REAL NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `ocr_verified` INTEGER NOT NULL DEFAULT 0")
        }
    }

/**
 * Migration 1 → 2: introduce the trip_attachments table.
 *
 * Each row stores one photo attachment (receipt or odometer proof) keyed by the
 * track_token (= SavedTrack.routeId). The type column uses the AttachmentType enum
 * name (TEXT): RECEIPT, ODOMETER_START, ODOMETER_END.
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `submit_drafts` (
                    `formKey`      TEXT    NOT NULL,
                    `uniqueKey`    TEXT    NOT NULL,
                    `payloadJson`  TEXT    NOT NULL,
                    `status`       TEXT    NOT NULL,
                    `errorMessage` TEXT,
                    `createdAt`    INTEGER NOT NULL,
                    `updatedAt`    INTEGER NOT NULL,
                    PRIMARY KEY(`formKey`, `uniqueKey`)
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_library` (
                    `id`        TEXT    NOT NULL PRIMARY KEY,
                    `uri`       TEXT    NOT NULL,
                    `mimeType`  TEXT    NOT NULL,
                    `label`     TEXT    NOT NULL,
                    `source`    TEXT    NOT NULL,
                    `savedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trip_attachments` (
                    `id`          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `track_token` TEXT    NOT NULL,
                    `type`        TEXT    NOT NULL,
                    `uri`         TEXT    NOT NULL,
                    `ocr_text`    TEXT,
                    `created_at`  INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_trip_attachments_track_token` ON `trip_attachments` (`track_token`)",
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_trip_attachments_track_token_type` ON `trip_attachments` (`track_token`, `type`)",
            )
        }
    }
