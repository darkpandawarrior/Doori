# Doori (ex-Mileway) BACKLOG, consolidated 2026-11-03

Slug: `mileway` (plans folder name kept; the product is now Doori).
As-of: 2026-11-03. Read-only consolidation; no code, plan or handoff was edited.

Repos:
- `/Users/darkpandawarrior/Repos/Android/Doori` (GitHub `darkpandawarrior/Doori`, public). Local `main` = `origin/main` = `91af9ccc` (2026-10-28), working tree clean, no stashes, one registered worktree (the main checkout).
- `/Users/darkpandawarrior/Repos/Android/Doori/.worktrees/kmp-toolkit-plain-location`: NOT a live worktree, see "Dangling work".

Evidence method: `git grep` on `main`, `gh pr list -R darkpandawarrior/Doori --state all --limit 100`, `git ls-remote --heads origin`, `git worktree list`, `git stash list`, file reads. Items marked (carried) were verified by the 2026-10-19 collation (`plans/mileway/2026-10-19-doori-completion/BACKLOG.md`) and not re-checked today. Everything else was re-checked on 2026-11-03.

## Where the program actually stands

- Doori is a 36-module KMP expense/mileage/travel app. The 29-lane "doori-suite" (L0 to L36, W1 to W3) is fully landed: Phase 1 #231, Phase 2 #232, Phase 3 #235, Phase 4 #259 (merged 2026-10-20), all as merge commits.
- The completion HANDOFF (`2026-10-19-doori-completion/HANDOFF.md`) lists 5 steps. Step 1 (land phase 4) and step 2 (behaviour spec, `SPEC.md`) are DONE. Step 5 (hygiene) is mostly done. Steps 3 (design pass) and 4 (feature/backend lanes from BACKLOG + SPEC) have not started. Since 2026-10-20 the only change on main is #260 (strip agent markers, 2026-10-28). Nothing has been built in the 14 days since the spec landed.
- The reason work "goes missing" here: 7 places claim to be the plan or the progress log (see Sources consolidated), 3 of them contradict reality (suite `PROGRESS.md` still says Phase 3 and 4 lanes are `queued`; `OWNER_ACTIONS.md` and `DECISIONS.md` still say noGms has 19 GMS entries, it has 0; `.ralph/FRONTIER.md` still points at V37 to V39 as the frontier and is gitignored).

## Start here (3 highest-value next actions)

1. **Close the open PRs and delete the dead remote branches, in one sitting (S).**
   Open: #258 (kmp-toolkit bump to f2c83f8, all checks pass, mergeable), #257 (screenshot baseline refresh, 122 files, mergeable, created before #259 merged so re-record or close), Dependabot #250 to #254 (firebase-bom 34 to 35 is a major bump: ask-first). Delete `origin/redo/L20`, `origin/redo/L25` (1 commit each, behind main by 31, superseded by merged L20/L25 in #259), and `origin/bot/refresh-screenshot-baselines` if #257 is closed.
   Done-test: `gh pr list -R darkpandawarrior/Doori --state open` returns only deliberate items, and `git ls-remote --heads origin` lists only `main` plus live PR branches.
2. **First fix the teardown and spec (section F, TD-01 to TD-04), then run the SPEC.md planner pass, then execute the top of the gap table as phase 5 (the missing step 4).**
   `SPEC.md` section 10 already ranks 20 gaps; its own caveat says "Doori lacks X" came from a keyword scan, so first confirm each row against code and size it. Recommended first lane group (all unblocked, no new dependency, no owner call): SRV-02 + SRV-03 (server persists per-diem/advance lines and FX fields; `server/.../ReportRoutes.kt:250` still throws `error("Persisting ... is not yet implemented")`), EXP-05 + SPEC-16 + SPEC-20 (attach an advance to a report, settlement writes, balance block), RPT-02 (one money formatter, 59 `"₹${...}"` sites remain on main).
   Done-test: one integration branch `feat/doori-phase5` merged via a gated PR; `git grep -c '"₹\${' -- '*.kt'` drops to 0; a `:server` test round-trips a per-diem line and an advance line.
3. **Fix the three lying docs and make this file the only forward index (S).**
   (a) `docs/OWNER_ACTIONS.md` 6.1 and `docs/DECISIONS.md` 2026-06-23 say noGms has 15 GMS/ML Kit + 4 Firebase entries; `app/dependencies/noGmsReleaseRuntimeClasspath.txt` now has 0 (L13 landed). That also resolves the ML Kit "owner decision" in the Owner Actions text: the choice was made implicitly by L13, so confirm and record it. (b) Mark the suite `PROGRESS.md` lane table as final (all lanes MERGED). (c) Copy this BACKLOG into the Doori repo as `docs/BACKLOG.md` (it contains no secrets or client values) and add one line to `AGENTS.md` / `.ralph/FRONTIER.md` pointing at it instead of at V37 to V39.
   Done-test: `grep -c 'play-services\|com.google.mlkit' app/dependencies/noGmsReleaseRuntimeClasspath.txt` = 0 and `grep -n "19 proprietary" docs/OWNER_ACTIONS.md` = no hit; a fresh session reading only AGENTS.md reaches this file.

## Verdict counts

144 items: DONE 25, PARTIAL 13, NOT-BUILT 98, SUPERSEDED 3, DROP-PROPOSAL 5. Counted with a script over the table rows after the 2026-11-03 RE-MAP update (adds TD-01 to TD-16); several rows group related items (e.g. TRK-15, EXP-24), so item counts understate the SPEC.md gap-table scope.

## Item table

Verdicts: DONE, PARTIAL, NOT-BUILT, SUPERSEDED, DROP-PROPOSAL. Size: S under half a day, M 1 to 3 days, L more.

### A. Shipped (the doori-suite and its predecessors)

| Id | Item | Verdict | Evidence | Source |
|---|---|---|---|---|
| SHP-01 | L0 frozen v48 Room schema + `check-frozen-migrations.sh` gate | DONE | PR #230; `core/data/schemas/*/48.json`, `49.json`, `50.json` present | suite HANDOFF |
| SHP-02 | Phase 1: claim domain, policy engine, persistence, approval+payout, report submit, mileage roll-up, zero-GMS location, activity-recognition transitions, W1 wire-up, W2 captures | DONE | PR #231 (merge a97f272f) | suite PROGRESS, HANDOFF |
| SHP-03 | Phase 2: per-diem, FX pins, split/attendee fields, advances reconcile, card import, approval depth (schema 50), AI affidavit, OCR confidence, rate tables | DONE | PR #232; `MilewayDatabase.kt:169` version = 50 | suite PROGRESS |
| SHP-04 | Phase 3: saved places, reimbursement status, rate versions, OCR depth, travel requests, iOS shell, whats-new | DONE | PR #235 | suite PROGRESS |
| SHP-05 | Phase 4: holiday flags + FX provenance (L19), explainable policy flags from OCR (L20), local nudges (L23), debug backend toggle (L24), GL mapping + export (L25) | DONE | PR #259 merge bf096426; `NagerDateClient.kt`, `DetectionHealthNudges.kt`, `GlMappingDao.kt`, `app/src/debug/.../DebugBackendToggle.kt` on main | completion HANDOFF |
| SHP-06 | Ktor `:server`, `:contract`, opId idempotency, backend infra | DONE | PR #225; both modules in `settings.gradle.kts` | BACKLOG 10-19 |
| SHP-07 | True zero-GMS noGms flavour, `PlainLocationTracker` per flavour (L13, kmp-toolkit PR #67) | DONE | noGms dependency baseline has 0 matches for play-services / mlkit / firebase; `PlainLocationTracker` in 6 files | FRONTIER, suite HANDOFF |
| SHP-08 | V25 to V32 series: core:media, core:ai, core:forms, unified capture, 16-type forms, clarification rooms, search + notification centre, shell-nav fix, L10N pass | DONE | local `.ralph/PROGRESS.md` "PLAN_V25 to V32 SERIES COMPLETE (2026-07-13)"; modules exist in `settings.gradle.kts` | PLAN_V25_ONWARDS_MASTER, DEFERRED_BACKLOG |
| SHP-09 | V33 backend + parity, V34 close-out, V35 home cards, V36 what's-new | DONE | local PROGRESS "V33 COMPLETE", "V34/V35 FINAL"; `feature:whatsnew` module exists; `AppSyncTrigger` in `MilewayApplication.kt` | PLAN_V33_MASTER, V34, V35, V36 |
| SHP-10 | V37 phase 1: GMS flavour-gating of activity recognition | DONE | PR #48 (per PROGRESS 2026-07-18); `ActivityRecognizer` in 12 files | PLAN_V37 |
| SHP-11 | Compose-resources missing from Android APK fix | DONE | PROGRESS V34 P1: "VERIFIED present in app-noGms-debug.apk" and on emulator | 2026-07-10-compose-resources-android-assets-fix.md |
| SHP-12 | Task 5: `MlKitLlmGateway` consumes the kmp-toolkit `OnDeviceLlm` seam | DONE | `feature/agent/build.gradle.kts:30-41` references the seam | `.superpowers/sdd/task-5-*` |
| SHP-13 | Hardware-event VM tests + Koin outbox binding | DONE | PR #76, #73 (FRONTIER 2026-08-05) | tracked FRONTIER |
| SHP-14 | Required-checks ruleset (Build & Test + quality gate) | DONE | ruleset 19036462, OWNER_ACTIONS 0.1 | OWNER_ACTIONS |
| SHP-15 | DESIGN.md, Paper default theme | DONE | PR #236; `DESIGN.md` at repo root | design/ , DECISIONS |
| SHP-16 | UI/UX quick wins 1 and 2: onPrimary contrast, list motion, shimmer removal, reduced motion, TalkBack roles, 48dp targets, strings to resources | DONE | PR #242, #248 | audit-doori.md |
| SHP-17 | Toolchain alignment (AGP alpha10, Kotlin 2.4.21, CMP alpha02, compileSdk 37.1), maplibre-compose 0.20, dependabot batches | DONE | PR #256, #247, #243 to #246, #249 | BACKLOG 10-19 |
| SHP-18 | Decisions log | DONE | PR #255; `docs/DECISIONS.md` | completion HANDOFF |
| SHP-19 | Local rename Mileway to Doori, worktrees and symlink shim removed | DONE | `~/Repos/Android/Mileway*` and `Doori-wt*` no longer exist; PR #222 (prose rename) | completion HANDOFF |
| SHP-20 | Behaviour spec from the shipped employee app (SPEC-01 to 22, gap table) | PARTIAL | `2026-10-19-doori-completion/SPEC.md` (466 lines) exists, but `RE-MAP.md` (2026-11-03) shows its source teardown is incomplete (13,604 of 45,370 top-level classes undecompiled, main Log Miles view model missing from the tree) and several spec facts are wrong; fix list in section F. Decompiled output stays in `~/RE/doori-spec` outside git | RE-MAP.md |
| SHP-21 | Milestone Program Wave 0.1 `verifyAll`, Wave 2.4 artefact index | DONE | `build.gradle.kts:383`; `docs/ARTEFACT_INDEX.md` regenerated 2026-10-28 | MILESTONE_RELEASE_PROGRAM |
| SHP-22 | Merged branches cleanup from the 10-19 list (docs/design-md, quick-wins, maplibre, archive/2026-10-11, lane/L20, lane/L25, feat/doori-phase4, stashes, chore/agp-alpha08, toolchain branches) | DONE | `git ls-remote --heads origin` shows none of them; `git stash list` empty | completion HANDOFF |

### B. Open work: tracking, expenses, travel, reports

| Id | Item | Verdict | Evidence | Source |
|---|---|---|---|---|
| TRK-03 | Gms-flavour unit test for `GmsDriveStartSource` / `GmsDriveTransitionReceiver` | NOT-BUILT | no test file matches those names | PROGRESS follow-ups |
| TRK-04 | L4 mapper built per INELIGIBLE row; fixed id `mileage_<tripId>` throws on retry | NOT-BUILT (carried, unverified) | L4 verifier note | suite PROGRESS |
| TRK-05 | `core:sensors` extraction (V37 phase 2) | NOT-BUILT | no `:core:sensors` in `settings.gradle.kts` | PLAN_V37 |
| TRK-06 | iOS CMMotionActivityManager / CMPedometer (V37 phase 3) | NOT-BUILT | no references on main | PLAN_V37 |
| TRK-07 | HAR model choice (custom vs pre-trained) and standalone-GPS yes/no | NOT-BUILT (owner decision) | FRONTIER ask-first gates 1 and 3 | FRONTIER |
| TRK-08 | `iosTest` source set for `feature:tracking` | NOT-BUILT | `feature/tracking/src` has androidMain, commonMain, commonTest, iosMain, main; no iosTest | tracked FRONTIER |
| TRK-10 | L23 (TRK-09 nudges, `e5110d0e`, `DetectionHealthNudges.kt`, merged per RE-MAP.md) gaps: "not yet claimed" copy, no device background-execution proof, Swift not compiled in the gate | PARTIAL | nudge code on main (`DetectionHealthNudges.kt`); gaps from PR #259 body | session-kit/phase4-pr-body.md |
| TRK-11 | `LocationDataSyncer` `send` wired to real endpoints | DONE | `di/TrackingModule.kt:106-112`, `RealLocationSend.kt:32` on main 91af9ccc (RE-MAP.md); real-backend gate not checked | RE-MAP.md |
| TRK-12 | Shadow algorithm mode needs a real drive and a 15 to 30 trip reference corpus | PARTIAL | `ShadowAlgorithmRunner.kt` exists, off by default; no corpus anywhere in repo | MILESTONE_RELEASE_PROGRAM Wave 3 |
| TRK-13 | Battery guard on journey start (SPEC-04) | DONE | `feature/tracking/.../manager/PreflightChecks.kt` blocks at 10 percent or below, warns below 30 (RE-MAP.md); "start anyway" not verified | RE-MAP.md |
| TRK-14 | Odometer rollover and digit lock (SPEC-11) | NOT-BUILT | SPEC gap table row 18 | SPEC.md |
| TRK-15 | Insight rules, data-quality score, tracking-issue list (SPEC-06); crash session restore from 3 sources (SPEC-22); log miles inside a trip (SPEC-19); bubble overlay (SPEC-13) | NOT-BUILT (SPEC says partial, unverified) | SPEC section 9 | SPEC.md |
| EXP-02 | `DraftExpenseEntity` cannot hold new expense details or FX pins | NOT-BUILT (carried) | PR #232 not-fixed list | PR #232 |
| EXP-03 | Mixed-currency report shows no combined total | NOT-BUILT (carried) | PR #232 | PR #232 |
| EXP-04 | Affidavit/justification covers expense lines only, not mileage lines | NOT-BUILT (carried) | PR #232 | PR #232 |
| EXP-05 | No UI attaches an AdvanceLine to a report; no settlement writes (also SPEC-16 balance block, SPEC-20 ten-kind settlement ledger) | NOT-BUILT | L9 follow-up; SPEC gap rank 7 | suite PROGRESS, SPEC.md |
| EXP-06 | Accounting months use UTC; pre-v50 reports have NULL period metadata | NOT-BUILT (carried) | PR #232 | PR #232 |
| EXP-07 | No segregation of duties (same identity can be manager and FINANCE) | NOT-BUILT (carried; needs real-backend roles) | L11 verifier note | suite PROGRESS |
| EXP-08 | `com.siddharth.kmp:ai` declared by L12, nothing imports it | NOT-BUILT (carried) | `git grep "import com.siddharth.kmp.ai" -- feature/logging` = no hit | suite PROGRESS |
| EXP-09 | L10 card-import UI strings hardcoded; iOS imports by pasted text only | NOT-BUILT (carried) | PR #232 | suite PROGRESS |
| EXP-11 | L21 mileage-rate schedules not wired to any UI or the policy engine | NOT-BUILT (carried) | PR #232 | PR #232 |
| EXP-12 | India perquisite is a secondary-source note; FAVR does not certify all rules | NOT-BUILT (carried) | PR #232 | PR #232 |
| EXP-13 | L17 rate versions stored but unread by `PolicyEngine`; editor is an unauthenticated local admin surface | NOT-BUILT (carried) | PR #235 | PR #235 |
| EXP-14 | `MileageAppFunctions.kt` and `OfflineAssistantEngine.kt` assume INR | NOT-BUILT (carried; INR text still at `MileageAppFunctions.kt:122`) | L7 verifier note | suite PROGRESS |
| EXP-15 | FX label prints raw Double; no test of card-rate over an existing pin | NOT-BUILT (carried) | L7 verifier note | suite PROGRESS |
| EXP-17 | Home-address redaction does not cover debug-menu export via `TrackExportManager` | NOT-BUILT (carried) | `TrackExportManager` still in 3 files | suite PROGRESS |
| EXP-18 | Plurals ("1 items"), affidavit field sizing, raw "MILE" label, "16.09344 km" | NOT-BUILT (carried) | PR #232, #235 polish lists | PR bodies |
| EXP-20 | L19 (EXP-19, `e1b6a54b`, `NagerDateClient.kt`, `HolidayFlagUseCase.kt`, merged per RE-MAP.md; FX provenance part unverified) holiday/country-year lookups are serial | NOT-BUILT (carried) | L19 note | L19-report.md |
| EXP-21 | L20 (EXP-16, `a3358359`, `ExplainableViolation.kt`, merged per RE-MAP.md) extras: ExpenseScreen OCR-state wiring has no unit test; OCR provenance not persisted (needs schema) | PARTIAL | L20 verifier note; L20 itself merged in #259 | lanes/L20/report.md |
| EXP-22 | L25 (EXP-10, `66a07a06`, `GlExportCsv.kt`, merged per RE-MAP.md) GL export extras: CSV-injection guard prefixes a GL code starting with - or + | PARTIAL | L25 verifier note; L25 merged in #259 | lanes/L25/report.md |
| EXP-23 | Voucher hold/unhold with remarks (SPEC-12) | NOT-BUILT | SPEC gap rank 20 | SPEC.md |
| EXP-24 | OCR retry/edit flow and bulk upload queue (SPEC-09); document-trust classification (SPEC-21); cloud library OCR cache + purge (SPEC-14) | NOT-BUILT | SPEC gap ranks 10 and 12 | SPEC.md |
| EXP-25 | Dependent-services eligibility review before mileage claim (SPEC-10) | NOT-BUILT | SPEC gap rank 13 | SPEC.md |
| EXP-26 | Reportee track view and approver distance edit (SPEC-17) | NOT-BUILT | SPEC gap rank 14 | SPEC.md |
| TRV-02 | Travel requests are session-only; durable persistence needs Room v51; review is a local simulation | NOT-BUILT (carried) | PR #235 | PR #235 |
| TRV-03 | L15 saved places take raw lat/lng; no saved-place picker | NOT-BUILT (carried) | PR #235 | PR #235 |
| TRV-04 | Per-diem date error shown too early; delegate chip selected state | NOT-BUILT (carried) | PR #235 polish | PR #235 |
| TRV-05 | In-app GDS booking, email/SMS receipt ingestion, trip split/merge | DROP-PROPOSAL | spec.md and SPEC.md both mark it deliberately out of scope | SPEC.md rank 4 |
| TRV-06 | Travel reschedule lifecycle and travel-confirmation flow (SPEC-08); per-date daily-allowance queue with resubmit (SPEC-07) | NOT-BUILT | SPEC gap ranks 8 and 9 | SPEC.md |
| RPT-02 | One money formatter | NOT-BUILT | `git grep -c '"₹\${'` = 59 lines; `formatMoney` private copies remain in `feature/advances` and `feature/cards`; `formatMinorCurrency` exists in `:contract` (22 files) | audit-doori item 2 |
| RPT-04 | Indian lakh grouping for ₹ | NOT-BUILT | no `Grouping.Indian` / lakh hit; decision recorded 2026-10-19 | mobile-uiux DECISIONS |
| RPT-05 | Home mileage card shows demo constants | NOT-BUILT | `shared/.../HomeComponents.kt:1534-1536` `WEEK_KM_DEMO = 248.0`, `WEEK_AMOUNT_DEMO = 14820.0`, used at 1598 to 1658 | audit-doori item 4 |
| APR-02 | Inline approve with undo; remaining bulk-comment strings | NOT-BUILT (carried; no "undo" in `feature/approvals` commonMain not confirmed, grep errored) | audit-doori item 12 | audit-doori.md |
| APR-03 | Dated out-of-office window with delegate (SPEC-01) | NOT-BUILT | SPEC gap rank 15 | SPEC.md |

### C. Open work: backend, sync, iOS, Wear, platform gaps

| Id | Item | Verdict | Evidence | Source |
|---|---|---|---|---|
| SRV-02 | Persist PerDiemLine / AdvanceLine on the server | NOT-BUILT | `server/.../ReportRoutes.kt:250` `error("Persisting ... is not yet implemented")` | suite HANDOFF follow-ups |
| SRV-03 | `:server` codec drops FX fields and sums amountMinor without currency | NOT-BUILT (carried) | `ReportRoutes.kt:225-272`, `:107` | suite PROGRESS L7 |
| SRV-04 | Golden-gate must-cases (79 of 106) as `:server` tests | NOT-BUILT (carried, unverified) | `2026-09-25-doori-suite/backend-golden-gate.md` | #225 follow-up |
| SRV-05 | Flip `useRealBackend` for production | NOT-BUILT (owner decision, one-way door) | flag still false; only debug/release toggle code reads it (`DebugBackendToggle.kt`) | AGENTS.md guardrail |
| SRV-06 | End-to-end offline-to-online run against local `:server` | PARTIAL | PROGRESS V34: "E2E CLOSED with findings", emulator geo-fix not consumed; never repeated after the L24 toggle | local PROGRESS ~2328 |
| SRV-07 | Real push, payout rail, multi-tenant admin | DROP-PROPOSAL | SPEC.md rank 5, out of scope for a portfolio app | SPEC.md |
| SRV-08 | L24: two backend switches in debug builds, no auto restart, no device smoke | DONE | merged in #259 (`f4549846`, `8133660b`, per RE-MAP.md); device smoke caveat not re-verified | PR #259 body, RE-MAP.md |
| SPEC-02 | Server-driven tenant configuration layer (about 60 switches) | NOT-BUILT | SPEC gap rank 1, size L; weakest evidence (keyword scan) so size it first | SPEC.md |
| SPEC-03 | Sync progress step list, four-state status | NOT-BUILT | SPEC gap rank 16 | SPEC.md |
| SPEC-05 | Tamper/security-violation banner | DROP-PROPOSAL | SPEC marks it optional for a public app | SPEC.md |
| SPEC-15 | Notification channel preferences | NOT-BUILT | SPEC section 9 | SPEC.md |
| SPEC-18 | Hidden multi-press gesture and diagnostics | DROP-PROPOSAL | SPEC says decline | SPEC.md |
| SYN-02 | Mock-heavy repositories behind a disabled real backend | NOT-BUILT | 41 files still reference `MockAccountRepository` / `FakeProfileRepository` / `DemoMockData` / `AgentMockData` | BACKLOG 10-19, SPEC rank 6 |
| SYN-03 | Room v51 for durable travel requests (see TRV-02) | NOT-BUILT | DB is at 50 | PR #235 |
| SYN-04 | Pull-to-refresh on history lists | NOT-BUILT | `PullToRefresh` only in `HomeScreen.kt` and `RoutePointsScreen.kt` | audit-doori item 8 |
| MIG-01 | Migration data-move tests for 32 to 33 (INSERT...SELECT), 47 to 48 (DROP), 5 to 6 and 16 to 17 (FK recreate) | NOT-BUILT | only 48to49 and 49to50 tests (desktopTest) plus the 8 old androidTest files exist; none for the four risky crossings | 2026-08-02-migration-test-audit.md |
| MIG-02 | Write down what happens when the Keystore entry is lost (device wipe, key invalidated) | NOT-BUILT | no doc found | migration-test-audit "Related" |
| ENC-01 | Encryption at rest for the Room database | NOT-BUILT (owner decision) | issue/PR #89 closed 2026-08-06 "unimplemented, needs one decision" (field-level vs full-file); no SQLCipher in app code | tracked FRONTIER ask-first 1 |
| IOS-02 | iOS cold SLC relaunch Koin bootstrap | NOT-BUILT (carried) | L16 report | L16-report.md |
| IOS-03 | iOS map navigation, expense entry/history, feature-detail callbacks, back-stack restore unwired (about 9 of 13 feature modules lack an iOS entry) | PARTIAL | iOS shell landed in #235; rest carried | PR #235, FRONTIER |
| IOS-04 | iOS speech-to-text and text-to-speech TODOs | NOT-BUILT | `feature/agent/src/iosMain/.../IosSpeechToText.kt:7`, `IosTextToSpeech.kt:4` | code scan |
| IOS-05 | App Store Connect API + match signing | NOT-BUILT (owner) | OWNER_ACTIONS 4.1 | OWNER_ACTIONS |
| IOS-06 | Swift code not compiled in the gate | NOT-BUILT | PR #259 note | phase4 PR body |
| WER-02 | V37 phase 4: Wear scope, first cross-feature consumer | NOT-BUILT (owner decision on consumer) | FRONTIER | PLAN_V37 |
| WER-03 | V39 Horologist / Health Services / SKIE | NOT-BUILT (ask-first deps approved in principle 2026-07-26; Horologist datalayer argued against) | no `Horologist` reference on main; V39 phase-0 audit shrank the plan | PLAN_V39 |
| WER-04 | V38 health/rewards (Health Connect, HealthKit, reward unit) | NOT-BUILT (design only, 6 open product questions, blocked on V37 phases 2 to 4) | no `HealthConnect` reference on main | PLAN_V38 |
| WER-05 | `play-wear` GitHub environment | NOT-BUILT (owner) | OWNER_ACTIONS 3.3 | OWNER_ACTIONS |
| WER-06 | Plan-level risk: V39 section 1b adds bandwidth-aware sync scheduling on top of an unconditional outbox; decide which owns scheduling before building | NOT-BUILT (decision) | tracked FRONTIER council finding | tracked FRONTIER |
| UXD-16 | Desktop `MediaCaptureLauncher.desktop.kt:19` and `OdometerOcrService.desktop.kt:9` throw NotImplementedError; `Watermark.desktop.kt` dead | NOT-BUILT | code scan today | code |

### D. Open work: design, accessibility, localisation

| Id | Item | Verdict | Evidence | Source |
|---|---|---|---|---|
| UXD-03 | Per-caller CTAs; delete duplicate `SavedTracksRevamp.EmptyStateBlock`, `WhatsNewEmptyState` | NOT-BUILT (carried) | audit item 3 | audit-doori.md |
| UXD-04 | Slim Home to one hero, one action, one list | NOT-BUILT (needs design call) | audit item 5 | audit-doori.md |
| UXD-05 | Shared shimmer transition | NOT-BUILT (carried) | audit item 6 | audit-doori.md |
| UXD-06 | `OnboardingCarousel` has no call site; permission primer; Compottie 2.3.3 pre-approved | NOT-BUILT | only its own file references it | audit item 11, mobile-uiux DECISIONS |
| UXD-07 | Tokenise motion, colour and elevation (codemod + guard) | NOT-BUILT (carried) | audit item 14 | audit-doori.md |
| UXD-08 | Remove gradient fills from Paper cards (needs before/after render) | NOT-BUILT (carried) | audit item 15 | audit-doori.md |
| UXD-09 | Shared-element transitions; dark-mode pass; MockCardView onPrimary recheck | NOT-BUILT (carried) | never run | BACKLOG 10-19 |
| UXD-12 | PLAN_V22_UI_POLISH: 13 unchecked tasks, 0 checked | PARTIAL (plan file never ticked; some done via #242/#248; re-audit before any work) | `grep -c "\[ \]"` = 13, `"\[x\]"` = 0 | PLAN_V22_UI_POLISH |
| UXD-14 | Snackbar placeholders in attachment selection and track preview | NOT-BUILT | `feature/media/src/androidMain/.../AttachmentSelectionScreen.kt:161,176,185` still call `showSnackbar` | code |
| UXD-15 | PLAN_L10N: 5 module sweeps (logging, tracking x2, shared/core:ui residuals, Hindi backfill) | PARTIAL | plan boxes all unchecked; literal `Text("...")` counts on main: logging 35, tracking 57, shared 9, core:ui 9 | PLAN_L10N |
| UXD-17 | Web demo viewport meta / `user-scalable=no`; `cv/mileway-app` stubs | NOT-BUILT (carried) | github.io audit | BACKLOG 10-19 |
| A11Y-02 | Full TalkBack pass | NOT-BUILT | never run | audit-doori.md |

### E. Open work: CI, release, distribution, docs

| Id | Item | Verdict | Evidence | Source |
|---|---|---|---|---|
| CI-02 | `testGmsDebugUnitTest` suite red for missing Roborazzi goldens (CI does not run it) | NOT-BUILT (carried) | PR #231 | PR #231 |
| CI-03 | Journey / instrumented CI lane | NOT-BUILT (owner decision) | BACKLOG 10-19 | BACKLOG 10-19 |
| CI-04 | Auto-delete merged head branches (repo setting) | NOT-BUILT (owner) | OWNER_ACTIONS 0.2; stale `redo/*` branches prove it is still off | OWNER_ACTIONS |
| CI-05 | `SCREENSHOT_BOT_TOKEN` | PARTIAL | PAT push added in #211/#215; but bot PR #257 still open with stale baselines | OWNER_ACTIONS 1.1 |
| CI-06 | `CRASHLYTICS_UPLOAD` variable | NOT-BUILT (owner) | OWNER_ACTIONS 2.1; also listed in vault Owner Actions as "Doori CI secrets" | OWNER_ACTIONS |
| CI-07 | Signing + Firebase secrets, Play credentials, first manual AAB | NOT-BUILT (owner) | OWNER_ACTIONS 3.1, 3.2; vault `Owner/Owner Actions.md` line 20 | OWNER_ACTIONS |
| CI-08 | Alternative stores (Amazon, Samsung, Huawei, Indus, Aptoide) | NOT-BUILT (owner; workflows exist, never executed) | OWNER_ACTIONS tier 5 | OWNER_ACTIONS |
| CI-09 | F-Droid publish (noGms now has 0 GMS entries) | PARTIAL | prerequisite met by L13; `publish-fdroid.yml` needs signing secrets; docs still say it is blocked | OWNER_ACTIONS 6.1 |
| CI-10 | Verify Play Oct-2026 Transition-API date (uncited) | NOT-BUILT | spec.md owner decisions | suite HANDOFF |
| CI-11 | Verify PaymentsLab-KMP licence; who curates the rate JSON mirrors | NOT-BUILT (owner) | spec.md | suite HANDOFF |
| CI-12 | Confirm Home-address redaction as export default | NOT-BUILT (owner confirm; implemented as default-on in L18a) | spec.md | suite HANDOFF |
| CI-17 | `verifyAll` wired into CI on every push and on a schedule; screenshot freshness assertion; `/Users/` path guard test; failure alert (Wave 0.2 to 0.5) | NOT-BUILT | `verifyAll` is defined in `build.gradle.kts:383` but no workflow under `.github/workflows` calls it | MILESTONE_RELEASE_PROGRAM |
| CI-18 | Milestone release cut after Wave 0 to 3 | PARTIAL | latest tag `v2026.08.35.36.843` (2026-08-25); `MILESTONE` still 36; no release since phases 1 to 4 landed | MILESTONE_RELEASE_PROGRAM Wave 4 |
| CI-19 | Rebrand/presentation council (2.3), retire old surfaces | SUPERSEDED | Mileway to Doori rename + DESIGN.md + README regen cover it (#222, #236) | MILESTONE_RELEASE_PROGRAM |
| CI-20 | Uptodown submission (account, banner, privacy URL); also update asset URL from the old `Mileway` repo name | NOT-BUILT (owner) | `metadata/uptodown-submission.md` "Not done yet" | uptodown-submission.md |
| CI-21 | Lane runner `run.sh launch()` session-id grep bug | NOT-BUILT | lives in the AgentHarness repo, outside Doori | completion HANDOFF |
| DOC-01 | README regeneration for phase 4 | DONE | commits b66030db, 2a632bd4 | completion HANDOFF |
| DOC-02 | Stale docs: OWNER_ACTIONS 6.1 and DECISIONS 2026-06-23 (19 GMS entries), suite PROGRESS lane table (L15 to L25 `queued`), memory `project_doori_full_build` "state as of 2026-09-30", `.ralph/FRONTIER.md` frontier | NOT-BUILT | see action 3 above | multiple |
| DOC-03 | Older DEFERRED_BACKLOG / MASTER_GAP_INVENTORY / V25 to V31 plan files | SUPERSEDED | every item folded into V25 to V32, which PROGRESS records COMPLETE 2026-07-13 | DEFERRED_BACKLOG header |
| DOC-04 | V3 to V24 plans, PLAN.md, SCOPE.md, INVENTORY.md, MASTER_PANEL_ABSORPTION.md | SUPERSEDED | phone parity complete; MASTER_PANEL_ABSORPTION distilled into PLAN_V18 | tracked FRONTIER supersession chain |
| DOC-05 | Nav3 deliberately left as-is | DROP-PROPOSAL (decision, no work) | FRONTIER 2026-07-26 | FRONTIER |
| DOC-06 | CocoaPods trunk read-only 2026-12-02: Doori exposure unchecked | NOT-BUILT (check) | `iosApp` pod usage not inspected | memory note |

### F. Teardown and spec fixes (RE-MAP 2026-11-03)

Evidence file: `RE-MAP.md` (this folder; paths and counts only, no decompiled source is quoted and none may be pasted here). Verdict there: the owner suspicion is true, the teardown in `~/RE/doori-spec` is incomplete and the spec mis-states facts. Ordering rule: items TD-01 to TD-04 come BEFORE sizing phase 5 from the SPEC.md gap table (start-here action 2).

Status corrections found by RE-MAP and applied above: TRK-11 DONE, TRK-13 (SPEC-04) DONE, SRV-08 DONE; EXP-10, EXP-16, EXP-19 and TRK-09 are merged in #259 (shown via SHP-05, EXP-20 to EXP-22, TRK-10); SHP-20 downgraded DONE to PARTIAL. SPEC-13 (bubble service) exists and SPEC-18 is guarded (see TD-11).

| ID | Item | Verdict | Done-test | Source |
|---|---|---|---|---|
| TD-01 | Complete decompile on a scratch copy of app.apk into a new versioned dir (`jadx --no-res -j 2 -d <new-dir> <scratch>/app.apk`), keep the old folder, repoint the spec. Est. 6 to 10 min on an idle machine; state wall time first (load was 14.6, swap 6.5 of 8 GB when checked) | NOT-BUILT | rerun of `dex_coverage.py` on the new output shows ordinary missing class definitions near zero (was 7,168 of 13,604 missing) | RE-MAP fix 1 |
| TD-02 | Record which engine wrote jadx-out (inspect.md says jadx-headless-mcp 0.7.1 via rea) and keep the exact command beside it | NOT-BUILT | a README in the teardown dir names engine, version and command | RE-MAP fix 2 |
| TD-03 | Explain progress total 32,144 vs final marker 22,756 (70.8 percent); confirm whether the run stopped early | NOT-BUILT | one paragraph in the teardown README gives the cause | RE-MAP fix 3 |
| TD-04 | Put a decompiled LogMilesViewModel into the main tree (absent from `jadx-out/sources/.../logMiles/viewmodel`); until then cite `single/LogMilesViewModel.java` as a separate run | NOT-BUILT | the file exists under the new tree with no JADX failure markers on the Log Miles methods | RE-MAP fix 4 |
| TD-05 | SPEC s0: signing is v2 and v3, not v2 only | NOT-BUILT | `grep -n "v2 scheme only" SPEC.md` = no hit | RE-MAP fix 5 |
| TD-06 | SPEC s0: 97 app-owned activities (111 declared incl. library), or state the counting rule | NOT-BUILT | `grep -n "about 90" SPEC.md` = no hit | RE-MAP fix 6 |
| TD-07 | SPEC s0 and s8: remove "525 locale variants" or record the method (116 values dirs, 104 language codes, 125 aapt2 config lines) | NOT-BUILT | `grep -n "525" SPEC.md` = no hit or a method note | RE-MAP fix 7 |
| TD-08 | SPEC-02: "about 60 switches" unsupported; s4.2 lists 37 bullets (about 41 split). State that count or drop the number (also edit SPEC-02 row above) | NOT-BUILT | `grep -n "about 60" SPEC.md BACKLOG.md` = no hit | RE-MAP fix 8 |
| TD-09 | SPEC s5: replace "169 reads and 128 writes" with "169 and 128 uses of two annotations" until verbs are confirmed and the 9 other uses accounted for | NOT-BUILT | `grep -n "169 reads" SPEC.md` = no hit | RE-MAP fix 9 |
| TD-10 | SPEC s7: "on-device AI binding" is not in uses-permission; name where declared or drop it | NOT-BUILT | the phrase is either sourced to a manifest entry or removed | RE-MAP fix 10 |
| TD-11 | Refresh the "Doori lacks it" table and s1.1/s7 "remaining differences" for drift (TRK-11 wired, SPEC-04 built, EXP-10/16/19, TRK-09, SRV-08 merged, SPEC-13 exists, SPEC-18 guarded) | NOT-BUILT | no SPEC row lists a now-built item as missing | RE-MAP fix 11 |
| TD-12 | Add the spec-silent teardown features (import flow, MilesIntro, cab booking tracking, mileage AI analyzers, mileage debug screens, mileage work jobs) as a SPEC section or record them as out of scope | NOT-BUILT | SPEC has the section or an explicit out-of-scope list | RE-MAP fix 12 |
| TD-13 | Re-baseline the 2026-10-19 BACKLOG snapshot to main 91af9ccc; correct SRV-02 line refs | PARTIAL | this file is the re-baseline (statuses above updated 2026-11-03); SRV-02 now cites `ReportRoutes.kt:250`; the old 10-19 BACKLOG is marked superseded | RE-MAP fix 13 |
| TD-14 | Provenance, OWNER RULING PENDING: Doori main has "reference app" text in 156 lines across 117 tracked .kt files (e.g. `PluginCatalog.kt` near 192 and 322-323; `LocationDataSyncer.kt` near 96). Provenance rule (suite HANDOFF lines 299-305) says Doori code is written fresh. Comments are not code, and it is undetermined whether "reference app" means the teardown APK | DROPPED by owner ruling 2026-11-03 | the "reference app" comments stay; no reword PR | RE-MAP fix 14 |
| TD-15 | Teardown hygiene: delete or fix empty `jadx-out/endpoints.txt`; keep `endpoints_all.txt` as the source of the 306 count and note it in the teardown README | NOT-BUILT | `endpoints.txt` gone or non-empty; README names the 306 source | RE-MAP fix 15 |
| TD-16 | Verification gap: search the debug screens for the three-press gesture before SPEC states it | NOT-BUILT | SPEC statement carries a code citation or is removed | RE-MAP fix 16 |

Owner decision added: (12) rule on TD-14 ("reference app" comments), and widen decision 10 (where `~/RE/doori-spec` lives) to whether the teardown-derived spec and RE-MAP.md belong in a tracked folder at all (the critic flagged that `plans/mileway/` is not covered by the employer-material ignore patterns in DATA_CONTRACT.md).

## Dangling work

Branches (remote, `git ls-remote --heads origin`):
- `origin/redo/L20` (96f16b3a) and `origin/redo/L25` (0d44c122): 1 commit each, 31 behind main. The 10-20 HANDOFF says they were deleted; they are not. They are superseded by L20/L25 merged in #259. Delete.
- `origin/bot/refresh-screenshot-baselines` (11ab3bdc, 2026-10-27) = PR #257, 122 changed files, made before #259 merged. Re-record or close.
- `origin/chore/bump-kmp-toolkit` (1df6d982, 2026-11-02) = PR #258, all checks green, mergeable. Merge.
- Five Dependabot branches = PRs #250 to #254 (firebase-bom 35 is a major bump; navigation-compose rc01 is a pre-release).

Worktrees:
- `/Users/darkpandawarrior/Repos/Android/Doori/.worktrees/kmp-toolkit-plain-location` (1.6 MB, files dated 2026-07-18): orphan. Its `.git` file points to `/Users/darkpandawarrior/Repos/Mileway/.git/modules/external/kmp-toolkit/worktrees/kmp-toolkit-plain-location`, a path that no longer exists, so `git status` errors ("gitfile does not point to a valid repository"). `git worktree list` in both Doori and `external/kmp-toolkit` does not know it. Its branch `feat/plain-location-tracker` is already an ancestor of toolkit `origin/main`. The Offload plan in the vault (line 130) already flagged it as "unknown, left alone". Safe to delete after the owner confirms; nothing in it is unique.
- No other Doori worktrees. `Doori-wt-toolchain` and `Mileway-wt-*` are gone.

Local-only items:
- `external/kmp-toolkit` is on detached HEAD 59dee39 (the pinned commit). Local toolkit branch `chore/toolchain-alpha08-kotlin-rc` (54c39b5, has a remote) is not in toolkit main; DROP-PROPOSAL, the toolchain moved past it (#76 in toolkit).
- Local toolkit branches `main`, `feat/plain-location-tracker`, `feat/geopoint-motion-fields` are all ancestors of `origin/main`; `git branch -d` candidates.
- Dirty files: none (`git status -sb` clean). Stashes: none.
- Gitignored machine-local clutter inside the Doori checkout: `gate.log` (329 KB, 2026-09-06), `.ralph/*.log` build logs (about 700 KB), `.superpowers/sdd/` (task-5 diffs, work done), `MILESTONE`/`VERSION`/`BUILD_NUMBER` are tracked and fine.
- `~/.local/state/agent-lanes/L*` lane state dirs for finished lanes L3 to L36: stale; safe to prune once nothing resumes them.
- Plans folder: `plans/mobile-uiux-2026-10/doori-rebaseline-recreate.patch` is a 22-line patch to `.github/workflows/dependabot-rebaseline.yml` (adds a `RECREATE_TOKEN` step). `git grep RECREATE_TOKEN` finds nothing on main, so it was never applied. Either land it as a PR or drop it; a parked patch is how work gets lost.
- `~/RE/doori-spec` (530 MB, outside git): the only content of `~/RE`. Contents: `app.apk`, `apktool-out`, `jadx-out`, `inspect.md`, `strings.tsv`, `cert.der`, `single`. It is the decompiled input to SPEC.md. The Offload plan row D6 asks the owner to pick a destination. Keep it out of every git repo (provenance rule); SPEC.md is the only derived artefact allowed in plans.

## Sources consolidated

- `plans/mileway/RE-MAP.md` (2026-11-03): KEEP, evidence file for section F. Its fix list items 1 to 16 are TD-01 to TD-16.
- `plans/mileway/2026-10-19-doori-completion/BACKLOG.md`: superseded by this file (re-baselined to main 91af9ccc).

Each source is tagged: **FULL** = every item now appears in this table, so it can be marked superseded by this file; **KEEP** = still the live reference for something this table only points at.

| Path | Status |
|---|---|
| `plans/mileway/2026-10-19-doori-completion/BACKLOG.md` | FULL (this file re-verifies and supersedes it; ids kept) |
| `plans/mileway/2026-10-19-doori-completion/HANDOFF.md` | FULL for status; mark superseded, but append one line "see BACKLOG.md" |
| `plans/mileway/2026-10-19-doori-completion/SPEC.md` | KEEP (source for SPEC-nn behaviour detail; this file carries the gap table) |
| `plans/mileway/2026-10-19-doori-completion/lanes/*` (L20, L25 reports, adapter) | FULL (lanes merged in #259) |
| `plans/mileway/2026-09-25-doori-suite/HANDOFF.md`, `PROGRESS.md`, `EXECUTION-STATUS.md` | FULL; mark superseded (PROGRESS lane table is wrong for L15 to L25) |
| `plans/mileway/2026-09-25-doori-suite/spec.md`, `master-plan.json`, `backend-*.md` | KEEP as design record; SRV-04/09 point at `backend-golden-gate.md` |
| `plans/mileway/2026-09-25-doori-suite/session-kit/*` | FULL (briefs and reports of merged lanes; keep as audit trail only) |
| `plans/mileway/FRONTIER.md`, `2026-07-24-FRONTIER.md`, `.ralph/FRONTIER.md` | FULL; superseded by this file (the three disagree with each other) |
| `plans/mileway/PROGRESS.md`, `PROGRESS_V33.md`, `.ralph/PROGRESS.md` | KEEP as history only (PROGRESS records intent as fact; FRONTIER already warns) |
| `plans/mileway/PLAN.md`, `SCOPE.md`, `INVENTORY.md`, `MASTER_PANEL_ABSORPTION.md`, `PLAN_V3` to `PLAN_V20`, `.ralph/PLAN_V21` to `V32`, `PLAN_V25_ONWARDS_MASTER.md`, `DEFERRED_BACKLOG.md`, `MASTER_GAP_INVENTORY.md` | FULL; superseded (complete or folded; DOC-03, DOC-04) |
| `PLAN_V33_MASTER.md`, `2026-07-16-PLAN_V34_CLOSEOUT.md` (+ `PLAN_V34_CLOSEOUT.md` copies, which differ), `2026-07-16-PLAN_V35_HOME_CARDS.md`, `.ralph/PLAN_V36_WHATSNEW.md`, `SPEC_V36_WHATSNEW.md` | FULL; complete |
| `2026-07-18-PLAN_V37_DEVICE_SENSORS.md`, `2026-07-18-PLAN_V38_HEALTH_WEARABLES_REWARDS.md`, `2026-07-24-PLAN_V39_WATCH_HOROLOGIST_HEALTH.md` | KEEP (design detail for TRK-05..07, WER-02..06); status carried here |
| `.ralph/PLAN_L10N.md`, `PLAN_V22_UI_POLISH.md` | KEEP until UXD-12 / UXD-15 are re-audited |
| `2026-07-10-compose-resources-android-assets-fix.md` | FULL (SHP-11 done) |
| `2026-08-02-migration-test-audit.md` | KEEP (test recipe for MIG-01/02) |
| `MILESTONE_RELEASE_PROGRAM.md` | KEEP until CI-17 and CI-18 close |
| `architecture-references.md`, `kmp-migration-map.md`, `navgraph-agent-guide.md`, `bubble-bottombar/`, `dice-inventory/`, `dice-reference/`, `design/` | Reference material, not plans; no open items; leave in place. `bubble-bottombar/` and `design/` ideas are in the app and DESIGN.md |
| `.ralph/REFERENCE.md`, `leak-patterns.txt`, `build_*.log`, `v21-v22-runner.mjs` | Machine-local, gitignored; not plans. Clean up with the gitignored clutter |
| Repo `docs/OWNER_ACTIONS.md` | KEEP (live owner checklist; fix 6.1) |
| Repo `docs/DECISIONS.md` | KEEP (fix the 2026-06-23 line) |
| Repo `docs/ARTEFACT_INDEX.md`, `docs/RELEASE.md`, `DESIGN.md`, `MILESTONE`, `metadata/uptodown-submission.md` | KEEP (live reference; uptodown file feeds CI-20) |
| Repo `.superpowers/sdd/` | FULL (task 5 done, SHP-12); gitignored, deletable |
| `plans/mobile-uiux-2026-10/audit-doori.md`, `DECISIONS.md`, `doori-rebaseline-recreate.patch` | KEEP audit and decisions; patch is dangling work |
| Vault notes (Branches to decide, Offload plan, Lanes, Projects, Owner Actions, Past sessions synthesis, Home, Identity map) | Pointers only. Their Doori content: Offload plan flags the orphan worktree and `~/RE/doori-spec`; Owner Actions line 20 = CI-06/CI-07; Home lists PRs #257/#258; Past sessions synthesis line 66 = V37 to V39 design-only. No separate plan items |

## Owner decisions needed

1. Merge #258 now? (green, mergeable) and re-record or close #257 (bot baselines made before phase 4).
2. firebase-bom 34 to 35 (#251) and navigation-compose rc01 (#252): accept, or leave Dependabot ignoring them.
3. Approve deleting `origin/redo/L20`, `origin/redo/L25` and the orphan `.worktrees/kmp-toolkit-plain-location`.
4. ML Kit / FOSS: noGms is now GMS-free (L13), so is F-Droid back on (CI-09), with OCR coming from a non-ML-Kit path, or is the F-Droid track dropped? The docs still pose the old three-way choice.
5. Encryption at rest (ENC-01): field-level vs full-file database encryption (full-file adds a dependency).
6. V37 to V39 health/wear line (TRK-05..07, WER-02..06): proceed (answer HAR model, second consumer, standalone GPS, the 6 V38 questions) or park the whole line. It is the largest block of unbuilt design with no owner answer.
7. `useRealBackend` production flip (SRV-05) and whether to invest in SRV-02/03/04 server round-trip at all, or keep the backend as a demo (SRV-07 proposed drop).
8. Cut a milestone release now (CI-18) and which owner secrets (CI-06/07) to create first.
9. Rate-JSON curator and PaymentsLab-KMP licence check (CI-11); confirm Home-address redaction default (CI-12).
10. Where `~/RE/doori-spec` should live (Offload plan row D6), or delete after SPEC.md review.
11. Confirm DROP-PROPOSALs: TRV-05, SRV-07, SPEC-05, SPEC-18, DOC-05.
12. Rule on TD-14: does "reference app" in Doori comments mean the teardown APK (reword 156 lines) or not? RULED 2026-11-03: see Owner ruling 2026-11-03 below.
13. Rule whether the teardown-derived spec and `RE-MAP.md` stay in tracked `plans/mileway/` (see section F). RULED 2026-11-03: they stay.

### Owner ruling 2026-11-03 (decisions 12 and 13, TD-14)

- The write-fresh provenance rule is overturned. Doori may reference the teardown app openly and build on it.
- The "reference app" comments stay. TD-14 is DROPPED by ruling.
- `RE-MAP.md` stays in `plans/mileway/`.
- Note: reimplement in Doori's own architecture. Never paste decompiled code verbatim into the public repo.

## Not determined

- Whether the "(carried)" items are still open: only the ones with a code citation above were re-checked on 2026-11-03.
- APR-02 (undo on approve): the check errored; not confirmed either way.
- Server-side policy outcomes, mileage rule defaults and 3 large ViewModels in the employee-app spec were not statically readable (SPEC.md caveat), so SPEC-nn sizes are estimates.
- GitHub Actions run history for the 5 alt-store workflows (never executed per OWNER_ACTIONS).
