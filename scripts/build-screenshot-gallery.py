#!/usr/bin/env python3
"""Build a single self-contained HTML gallery of every capture in docs/screenshots.

    python3 scripts/build-screenshot-gallery.py [-o OUT.html]

Why this exists as a script rather than a saved page: the output embeds all 327 captures as
data URIs and lands around 4.5MB, which does not belong in git when the PNGs it is built from
are already tracked. Run it and you get the current truth; a committed copy would start lying
the moment anyone re-records.

Everything it reports is measured from the images themselves, never asserted:

  * light/dark comes from each capture's own mean luminance, so a screen that quietly stops
    following the app theme shows up as the wrong face rather than blending in. That is exactly
    how the pinned-MATRIX preview default was found - a block of "app screens" reading dark
    green while the app had not been green for two design directions.
  * ordering is each file's first-add commit date, newest first.
  * the before/after wipes diff against a git ref, so "what changed" is the repository's answer
    and not a memory of what changed.

Paths are derived from this file's location. Nothing is hardcoded to one machine - iOS captures
wrote outside the repo for months because of an absolute path, and OutputPathGuardTest exists to
stop that recurring.
"""
from __future__ import annotations

import argparse
import base64
import html
import io
import os
import subprocess
import sys
from collections import OrderedDict

try:
    from PIL import Image, ImageStat
except ImportError:
    sys.exit("Pillow is required:  python3 -m pip install --user Pillow")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(REPO, "docs", "screenshots")

# Captures deliberately rendered in a scheme that is NOT the app default. Their filenames are the
# contract: a direction study or a variant sample is supposed to look like itself.
PREFIX_KIND = (
    ("dir_", "direction", "Direction study"),
    ("variant_", "variant", "Theme variant"),
)
TARGET_PREFIXES = ("web_", "desktop_", "wear_", "widget_", "ios_")
# Mean luminance at or below this reads as a dark render. Deliberately generous: a light screen
# showing a modal over a scrim can sit in the 130s without being dark.
DARK_AT = 140


def kind_of(name: str) -> tuple[str, str]:
    for prefix, key, label in PREFIX_KIND:
        if name.startswith(prefix):
            return key, label
    if name.startswith(TARGET_PREFIXES):
        return "target", "Other target"
    if "_matrix" in name:
        return "variant", "Theme variant"
    return "screen", "App screen"


def pretty(name: str) -> str:
    stem = name[:-4] if name.endswith(".png") else name
    for prefix, _, _ in PREFIX_KIND:
        if stem.startswith(prefix):
            stem = stem[len(prefix):]
    return stem.replace("_", " ")


def git(*args: str) -> str:
    return subprocess.check_output(["git", "-C", REPO, *args], text=True, stderr=subprocess.DEVNULL)


def first_added() -> dict[str, str]:
    """Map capture filename -> ISO date it first entered the repo (log is newest-first)."""
    out, added, date = git("log", "--diff-filter=A", "--name-only", "--pretty=format:@%cI",
                           "--", "docs/screenshots"), {}, None
    for line in out.splitlines():
        line = line.strip()
        if line.startswith("@"):
            date = line[1:]
        elif line.endswith(".png"):
            added.setdefault(os.path.basename(line), date)
    return added


def encode(im: Image.Image, width: int, quality: int) -> tuple[str, Image.Image]:
    small = im.convert("RGB")
    small.thumbnail((width, width * 3), Image.LANCZOS)
    buf = io.BytesIO()
    small.save(buf, "WEBP", quality=quality, method=4)
    return "data:image/webp;base64," + base64.b64encode(buf.getvalue()).decode(), small


def mean_rgb(im: Image.Image) -> list[int]:
    # ImageStat rather than iterating getdata(): same arithmetic, no per-pixel Python loop, and
    # getdata() is deprecated for removal in Pillow 14.
    return [int(v) for v in ImageStat.Stat(im).mean[:3]]


def collect(width: int, quality: int) -> list[dict]:
    added = first_added()
    rows = []
    for name in sorted(os.listdir(SHOTS)):
        if not name.endswith(".png"):
            continue
        im = Image.open(os.path.join(SHOTS, name))
        w, h = im.size
        uri, small = encode(im, width, quality)
        m = mean_rgb(small)
        rows.append({"name": name, "w": w, "h": h, "mean": m,
                     "night": sum(m) / 3 <= DARK_AT, "added": added.get(name, ""), "uri": uri})
    rows.sort(key=lambda r: (r["added"], r["name"]), reverse=True)
    return rows


def wipe_pairs(rows: list[dict], ref: str, limit: int) -> list[dict]:
    """Same screen at `ref` and now. Only app screens: comparing a direction study to its own
    past says nothing about what changed in the app."""
    if not ref:
        return []
    pairs = []
    for row in rows:
        if len(pairs) >= limit:
            break
        if kind_of(row["name"])[0] != "screen":
            continue
        try:
            raw = subprocess.check_output(
                ["git", "-C", REPO, "show", f"{ref}:docs/screenshots/{row['name']}"],
                stderr=subprocess.DEVNULL)
            before, _ = encode(Image.open(io.BytesIO(raw)), 560, 80)
            after, _ = encode(Image.open(os.path.join(SHOTS, row["name"])), 560, 80)
        except (subprocess.CalledProcessError, OSError):
            continue  # not present at that ref, or unreadable there
        if before == after:
            continue  # identical bytes render an inert comparator; not worth the payload
        pairs.append({"name": row["name"], "before": before, "after": after})
    return pairs


CSS = """
:root{
  /* Doori's own PaperSpec. The gallery is rendered in the palette it documents, so a token
     that goes wrong in the app goes visibly wrong here too. */
  --canvas:#F7F3EA; --card:#FFFFFF; --rule:#DDD3B8; --muted:#6E6353;
  --ink:#241F1A; --accent:#1E3A5F; --accent-soft:#1E3A5F1a; --shot-bg:#EFE9DC;
}
@media (prefers-color-scheme:dark){
  :root:not([data-theme="light"]){
    /* PaperNightSpec: hand-tuned warm dark, not an inversion. */
    --canvas:#14120E; --card:#1C1912; --rule:#3A3324; --muted:#AA9F89;
    --ink:#EDE6D8; --accent:#8FB4E0; --accent-soft:#8FB4E026; --shot-bg:#241F16;
  }
}
:root[data-theme="dark"]{
  --canvas:#14120E; --card:#1C1912; --rule:#3A3324; --muted:#AA9F89;
  --ink:#EDE6D8; --accent:#8FB4E0; --accent-soft:#8FB4E026; --shot-bg:#241F16;
}
*{box-sizing:border-box}
/* display:grid/flex on .grid and .daybreak outranks the hidden attribute, so filtering would
   set hidden and nothing would disappear. Enforce it once, globally. */
[hidden]{display:none!important}
body{margin:0; background:var(--canvas); color:var(--ink);
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
  font-size:15px; line-height:1.55; -webkit-font-smoothing:antialiased}
.figure{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;
  font-variant-numeric:tabular-nums; font-size:.82em; letter-spacing:-.01em}
.wrap{max-width:1180px; margin:0 auto; padding:0 28px 96px}
header{padding:72px 0 0; border-bottom:1px solid var(--rule)}
.eyebrow{font-size:11px; letter-spacing:.16em; text-transform:uppercase; color:var(--muted);
  margin:0 0 18px; display:flex; gap:10px; align-items:baseline; flex-wrap:wrap}
h1{font-family:ui-serif,"Iowan Old Style",Georgia,"Times New Roman",serif;
  font-weight:600; font-size:clamp(2.1rem,5vw,3.4rem); line-height:1.06; letter-spacing:-.02em;
  margin:0 0 20px; text-wrap:balance; max-width:20ch}
h1 em{font-style:italic; color:var(--accent)}
.standfirst{max-width:62ch; color:var(--muted); margin:0 0 34px; font-size:1.02rem}
.ledger{list-style:none; margin:0; padding:0 0 30px; display:grid;
  grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:0 26px}
.ledger li{display:flex; align-items:baseline; gap:10px; padding:9px 0; border-top:1px solid var(--rule)}
.ledger .n{font-family:ui-monospace,SFMono-Regular,Menlo,monospace; font-variant-numeric:tabular-nums;
  font-size:1.5rem; color:var(--accent); letter-spacing:-.03em}
.ledger .l{font-size:11px; letter-spacing:.11em; text-transform:uppercase; color:var(--muted)}
section{padding-top:56px}
h2{font-family:ui-serif,"Iowan Old Style",Georgia,serif; font-weight:600; font-size:1.22rem;
  letter-spacing:-.01em; margin:0}
.sec-head{display:flex; align-items:baseline; gap:16px; margin-bottom:8px}
.sec-head .rule{flex:1; height:1px; background:var(--rule)}
.note{color:var(--muted); max-width:64ch; margin:0 0 26px; font-size:.94rem}
.wipes{display:grid; grid-template-columns:repeat(auto-fit,minmax(270px,1fr)); gap:26px}
.wipe{margin:0}
.wipe-frame{position:relative; background:var(--shot-bg); border:1px solid var(--rule);
  border-radius:2px; overflow:hidden; line-height:0}
.wipe-frame img{width:100%; display:block}
/* clip-path over a full-width overlay: both halves stay in register at any frame width, and the
   comparator is already correct before any script runs. */
.w-before{position:absolute; inset:0; height:100%; object-fit:cover;
  clip-path:inset(0 calc(100% - var(--pos)) 0 0)}
.w-handle{position:absolute; top:0; bottom:0; left:var(--pos); width:1px; background:var(--accent);
  box-shadow:0 0 0 1px var(--accent-soft)}
.w-handle::after{content:""; position:absolute; top:50%; left:50%; width:26px; height:26px;
  transform:translate(-50%,-50%); border:1px solid var(--accent); border-radius:50%; background:var(--card)}
.w-range{position:absolute; inset:0; width:100%; height:100%; opacity:0; cursor:ew-resize; margin:0}
.w-range:focus-visible{opacity:1; outline:2px solid var(--accent); outline-offset:-2px}
.w-lab{position:absolute; bottom:8px; font-size:10px; letter-spacing:.12em; text-transform:uppercase;
  color:var(--ink); background:var(--card); border:1px solid var(--rule); padding:2px 7px;
  border-radius:2px; line-height:1.4}
.w-lab-l{left:8px} .w-lab-r{right:8px}
.filters{display:flex; flex-wrap:wrap; gap:8px; margin:0 0 30px}
.chip{font:inherit; font-size:12px; letter-spacing:.02em; color:var(--muted); background:transparent;
  border:1px solid var(--rule); border-radius:2px; padding:6px 13px; cursor:pointer}
.chip[aria-pressed="true"]{color:var(--canvas); background:var(--accent); border-color:var(--accent)}
.chip:focus-visible{outline:2px solid var(--accent); outline-offset:2px}
.daybreak{display:flex; align-items:baseline; gap:14px; margin:38px 0 14px}
.daybreak h2{font-size:1rem}
.daybreak .rule{flex:1; height:1px; background:var(--rule)}
.daybreak .figure{color:var(--muted)}
.grid{display:grid; grid-template-columns:repeat(auto-fill,minmax(196px,1fr)); gap:22px}
.card{margin:0; display:flex; flex-direction:column; gap:9px}
.shot{padding:0; border:1px solid var(--rule); border-radius:2px; background:var(--shot-bg);
  cursor:zoom-in; overflow:hidden; line-height:0; display:block; width:100%}
.shot:focus-visible{outline:2px solid var(--accent); outline-offset:2px}
.shot img{width:100%; height:auto; display:block}
figcaption{display:flex; flex-direction:column; gap:4px}
.cap-t{font-size:12.5px; line-height:1.35}
.cap-m{display:flex; flex-wrap:wrap; gap:7px; align-items:center; color:var(--muted)}
.tag{font-size:9.5px; letter-spacing:.1em; text-transform:uppercase; border:1px solid var(--rule);
  padding:1px 6px; border-radius:2px}
.tag-direction,.tag-variant{color:var(--accent); border-color:var(--accent)}
.face{font-size:9.5px; letter-spacing:.1em; text-transform:uppercase}
.face-night{color:var(--accent)}
dialog{border:none; padding:0; background:transparent; max-width:96vw; max-height:96vh}
dialog::backdrop{background:color-mix(in srgb,var(--canvas) 88%,#000)}
dialog img{max-width:92vw; max-height:82vh; width:auto; display:block; border:1px solid var(--rule);
  border-radius:2px; background:var(--shot-bg)}
.lb-cap{display:flex; justify-content:space-between; gap:16px; padding-top:10px; color:var(--muted);
  font-size:12px}
@media (prefers-reduced-motion:reduce){*{animation:none!important; transition:none!important}}
@media (max-width:640px){.wrap{padding:0 18px 72px} header{padding-top:44px}}
"""

JS = """
// Wipe comparators: script only moves --pos; the clipping is CSS, so it survives a JS failure.
for (const w of document.querySelectorAll('.wipe')) {
  const range = w.querySelector('.w-range');
  const sync = () => w.style.setProperty('--pos', range.value + '%');
  range.addEventListener('input', sync);
  sync();
}
const cards = [...document.querySelectorAll('.card')];
const chips = [...document.querySelectorAll('.chip')];
let kindF = 'all', faceF = 'all';
const apply = () => {
  for (const c of cards) {
    c.hidden = !((kindF === 'all' || c.dataset.kind === kindF)
              && (faceF === 'all' || c.dataset.face === faceF));
  }
  for (const g of document.querySelectorAll('.grid')) {
    const any = [...g.querySelectorAll('.card')].some(c => !c.hidden);
    g.hidden = !any;
    g.previousElementSibling.hidden = !any;   // its date heading
  }
};
for (const chip of chips) {
  chip.addEventListener('click', () => {
    const group = chip.dataset.group;
    for (const o of chips) if (o.dataset.group === group) o.setAttribute('aria-pressed', String(o === chip));
    if (group === 'kind') kindF = chip.dataset.value; else faceF = chip.dataset.value;
    apply();
  });
}
const dlg = document.querySelector('dialog');
const dImg = dlg.querySelector('img');
const dName = dlg.querySelector('.lb-name');
const dMeta = dlg.querySelector('.lb-meta');
for (const c of cards) {
  c.querySelector('.shot').addEventListener('click', () => {
    const img = c.querySelector('img');
    dImg.src = img.src; dImg.alt = img.alt;
    dName.textContent = c.dataset.name;
    dMeta.textContent = img.getAttribute('width') + '\\u00d7' + img.getAttribute('height')
                      + '  \\u00b7  ' + c.dataset.face;
    dlg.showModal();
  });
}
dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });
"""


def render(rows: list[dict], pairs: list[dict], built: str) -> str:
    counts = {
        "total": len(rows),
        "night": sum(1 for r in rows if r["night"]),
        **{k: sum(1 for r in rows if kind_of(r["name"])[0] == k)
           for k in ("screen", "direction", "variant", "target")},
    }

    groups: OrderedDict[str, list[dict]] = OrderedDict()
    for r in rows:
        groups.setdefault((r["added"] or "")[:10] or "undated", []).append(r)

    body = []
    for date, items in groups.items():
        body.append(f'<div class="daybreak"><h2>{html.escape(date)}</h2><span class="rule"></span>'
                    f'<span class="figure">{len(items)}</span></div><div class="grid">')
        for r in items:
            key, label = kind_of(r["name"])
            face = "night" if r["night"] else "day"
            t = html.escape(pretty(r["name"]))
            body.append(
                f'<figure class="card" data-kind="{key}" data-face="{face}" '
                f'data-name="{html.escape(r["name"])}">'
                f'<button class="shot" type="button" aria-label="Enlarge {t}">'
                f'<img loading="lazy" src="{r["uri"]}" alt="{t}" width="{r["w"]}" height="{r["h"]}">'
                f'</button><figcaption><span class="cap-t">{t}</span><span class="cap-m">'
                f'<span class="tag tag-{key}">{label}</span>'
                f'<span class="figure">{r["w"]}&times;{r["h"]}</span>'
                f'<span class="face face-{face}">{face}</span></span></figcaption></figure>')
        body.append("</div>")

    wipes = []
    for p in pairs:
        t = html.escape(pretty(p["name"]))
        wipes.append(
            f'<figure class="wipe" style="--pos:50%"><div class="wipe-frame">'
            f'<img class="w-after" src="{p["after"]}" alt="{t} now">'
            f'<img class="w-before" src="{p["before"]}" alt="{t} previously">'
            f'<div class="w-handle" aria-hidden="true"></div>'
            f'<input class="w-range" type="range" min="0" max="100" value="50" '
            f'aria-label="Reveal {t} before or after"><span class="w-lab w-lab-l">before</span>'
            f'<span class="w-lab w-lab-r">now</span></div>'
            f'<figcaption><span class="cap-t">{t}</span></figcaption></figure>')

    wipe_section = ""
    if wipes:
        wipe_section = f"""<section>
  <div class="sec-head"><h2>What changed</h2><span class="rule"></span></div>
  <p class="note">The same screen before and after, pulled straight from the earlier commit.
    Drag to wipe.</p>
  <div class="wipes">{''.join(wipes)}</div>
</section>"""

    return f"""<title>Doori &mdash; screenshot gallery</title>
<style>{CSS}</style>
<div class="wrap">
<header>
  <p class="eyebrow"><span>Doori</span><span>&middot;</span><span>screenshot gallery</span>
    <span>&middot;</span><span class="figure">{html.escape(built)}</span></p>
  <h1>Every screen, as it <em>actually</em> renders.</h1>
  <p class="standfirst">Generated from the captures in <span class="figure">docs/screenshots</span>,
    newest first. Light and dark are measured from each image rather than assumed, so a screen that
    stops following the app theme shows up here as the wrong face instead of blending in.</p>
  <ul class="ledger">
    <li><span class="n">{counts['total']}</span><span class="l">captures</span></li>
    <li><span class="n">{counts['screen']}</span><span class="l">app screens</span></li>
    <li><span class="n">{counts['direction']}</span><span class="l">direction studies</span></li>
    <li><span class="n">{counts['night']}</span><span class="l">dark renders</span></li>
    <li><span class="n">{counts['target']}</span><span class="l">other targets</span></li>
  </ul>
</header>
{wipe_section}
<section>
  <div class="sec-head"><h2>Every capture</h2><span class="rule"></span>
    <span class="figure">newest first</span></div>
  <div class="filters">
    <button class="chip" data-group="kind" data-value="all" aria-pressed="true">All {counts['total']}</button>
    <button class="chip" data-group="kind" data-value="screen" aria-pressed="false">App screens {counts['screen']}</button>
    <button class="chip" data-group="kind" data-value="direction" aria-pressed="false">Direction studies {counts['direction']}</button>
    <button class="chip" data-group="kind" data-value="variant" aria-pressed="false">Theme variants {counts['variant']}</button>
    <button class="chip" data-group="kind" data-value="target" aria-pressed="false">Other targets {counts['target']}</button>
    <button class="chip" data-group="face" data-value="all" aria-pressed="true">Any light</button>
    <button class="chip" data-group="face" data-value="day" aria-pressed="false">Light</button>
    <button class="chip" data-group="face" data-value="night" aria-pressed="false">Dark {counts['night']}</button>
  </div>
  {''.join(body)}
</section>
</div>
<dialog>
  <img alt="">
  <div class="lb-cap"><span class="lb-name figure"></span><span class="lb-meta figure"></span></div>
</dialog>
<script>{JS}</script>
"""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("-o", "--out", default=os.path.join(SHOTS, "gallery.html"))
    ap.add_argument("--width", type=int, default=420, help="thumbnail width in px (default 420)")
    ap.add_argument("--quality", type=int, default=74, help="WebP quality (default 74)")
    ap.add_argument("--diff-against", default="", metavar="REF",
                    help="git ref to build before/after wipes from, e.g. HEAD~1")
    ap.add_argument("--wipes", type=int, default=3, help="how many before/after pairs (default 3)")
    args = ap.parse_args()

    if not os.path.isdir(SHOTS):
        sys.exit(f"no captures at {SHOTS}")

    rows = collect(args.width, args.quality)
    if not rows:
        sys.exit(f"no PNGs in {SHOTS} - run `ROBORAZZI_RECORD=true ./gradlew screenshotTest` first")
    pairs = wipe_pairs(rows, args.diff_against, args.wipes)
    if args.diff_against and not pairs:
        print(f"note: no comparable app screens differ against {args.diff_against}", file=sys.stderr)

    built = git("log", "-1", "--format=%cs").strip() or ""
    page = render(rows, pairs, built)
    with open(args.out, "w") as fh:
        fh.write(page)

    dark = sum(1 for r in rows if r["night"])
    print(f"{args.out}  {os.path.getsize(args.out) / 1024 / 1024:.1f}MB  "
          f"{len(rows)} captures, {dark} dark, {len(pairs)} wipes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
