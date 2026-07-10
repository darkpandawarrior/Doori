# Changelog

All notable changes to **Mileway** — an offline-first Kotlin Multiplatform + Compose Multiplatform
mileage / travel / expense tracker for Android, iOS and Wear OS.

Versions map to the project's phased development milestones (`v0.N.0` = milestone N). Dates are
approximate for early retrospective entries; the app is built through versioned, individually-
revertable phases. This file is the browsable "what happened when" — each entry also has a matching
[GitHub Release](https://github.com/darkpandawarrior/Mileway/releases).

The format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased] — v0.24.0 · Super Profile (in progress)
A super-profile build-out: identity capture, verification & KYC flows, membership/subscription and
incentive surfaces, account lifecycle & session depth, vehicle garage with per-km rates, badges,
support hub, review prompting, self-audit, digital signature, favourites, offers, and a unified
banner/announcement system — all plugin-gated so different user personas see visibly different apps.

## [v0.23.0] — Watch platform build-out
Wear OS app, tile and complication on a shared KMP core; Apple watchOS umbrella framework and app
target; groundwork for home/lock-screen widgets across both platforms.

## [v0.22.0] — Account, session & profile depth
Deeper account lifecycle, multi-session management and delegation, and a richer profile surface.

## [v0.21.0] — Expense, bulk-expense, advance, log-miles & track-miles parity
Category catalogs, per-field validation, receipt attachments, tiered policy checks, draft
persistence, multi-row bulk entry with CSV import, voucher lifecycle, and advance requests.

## [v0.20.0] — On-device AI assistant
A KMP-correct, Room-persisted, data-grounded offline assistant with streaming and voice — replacing
the earlier Android-only prototype. A cloud-model seam is left in place but intentionally unwired.

## [v0.19.0] — Mileage feature: full Android + iOS parity
Common-first mileage feature with platform actuals behind clean interfaces; iOS reaches parity with
Android for tracking, camera/OCR and lifecycle.

## [v0.18.0] — Master-panel integration
Absorbed verified capability gaps surfaced by an engineering-braindump audit into the app.

## [v0.15.0] — Platform services, release pipelines & app-grade utilities
Review/update gating, deep-link routing, notification channels, and the CI/quality/release/store-
distribution workflow scaffolding.

## [v0.13.0] — Production-grade consolidation
The definitive consolidation pass: camera controls, multi-pass OCR, and a stable production baseline.

## [v0.12.0] — MVI clean-architecture migration + production tracking engine
Migration to a single-state MVI architecture across features, the production location/tracking
engine, and the full media pipeline.

## [v0.10.0] — Compose navigation graph + exhaustive previews
Nav-graph-based navigation and a broad, diverse Compose preview catalog.

## [v0.9.0] — Platform completeness
OCR, offline maps, multi-account support, CI, and the first Wear OS surface.

## [v0.8.0] — Production depth
Missing screens filled in, real services wired, plus performance and testing passes.

## [v0.7.0] — Mileage UX parity (deep fidelity)
A deep-fidelity pass bringing the mileage tracking UX to parity with the reference baseline's flows.

## [v0.6.0] — Full polish + platform features
Broad UI polish and additional platform-level features.

## [v0.5.0] — AI-centric UI makeover
A UI makeover foregrounding the assistant/AI surfaces.

## [v0.4.0] — Next iteration
Iterative feature and polish work on the demo baseline.

## [v0.3.0] — Full feature & polish execution
The first broad feature-and-polish execution pass over the offline mileage baseline.
