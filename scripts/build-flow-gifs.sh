#!/usr/bin/env bash
#
# build-flow-gifs.sh — stitch an ordered list of docs/screenshots/*.png frames into one
# animated flow GIF (movement over a stills catalogue). Each frame holds ~1.6s with a short
# crossfade into the next; frames are scaled to 380px wide and padded onto a common dark canvas
# so mixed screen heights line up. Colour is quantised with palettegen/paletteuse for a clean GIF.
#
# Usage:  scripts/build-flow-gifs.sh <output-name> <frame1> <frame2> [frame3 ...]
#   <output-name>   basename written to docs/gifs/<output-name>.gif
#   <frameN>        a docs/screenshots capture name, with or without the .png suffix
#
# Missing frames are skipped with a warning (the GIF is still built from the rest), so a flow
# whose frame did not record degrades gracefully instead of failing the whole run.
#
# Env overrides: HOLD (s/frame, default 1.6), FADE (crossfade s, 0.4), WIDTH (px, 380), FPS (20).
set -euo pipefail

FFMPEG="${FFMPEG:-/opt/homebrew/bin/ffmpeg}"
FFPROBE="${FFPROBE:-/opt/homebrew/bin/ffprobe}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/docs/screenshots"
OUT_DIR="$ROOT/docs/gifs"
WIDTH="${WIDTH:-380}"
HOLD="${HOLD:-1.6}"
FADE="${FADE:-0.4}"
FPS="${FPS:-15}"
BG="0x0D0D0D"   # near-black, matches the Matrix/terminal aesthetic

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <output-name> <frame1> <frame2> [frame3 ...]" >&2
  exit 2
fi

out_name="$1"; shift
mkdir -p "$OUT_DIR"

# Resolve + validate frames (accept name or name.png).
frames=()
for f in "$@"; do
  base="${f%.png}"
  path="$SRC/$base.png"
  if [ -f "$path" ]; then
    frames+=("$path")
  else
    echo "  [skip] missing frame: $base.png" >&2
  fi
done

n="${#frames[@]}"
if [ "$n" -eq 0 ]; then
  echo "  [error] no frames found for '$out_name' — nothing to build" >&2
  exit 1
fi

# Common canvas height = tallest frame once scaled to $WIDTH (rounded even).
maxh=0
for path in "${frames[@]}"; do
  IFS=',' read -r w h < <("$FFPROBE" -v error -select_streams v:0 \
    -show_entries stream=width,height -of csv=p=0 "$path")
  sh=$(( (h * WIDTH + w - 1) / w ))     # ceil(h*WIDTH/w)
  sh=$(( (sh + 1) / 2 * 2 ))            # round up to even
  [ "$sh" -gt "$maxh" ] && maxh="$sh"
done

out="$OUT_DIR/$out_name.gif"

# ── Single frame: a static (1-frame) GIF, no crossfade. ──────────────────────
if [ "$n" -eq 1 ]; then
  "$FFMPEG" -y -loglevel error -i "${frames[0]}" \
    -vf "scale=$WIDTH:-2:flags=lanczos,pad=$WIDTH:$maxh:-1:-1:color=$BG,split[a][b];[a]palettegen=stats_mode=full[p];[b][p]paletteuse=dither=bayer:bayer_scale=3" \
    "$out"
  echo "  [ok] $out (1 frame)"
  exit 0
fi

# ── Multi-frame crossfade slideshow. ─────────────────────────────────────────
# Each input is a still looped to (HOLD+FADE)s; xfade offsets step by HOLD so each frame is
# fully visible for ~HOLD before a FADE-long dissolve. Total ≈ n*HOLD + FADE.
dur=$(awk -v a="$HOLD" -v b="$FADE" 'BEGIN{printf "%.3f", a+b}')

inputs=()
prep=""
for i in $(seq 0 $((n - 1))); do
  inputs+=(-loop 1 -t "$dur" -i "${frames[$i]}")
  prep+="[$i:v]scale=$WIDTH:-2:flags=lanczos,pad=$WIDTH:$maxh:-1:-1:color=$BG,setsar=1,fps=$FPS,format=yuv420p[s$i];"
done

# Chain xfades: [s0][s1]xfade->[x1]; [x1][s2]xfade->[x2]; ...
chain=""
prev="[s0]"
for i in $(seq 1 $((n - 1))); do
  offset=$(awk -v h="$HOLD" -v i="$i" 'BEGIN{printf "%.3f", h*i}')
  if [ "$i" -eq $((n - 1)) ]; then
    label="[vout]"
  else
    label="[x$i]"
  fi
  chain+="${prev}[s$i]xfade=transition=fade:duration=$FADE:offset=$offset$label;"
  prev="[x$i]"
done

filter="${prep}${chain}[vout]split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=3"

"$FFMPEG" -y -loglevel error "${inputs[@]}" -filter_complex "$filter" -loop 0 "$out"
echo "  [ok] $out ($n frames)"
