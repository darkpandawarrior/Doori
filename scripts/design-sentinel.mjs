#!/usr/bin/env node
/**
 * design-sentinel — the thing that was missing all along.
 *
 * Every failure this repo suffered in Aug 2026 was the same shape: work existed, nothing ran it,
 * nothing alerted. Four screenshot harnesses nobody ran together. iOS captures writing to a path
 * dead since the repo moved, silently creating a phantom folder. A capture pipeline that no-op'd
 * for weeks. A theme nobody had looked at. None of it was hard to find — it was just never looked
 * for on a schedule.
 *
 * So this does the looking, on a schedule, and FAILS LOUDLY:
 *
 *   1. SCOPE CREEP   screens added to the codebase with no capture. The gap that grew to 119.
 *   2. BROKEN        captures that are blank/near-uniform — present but showing nothing.
 *   3. DRIFT         captures whose pixels changed since the last accepted baseline.
 *   4. DESIGN        a vision-model critique of what actually changed, not of all 217 every time.
 *
 * Design principle: it reviews the DELTA, not the world. Re-critiquing 217 unchanged screens every
 * night costs money and buries the one screen that regressed.
 *
 *   node scripts/design-sentinel.mjs            # check, exit non-zero on regression
 *   node scripts/design-sentinel.mjs --accept   # bless current state as the new baseline
 *   node scripts/design-sentinel.mjs --no-ai    # structural checks only, no spend
 */
import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync, existsSync, readdirSync, mkdirSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'

const REPO = join(dirname(fileURLToPath(import.meta.url)), '..')
const SHOTS = join(REPO, 'docs/screenshots')
const STATE = join(REPO, 'docs/screenshots/.sentinel.json')
const REPORT = join(REPO, 'docs/screenshots/SENTINEL_REPORT.md')
const args = process.argv.slice(2)
const ACCEPT = args.includes('--accept')
const NO_AI = args.includes('--no-ai')

const sh = (c, a, o = {}) => execFileSync(c, a, { cwd: REPO, encoding: 'utf8', ...o })
const png = () => readdirSync(SHOTS).filter(f => f.endsWith('.png')).sort()
const hash = f => createHash('sha1').update(readFileSync(join(SHOTS, f))).digest('hex').slice(0, 12)

// ── 1. scope creep ────────────────────────────────────────────────────────────
// Every screen-like composable in the tree, versus what has a capture. This is the check that would
// have caught the gap growing to 119 uncovered screens instead of discovering it months later.
function scopeCreep (shots) {
  const out = sh('bash', ['-c',
    `grep -rEho '^fun [A-Z][A-Za-z0-9]*(Screen|Sheet|Dialog)\\(' --include=*.kt ` +
    `feature core app shared 2>/dev/null | sed 's/^fun //;s/($//;s/(//' | sort -u`])
  const screens = out.split('\n').map(s => s.trim()).filter(Boolean)
  const snake = n => n.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
  const have = new Set(shots.map(f => f.replace(/\.png$/, '')))
  const missing = screens.filter(n => {
    const s = snake(n)
    return ![...have].some(h => h === s || h.startsWith(s.replace(/_screen$|_sheet$|_dialog$/, '')))
  })
  return { total: screens.length, missing }
}

// ── 2. broken captures ────────────────────────────────────────────────────────
// A blank tile is worse than a missing one: it looks covered and is not.
//
// This used to sample the PNG's *compressed* bytes for variety, which is not a measure of image
// content at all — compression makes every file look high-entropy. widget_ios_lockscreen.png is
// pure black, mean rgb(0,0,0), and reported 256 distinct sampled byte values, so it passed this
// check and shipped to the portfolio site. The check has to decode.
//
// Delegates to Pillow, as scripts/vision-review.py already does. If Pillow is missing this throws
// rather than returning an empty list: a broken-capture check that silently finds nothing is the
// exact failure it exists to prevent, and that is how a black rectangle stayed shipped.
function brokenShots (shots) {
  if (!shots.length) return []
  const script = `
import sys, json
from PIL import Image, ImageStat
out = []
for path in json.load(sys.stdin):
    try:
        im = Image.open(path).convert("RGB")
    except Exception:
        out.append(path); continue
    im.thumbnail((240, 240), Image.LANCZOS)
    st = ImageStat.Stat(im)
    # Standard deviation of the decoded pixels. A real screen has type, chrome and cards.
    if max(st.stddev[:3]) < 6.0:
        out.append(path)
print(json.dumps(out))
`
  const paths = shots.map(f => join(SHOTS, f))
  const res = execFileSync('python3', ['-c', script], { input: JSON.stringify(paths), encoding: 'utf8' })
  const flat = new Set(JSON.parse(res))
  // Size check kept as a cheap independent signal — a truncated file may still decode.
  return shots.filter(f => flat.has(join(SHOTS, f)) || readFileSync(join(SHOTS, f)).length < 3000)
}

// ── main ──────────────────────────────────────────────────────────────────────
const shots = png()
const prev = existsSync(STATE) ? JSON.parse(readFileSync(STATE, 'utf8')) : { shots: {}, accepted: [] }
const now = Object.fromEntries(shots.map(f => [f, hash(f)]))

const added = shots.filter(f => !prev.shots[f])
const removed = Object.keys(prev.shots).filter(f => !now[f])
const changed = shots.filter(f => prev.shots[f] && prev.shots[f] !== now[f])
const broken = brokenShots(shots).filter(f => !(prev.accepted || []).includes(f))
const creep = scopeCreep(shots)

// ── 4. design critique of the DELTA only ──────────────────────────────────────
let critique = ''
const toReview = [...added, ...changed].slice(0, 18)
if (!NO_AI && toReview.length) {
  try {
    critique = sh('python3', [join(REPO, 'scripts/vision-review.py'), 'google/gemini-2.5-pro', ...toReview],
      { timeout: 900_000 })
  } catch (e) {
    critique = `_Vision review unavailable: ${String(e.message).slice(0, 200)}_\n` +
      `_Structural checks above still stand — they need no key and no spend._`
  }
}

const lines = [
  `# Design sentinel — ${new Date().toISOString().slice(0, 10)}`, '',
  `${shots.length} captures · ${creep.total} screen-like composables in tree`, '',
  '## Scope creep',
  creep.missing.length
    ? `**${creep.missing.length} screens have no capture.**\n\n` +
      creep.missing.slice(0, 40).map(s => `- ${s}`).join('\n') +
      (creep.missing.length > 40 ? `\n- …and ${creep.missing.length - 40} more` : '')
    : 'None. Every screen-like composable has a capture.',
  '', '## Broken captures',
  broken.length
    ? `**${broken.length} blank or near-uniform.** A blank tile looks covered and is not.\n\n` +
      broken.map(f => `- ${f}`).join('\n')
    : 'None.',
  '', '## Drift since last accepted baseline',
  `added ${added.length} · changed ${changed.length} · removed ${removed.length}`,
  changed.length ? '\n' + changed.map(f => `- changed: ${f}`).join('\n') : '',
  removed.length ? '\n' + removed.map(f => `- **removed**: ${f}`).join('\n') : '',
  '', '## Design critique of what changed',
  critique || '_No new or changed captures to review._',
].join('\n')

mkdirSync(dirname(REPORT), { recursive: true })
writeFileSync(REPORT, lines)

if (ACCEPT) {
  writeFileSync(STATE, JSON.stringify({ shots: now, accepted: brokenShots(shots) }, null, 2))
  console.log(`baseline accepted: ${shots.length} captures`)
  process.exit(0)
}

console.log(lines.split('\n## Design critique')[0])
console.log(`\nfull report: docs/screenshots/SENTINEL_REPORT.md`)

// Exit non-zero on anything that means the system rotted. Removed captures and blank captures are
// unambiguous regressions. Scope creep is a warning until a threshold is agreed, so it is reported
// but does not fail the build yet — set SENTINEL_STRICT=1 to make it fail too.
const regressed = broken.length > 0 || removed.length > 0 ||
  (process.env.SENTINEL_STRICT === '1' && creep.missing.length > 0)
process.exit(regressed ? 1 : 0)
