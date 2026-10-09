# Doori DESIGN.md

> Inherits: house design standard (AgentHarness skill `design-md`). This file wins on conflict.
> Also inherits the shared token layer: kmp-toolkit `DESIGN.md` (present in `external/kmp-toolkit` once its pin includes it: 4dp rhythm, adaptive
> ladder, motion buckets). This file records only what Doori adds or overrides.
> Agents: read this before creating or changing UI. Values live in
> `core/ui/src/commonMain/kotlin/com/mileway/core/ui/theme/` (the code still uses the old
> `Mileway*` symbol names; the product is Doori).
> Dial: ENERGY 3 / RHYTHM 4 / MOTION 2

## Overview

Doori turns a recorded drive into a mileage reimbursement claim. The people who read that record
are the employee who drove, the manager who approves, finance who pays, and sometimes an auditor.
So the default direction is Paper: a printed expense sheet, warm off-white surfaces, ink-navy
accent, serif titles, monospace only for the number. It was chosen on 2026-08-10 from a
five-direction comparison and is the only direction with a hand-built opposite-luminance face, so
it follows the device and a night driver never gets a white page (`MilewayThemeVariant.DEFAULT`).

Ten selectable variants exist (Ember, Matrix, Amoled, Ion, Daybreak, Instrument, Signal, Ledger,
Paper, Refined Ember). The picker lists them; Paper is the product identity. Rationale for the
layers is in `core/ui/.../theme/LAYERS.md`; release rules in `docs/RELEASE.md`.

## Colors

Colour is meaning, in three layers (see `LAYERS.md`):

1. Base: the direction, a `MilewaySchemeSpec` mapped to the M3 `ColorScheme` by `toColorScheme`.
2. Semantic: `MilewayRoles.*` (money, distance, approved, pending, rejected, policyViolation,
   offlineQueued, activeTracking, destructive, informational, inactive, premium).
3. Domain: optional per-feature accent tint via `MilewayDomainTheme`; it may tint the accent, never
   change what "approved" or an amount looks like.

A screen asks for "pending", never for amber.

Paper light (`direction/PaperDirection.kt`, `PaperSpec`), mapped to M3 roles:

| M3 role | Value | Spec field |
|---|---|---|
| background | #F7F3EA | canvas |
| surface | #FFFFFF | surface |
| surfaceVariant | #F1ECDF | surfaceRaised |
| outline | #DDD3B8 | border |
| onSurface | #1F1B14 | text |
| onSurfaceVariant | #6E6353 | textMuted |
| primary | #1E3A5F | accent |
| onPrimary | #FFFFFF | onAccent |
| primaryContainer | #D9E3EE | accentContainer |
| onPrimaryContainer | #0F2338 | onAccentContainer |
| secondary | #14293F | accentDim |
| tertiary | #1D5FA8 | info |
| error | #A3291E | danger |

Semantic extras: warning #8A5A0A, success #1B7A43. Money role (`PaperColors.money`): #3B6E4E,
deliberately a different hue from success so a reimbursement figure never reads as a status tick.

Paper night (`PaperNightSpec`): background #14120E, surface #1C1912, surfaceVariant #241F16,
outline #3A3324, onSurface #F1E9D8, onSurfaceVariant #AA9F89, primary #8FB4E0, onPrimary #0C1B2C,
primaryContainer #223349, error #E2685C, warning #E0A64B, success #5FBE84, info #6FA6DD, money
#8FC9A0. Night is hand-tuned, not an inversion. `useGlow = false` in both faces.

Ember, the earlier signature (`MilewayThemes.kt`, `EmberSpec`): canvas #0B0806, surface #17110B,
accent #F5A623, text #F7EFE3, danger #FF453A. The Wear OS theme currently mirrors Ember
(`wear/src/main/kotlin/com/mileway/wear/theme/WearMilewayTheme.kt`).

Contrast target is WCAG AA 4.5:1 for text. The legacy `StatusGreen`, `StatusRed` and
`DesignTokens.StatusColors` are deprecated and theme-blind; do not use them.

## Typography

Fonts are platform defaults only (`FontFamily.Serif`, `Default`, `Monospace`); no bundled font.
Paper (`PaperTypography`): Serif for headlineLarge 32 Bold, headlineMedium 28 Bold, headlineSmall
24 SemiBold, titleLarge 22 SemiBold. Sans (`Default`) for titleMedium 16, titleSmall 14, bodyLarge
16 / 24, bodyMedium 14 / 20, bodySmall 12 / 16, labelLarge 14, labelMedium 12, labelSmall 11.
There is no monospace entry in the scale. Numbers get monospace through
`TextStyle.dataStyle()` or `MilewayType.dataLarge/Medium/Small` (`Type.kt`): distances, currency,
odometer and reference IDs, tabular timestamps. Mono on chrome is the flaw that made the app read
as a terminal; the old house scale `MilewayTypography` (mono titles and labels) is for the
Ember family only.

## Layout

`DesignTokens.Spacing` (`DesignTokens.kt`): xs 4, s 8, m 12, l 16, xl 24, xxl 32; screenHorizontal
16, carouselSpacing 12, sectionSpacing 24. Icons: inline 16, badge 18, navigation 20, header 24,
min touch target 48. Action tiles: container 52 (circular 56, compact 44), width 72. Header
heights: gradient 100, compact 56. Adaptive ladder from the toolkit applies on tablet, desktop and
web. Top bars depend on navigation depth (`NavigationDepth`): gradient only at ROOT.

## Elevation & Depth

Paper uses real shadows, never glow: `PaperElevation` resting 1, raised 3, prominent 6 dp.
Ember-family variants (`useGlow = true`) raise surfaces with a light edge instead. The generic
scale is `DesignTokens.Elevation`: card 2, raised 4, prominent 8. A driving screen should never
glare, day or night.

## Shapes

Paper (`PaperShapes`, an index-card radius): extraSmall 4, small 6, medium 8, large 10,
extraLarge 10 dp. House scheme for the other variants (`MilewayTheme.kt`): 8, 10, 12, 16, 16.
`DesignTokens.Shape`: button 12, chip 14, actionTile 14, roundedMd 16, carouselCard 18, roundedLg
20, sheet top 28 (sheetSquared 12). Buttons always pass `DesignTokens.Shape.button` explicitly.

## Components

Screens compose Material 3 components from `MaterialTheme`; shared pieces live in `core/ui`.
Wrap screens in `MilewayTheme { }` (or `PaperTheme { }` in isolated renders), and tint a feature
with `MilewayDomainTheme`. Reference renders: `docs/screenshots/`. Web marketing page:
`site/index.html`. Wear and widget surfaces have their own theme files; reuse the spec values.

## Motion

`DesignTokens.Motion`: INSTANT 0, QUICK 120, STANDARD 220, DELIBERATE 400 ms, with
`FastOutSlowInEasing` standard, `LinearOutSlowInEasing` decelerate, `FastOutLinearInEasing`
accelerate. Motion is quiet; nothing animates just to decorate a number.

## Do's and Don'ts

- Do ask for a role (`MilewayRoles.approved`) or an M3 `colorScheme` role, never a hex.
- Do keep serif for screen and card titles, sans for chrome, monospace for the figure only.
- Do check both Paper faces; the night face is a separate design.
- Do use the primary colour for the single main action on a screen.
- Don't add a raw `Color(0x...)` outside the theme package (313 existed when `LAYERS.md` was written).
- Don't read deprecated `Status*`, `Track*` or `DesignTokens.StatusColors`.
- Don't use glow, gradient fills or neon on Paper; depth is a shadow.
- Don't use the same hue for money and for success.
- Don't put monospace on buttons, tabs or labels.

## Agent notes

- Symbol names still say Mileway (`MilewayTheme`, `MilewayRoles`, `MilewayThemeVariant`); the
  rename to Doori deliberately left code and packages alone. Say Doori in user-facing copy.
- The code wins on any value here. If a hex differs, fix this file.
- New colour: pick its layer first (`LAYERS.md`, "Which layer does a new colour belong in").
- Run the `antislop` skill as the filter on any UI diff and report its Delivery Gate result.
