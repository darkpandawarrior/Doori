# Mileway theme layers — the rule, and the migration map

Mileway turns a recorded drive into a mileage reimbursement claim. That document is read by the
employee who drove, the manager who approves, finance who pays, and possibly a tax auditor. Colour
in this app is not decoration — it is how those four people tell an approved claim from a pending
one at a glance. It has to mean the same thing on every screen, and it has to survive a change of
design direction.

It currently does neither. 313 raw `Color(0x…)` literals live outside this package. `ApprovalsScreen`
hard-codes `Brush.horizontalGradient(Color(0xFF6C63FF), Color(0xFF9C6BFF))`, so the approvals top bar
renders the same purple under all ten themes; the live-drive stop button is the same red under all
ten. The most contested screen in the app is theme-blind. That is why picking a design direction
only ever half-applies.

---

## The three layers

```
┌─ 1 BASE ─────────── the design direction. Ledger / Signal / Paper / Instrument / Refined Ember /
│                     Ember / Matrix / Amoled / Ion / Daybreak.
│                     Canvas, surfaces, tonal steps, borders, text, type scale, the accent ramp.
│                     Everything inherits this.        MilewaySchemeSpec → MaterialTheme.colorScheme
│
├─ 2 SEMANTIC ─────── what a colour MEANS in this product. money, distance, approved, pending,
│                     rejected, policyViolation, offlineQueued, activeTracking, destructive,
│                     informational, inactive, premium.
│                     Derived from Layer 1. A screen asks for "pending", never for amber.
│                                                        MilewayRoleColors → MilewayRoles.*
│
└─ 3 DOMAIN ───────── an OPTIONAL per-feature accent overlay: tracking, approvals, expenses,
                      payables, cards, travel. A hue rotation + chroma scale over Layer 1's accent,
                      at unchanged tone. Cannot name a colour.
                                          MilewayDomain → MilewayDomainTheme { } → colorScheme.primary
```

### Which layer does a new colour belong in?

Answer in order. Stop at the first "yes".

| Ask | If yes | Where it goes |
|---|---|---|
| Is it a surface, canvas, border, divider, or body/label text? | It is chrome. | **Layer 1** — `MaterialTheme.colorScheme.surface/onSurface/outline/...`. Never a new token. |
| Does it express one of the twelve meanings above? | It is a role. | **Layer 2** — `MilewayRoles.<role>`. |
| Is it a near-miss of a role ("a slightly different green for verified vs approved")? | It is the same role. | **Layer 2**, the existing role. Two greens is how you get 290. |
| Does a whole feature need to feel different from its neighbour? | It is identity. | **Layer 3** — wrap the feature's nav entry in `MilewayDomainTheme(MilewayDomain.X)`. |
| Is it one screen that wants to feel special? | No. | Nothing. A screen is not a layer. |
| Is it brand/marketing artwork, a third-party logo, or a QR code's required black? | It is an asset. | A raw literal is correct — comment why. |

**A thirteenth role is a real decision, not a shortcut.** It means the product grew a meaning it did
not have. Add it to `MilewayRoleColors`, derive it in `roleColorsFrom`, and it lands on all ten
directions at once. If you cannot write one sentence saying what it means to an auditor, it is not
a role.

---

## Layer 1 — BASE

Already built. `MilewaySchemeSpec` (in `MilewayThemes.kt`) declares one direction's full palette;
`MilewayThemeVariant` lists the ten; `MilewayTheme` resolves one and provides it as a Material 3
`ColorScheme` plus the direction's type scale.

Read it as `MaterialTheme.colorScheme.*`. Raw hexes are allowed **only** inside this package — that
is what this package is for.

## Layer 2 — SEMANTIC

`MilewayRoles.kt`. Twelve roles, derived once in `roleColorsFrom` from the six colours a direction
already declares (`accent`, `accentGlow`, `warning`, `danger`, `info`, `success`) plus its muted
text tone.

Derived, not hand-tuned: ten directions × twelve roles would be 120 hand-picked hexes — the 290-colour
problem rebuilt inside `theme/`. A direction that genuinely disagrees overrides exactly one field
via `spec.roleColors().copy(pending = …)`.

```kotlin
Text(amount, color = MilewayRoles.money)
Icon(Icons.Default.CheckCircle, null, tint = MilewayRoles.approved)

Surface(color = MilewayRoles.tint(MilewayRoles.pending)) { Text("Awaiting approval") }   // tinted ground
Button(colors = ButtonDefaults.buttonColors(
    containerColor = MilewayRoles.destructive,
    contentColor = MilewayRoles.onFilled(MilewayRoles.destructive),                        // solid fill
)) { Text("Stop") }
```

`MilewayRoles.tint(role)` is the **only** sanctioned tinted background. The old code carried the same
idea at `.copy(alpha = 0.10f)`, `0.12f`, `0.15f`, `0.16f` and `0.35f`; every status chip now has one
weight. Do not write `.copy(alpha = …)` on a role.

### The twelve roles

| Role | Means | Use for |
|---|---|---|
| `money` | a monetary figure | amounts, totals, reimbursable value, balances |
| `distance` | measured distance / duration / route | odometer readings, trip length, the map polyline |
| `approved` | terminal-good | approved, verified, settled, passed, active subscription, earned badge |
| `pending` | in flight, blocking someone | awaiting approval, under review, processing, uploaded-not-verified |
| `rejected` | terminal-bad, a person decided | rejected, failed, expired, declined, cancelled |
| `policyViolation` | the policy engine flagged it, nobody has ruled yet | over-limit claim, missing receipt, out-of-policy hotel |
| `offlineQueued` | captured locally, not yet synced | outbox items, queued writes, "will sync when online" |
| `activeTracking` | a recording is running right now | live-drive pulse, active timer, capturing indicator |
| `destructive` | this action ends or destroys something | stop recording, delete, close account, SOS |
| `informational` | neutral, non-blocking | hints, tips, "processing" notices, info icons |
| `inactive` | not started / disabled / draft | drafts, disabled controls, unearned badges, completed-and-archived |
| `premium` | value the user unlocked | paid tier, corporate card, club benefit, reward |

**`rejected` vs `destructive`** is the distinction the old code lost. `rejected` labels a *state* a
claim is in. `destructive` colours an *affordance the user can press*. The live-drive stop button is
`destructive` — it is not an error, and it is not `activeTracking` either. The pulse around it is
`activeTracking`; the button that ends it is `destructive`.

## Layer 3 — DOMAIN

`MilewayDomain.kt`. Wrap a feature's entry point **once**, in the nav graph — not per screen, never
per component:

```kotlin
composable<Approvals> { MilewayDomainTheme(MilewayDomain.APPROVALS) { ApprovalsScreen(…) } }
```

Everything inside then picks the domain accent up through `MaterialTheme.colorScheme.primary`, which
M3 components, `DesignTokens.topBarGradientBrush()` and `DesignTokens.topBarContainerColor()` already
read — so the overlay lands on existing call sites with no edits. `ApprovalsGradientHeader` becomes
`background(DesignTokens.topBarGradientBrush())` and the hard-coded purple disappears.

A domain declares only a **hue rotation and a chroma scale**. It cannot name a colour. The accent is
recomputed from the active direction's accent in HCT at **unchanged tone**, which means:

- switching design direction moves every domain (approvals under Paper is a Paper colour);
- contrast against the direction's surfaces is preserved by construction, not by re-verification;
- there is no field a future PR can set to a hex.

A domain overlay touches the **accent ramp only** — `primary`, `secondary`, `primaryContainer`,
`onPrimaryContainer`, `inversePrimary`, `surfaceTint`. Surfaces, canvas, type and every Layer-2 role
are untouched: `approved` is the same green in payables as in approvals, and an amount reads
identically on both, because a reader compares those across screens. `ThemeLayersTest` asserts all
of this for every domain × every direction.

`TRACKING` is deliberately an identity overlay — the live drive *is* the brand moment, so it renders
in the unmodified direction accent.

---

## MIGRATION MAP

Translate against this table. Do not invent a role name — five agents inventing five vocabularies is
how the app got 313 literals. If a value you hit is not listed, match it to the nearest row by
**meaning**, not by hue, and say so in your commit.

Every replacement below is read inside a composable. If the call site is not composable (a top-level
`private val`, an enum default, a data class field), hoist the colour into the composable that draws
it — that is usually the real fix, and it is why so many of these ended up hard-coded.

### Greens → `MilewayRoles.approved`

`0xFF16A34A` (26×) · `0xFF4CAF50` (5×) · `0xFF22C55E` (3×) · `0xFF12B76A` (2×) · `0xFF2E7D32` ·
`0xFF388E3C` · `0xFF43A047` · `0xFF46C46B` · `0xFF00E676` · `0xFF69F0AE` · `0xFF8BC34A` ·
`0xFF1E9E6A` · `0xFF1AB090` · `0xFF3DDC84`

Verified documents, approved POs/claims, passed self-audits, active subscriptions, earned badges,
"Rewarded" referrals, savings copy, success ticks. All one role.

*Exception:* a green used as a **feature's identity** (not a status) is Layer 3 — see the teals below.

### Reds → `MilewayRoles.rejected` (state) or `MilewayRoles.destructive` (action)

`0xFFB91C1C` (12×) · `0xFFDC2626` (4×) · `0xFFEF4444` (2×) · `0xFFF44336` (2×) · `0xFFD32F2F` (2×) ·
`0xFF7F1D1D` (2×) · `0xFFFF5252` (2×) · `0xFFB71C1C` · `0xFFBA1A1A` · `0xFFF2545B` · `0xFFFF453A` ·
`0xFFC62F3B` · `0xFFCE3B3B` · `0xFFE64A19`

Decide per call site:

| Call site | Role |
|---|---|
| status label / chip: "Rejected", "Expired", "Failed", "Incomplete" | `rejected` |
| button, FAB, icon-button the user presses: stop, delete, SOS, "Delete account" | `destructive` |
| a warning icon next to a *destructive confirmation* | `destructive` |
| map end-marker | `destructive` |

Named sites: `LiveDriveScreen.kt:653` stop-button circle → `destructive`.
`SosBottomSheet.kt:89,137` → `destructive`. `AccountDeletionScreen.kt:131,157` → `destructive`.
`VehicleGarageScreen.kt:179` "Incomplete" → `rejected`. `SelfAuditScreen.kt:191` "Failed" → `rejected`.
`StatusChip.Error` / `StatusChip.Danger` → `rejected`.

### Ambers → `MilewayRoles.pending`

`0xFFF59E0B` (8×) · `0xFFFF9800` (5×) · `0xFFFFC107` (2×) · `0xFFF2C14E` · `0xFFFFA726` ·
`0xFFFFA000` · `0xFFF57F17` · `0xFFFFF8E1`

"Awaiting approval", "Pending", "Under review", uploaded-not-verified, low-balance,
`PoStatus.PENDING_APPROVAL`, `ApprovalStatus.PENDING`, star ratings on a *pending* review,
`StorageTier.CAUTION`, map pause-marker.

### Oranges → `MilewayRoles.policyViolation`

`0xFFEA580C` (10×) · `0xFFB45309` (7×) · `0xFFD97706` · `0xFFE65100` · `0xFFFF5722` (2×) ·
`0xFF7C2D12`

Anything flagged by a rule rather than by a person: over-policy claims, missing-receipt warnings,
"needs attention" counters, `ReviewResult.Pending`-with-a-problem, `GarageVerification.PENDING`
where a document is overdue. **If it is just "waiting", it is `pending`, not this.** When in doubt
between the two: does the user have to *fix* something? → `policyViolation`. Does the user just have
to *wait*? → `pending`.

### Blues → `MilewayRoles.informational` or `MilewayRoles.distance`

`0xFF1565C0` (11×) · `0xFF2563EB` (6×) · `0xFF2196F3` (4×) · `0xFF1D4ED8` (4×) · `0xFF0D47A1` (4×) ·
`0xFF1B6CA8` (4×) · `0xFF0F4C75` (2×) · `0xFF0B2A6B` (2×) · `0xFF3B82F6` · `0xFF1A73E8` ·
`0xFF42A5F5` · `0xFF1E88E5` · `0xFF5BA8F5` · `0xFF0277BD` · `0xFF0F4C81` · `0xFF80DEEA` ·
`0xFF00BCD4` (2×) · `0xFF0D2137`

| Call site | Role |
|---|---|
| route polyline, track line, distance/odometer figure, "km" chips, map start-of-route | `distance` |
| info icons, hints, "Uploaded", "Redeemed", processing notices, `StatusChip.Info` | `informational` |
| a header gradient that is a **feature's** identity (Analytics, Verification, Plans, Subscription) | **Layer 3** — see below |

Named sites: `MapLibreSurface.kt:59,134,211,213,219` polyline + halo → `distance`.
`KrossMapSurface.kt:128` → `distance`. `MapLegend.kt:69` → `distance`.
`Color.kt`'s deprecated `TrackPolyline` → `distance`.

### Greys → `MilewayRoles.inactive`

`0xFF6B7280` (3×) · `0xFF94A3B8` (2×) · `0xFF9AA5A0` · `0xFF9E9E9E` · `0xFF9CA3AF` · `0xFF9AA0A6` ·
`0xFF90A4AE` · `0xFF8A7F6E` · `0xFFE0E0E0`

Drafts, `NOT_UPLOADED`, `PoStatus.DRAFT`, `TripStatus.COMPLETED`-and-archived, unearned badges,
unselected toggle grounds, `ActivityType.IDLE`, `StatusChip.Neutral`.

*Not* body text or dividers — those are `MaterialTheme.colorScheme.onSurfaceVariant` / `outline`
(Layer 1). A grey that is chrome was never a role.

### Golds / rich purples used as reward → `MilewayRoles.premium`

`0xFFB8860B` (4×) · `0xFFF2C14E`-as-club-tier · `0xFF7B1FA2`-as-tier-badge · `0xFFDB2777` ·
`0xFF9333EA`-as-incentive

Club benefits, tier badges, corporate-card faces, incentive programmes, "Pro" plan markers.

### Offline / sync state → `MilewayRoles.offlineQueued`

No existing literal maps here cleanly — the state was previously drawn as plain `inactive` grey,
which is wrong: an offline-first app's queued write is a *normal* state, not a disabled one. Any
outbox indicator, "will sync" badge, or pending-upload chip you touch during the sweep moves to this
role. Flag any you find, do not leave them grey.

### Live / recording → `MilewayRoles.activeTracking`

The pulse ring, "REC" chip, live-drive glow, active-timer accent. Previously drawn as the accent hex
`0xFFF5A623` (6×) or as red. The **ring** is `activeTracking`; the **stop button** is `destructive`.

Named sites: `MatrixEffects.kt:130,182,208` default colours; `LiveTrackingComponents.kt:113,119,125`.

### Duplicated theme surfaces → Layer 1, `MaterialTheme.colorScheme.*`

These are verbatim copies of `EmberSpec` / `MatrixSpec` fields that leaked into feature code. They are
not roles and not domains — they are the base layer, re-typed:

| Literal | Copy of | Replace with |
|---|---|---|
| `0xFF0B0806` (6×) | Ember `canvas` | `colorScheme.background` |
| `0xFF17110B` (2×) | Ember `surface` | `colorScheme.surface` |
| `0xFF1C140D` | Ember `surfaceCard` | `colorScheme.surfaceContainer` |
| `0xFF241A10` | Ember `surfaceRaised` | `colorScheme.surfaceContainerHigh` |
| `0xFF3D2E1C` (2×) | Ember `border` | `colorScheme.outline` |
| `0xFF3A2A12` | Ember `accentContainer` | `colorScheme.primaryContainer` |
| `0xFFF7EFE3` | Ember `text` | `colorScheme.onSurface` |
| `0xFFF5A623` | Ember `accent` | `colorScheme.primary` (or `MilewayRoles.activeTracking` if it is the live glow) |
| `0xFFB87A1C` | Ember `accentDim` | `colorScheme.secondary` |
| `0xFF040C06` (2×) | Matrix `surface` | `colorScheme.surface` |
| `0xFF00280E` | Matrix `accentContainer` | `colorScheme.primaryContainer` |
| `0x14F5A623` | Ember accent @ 8% | `MilewayRoles.tint(colorScheme.primary)` |

Worst offenders: `feature/agent/.../AgentChatScreen.kt` (10×) and `AgentComponents.kt` (8×) reimplement
Ember wholesale — the assistant renders as Ember under every direction. `widget/MileageSummaryWidget.kt`
(5×) is a Glance widget outside the Compose theme tree; it needs the colours passed in from the host,
not read from `MaterialTheme`. Treat the widget as its own task.

### Feature-identity gradients and accents → Layer 3, `MilewayDomainTheme`

These are not roles. They are whole features asking to feel distinct — exactly what Layer 3 is for.
Replace the literal gradient with `DesignTokens.topBarGradientBrush()` and wrap the feature's nav
entry in the domain.

| Feature / file | Current literals | Domain |
|---|---|---|
| `ApprovalsScreen.kt:353` header | `0xFF6C63FF` → `0xFF9C6BFF` | `APPROVALS` |
| `feature/approvals` sheets | `0xFF1AB090` | `APPROVALS` (+ `approved` for the tick) |
| `PayablesHomeScreen.kt:120` | `0xFF00695C` → `0xFF26A69A` | `PAYABLES` |
| `feature/cards` `CardFace`/`CardDetail`/`CardComponents` | `0xFF2D2F6B`, `0xFF6367FA`, `0xFF3730A3`, `0xFF5C6BC0` | `CARDS` |
| `TravelHomeScreen.kt:109` | `0xFF00695C` → `0xFF00BCD4` | `TRAVEL` |
| `SpendsHomeScreen.kt`, `ExpenseHistoryScreen.kt` | `0xFF1A73E8`/`0xFF0D47A1`, `0xFF6A1B9A`/`0xFFAB47BC` | `EXPENSES` |
| `feature/tracking` `TrackingTheme.kt` gradients | `0xFF4CAF50`/`0xFF2196F3` etc. | `TRACKING` (identity — use `colorScheme.primary`) |
| `feature/profile` per-screen header gradients (≈15 screens) | indigo/teal/navy/purple pairs | **none** — profile is app chrome. Use `DesignTokens.topBarGradientBrush()` unwrapped. |

`feature/profile` is the single biggest offender (28 files) and almost all of it is per-screen header
gradients plus status chips. It gets **no** domain: settings/profile is chrome, and giving each of
fifteen sub-screens its own accent is the 290-colour problem wearing a Layer-3 costume. Every profile
header becomes the plain base gradient; every chip becomes a role.

`ProfileScreen.kt:837-846` declares eleven named colours (`blue`, `red`, `green`, `orange`, `purple`,
`violet`, `teal`, `indigo`, `cyan`, `darkTeal`) used to tint a menu grid. Menu icons are chrome:
replace the whole block with `colorScheme.primary` / `onSurfaceVariant`, or drop the tint entirely.
A settings row does not need an identity colour.

### Categorical data series (charts) → not a role

`AnalyticsHomeScreen.kt:617-620` maps "Mileage / Expense / Travel / Advance" to four hues. A chart
series is categorical data, not product meaning, and forcing it through roles would make two adjacent
series indistinguishable on a low-chroma direction. Derive the series from the active accent by
rotating hue (the same primitive Layer 3 uses) rather than hard-coding four hexes. If you need a
shared helper for this, add one **here**, in `theme/`, and use it from every chart — do not solve it
per screen.

### Genuinely-raw exceptions (leave, but comment)

- `QrHomeScreen.kt:228` QR modules must be near-black on near-white to scan. Keep, add a comment.
- `SignaturePadSheet.kt:128` signature ink is a legal artefact captured at a fixed colour. Keep.
- `IosDemoApp.kt`, `app/src/test/.../ScreenshotGalleryTest.kt`, `app/src/debug/.../ComponentShowcaseScreen.kt`
  are harnesses; they may pin colours to render a specific direction on purpose.
- `external/kmp-toolkit/**` is a vendored composite build. Out of scope — do not edit.

---

## The guard

`RawColorRatchetTest` (in `core/ui/src/androidHostTest`) counts every `Color(0x…)` outside this
package and fails if the total goes **up**. Baseline `313`, measured 2026-08-10. It is a ratchet, not
a ban, so parallel sweeps never collide on it: your batch lowers the number, and only a *new*
hard-coded colour fails the build. Drop `BASELINE` to the figure the test prints when your batch
lands. At 0 it becomes a real ban.

`Color.kt`'s `StatusGreen`/`StatusAmber`/`StatusRed`/`StatusBlue`/`Track*` and
`DesignTokens.StatusColors` are deprecated with the role each maps to, so the IDE points at the
answer at every one of their ~211 call sites.
