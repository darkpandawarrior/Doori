package com.mileway.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
