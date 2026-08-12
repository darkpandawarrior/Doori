#!/usr/bin/env node
/**
 * Groups dir_<variant>_<screen>.png captures BY SCREEN, so the five design directions sit side by
 * side in one row and can actually be compared.
 *
 * Sorting these alphabetically — which is what any default listing does — puts all five Ledger
 * screens together and all five Signal screens together, which is precisely the arrangement that
 * makes comparison impossible. The grouping IS the tool.
 */
import { readdirSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const D = join(dirname(fileURLToPath(import.meta.url)), '../docs/screenshots')
const ORDER = ['refined_ember', 'ledger', 'signal', 'paper', 'instrument'] // baseline first
const files = readdirSync(D).filter(f => f.startsWith('dir_') && f.endsWith('.png'))

const byScreen = {}
for (const f of files) {
  const m = f.replace(/^dir_/, '').replace(/\.png$/, '')
  const variant = ORDER.find(v => m.startsWith(v))
  if (!variant) continue
  const screen = m.slice(variant.length + 1)
  ;(byScreen[screen] ||= {})[variant] = f
}

const rows = Object.entries(byScreen).sort().map(([screen, v]) => `
<h2>${screen.replace(/_/g, ' ')}</h2>
<div class="row">${ORDER.map(o => v[o]
    ? `<figure><a href="${v[o]}" target="_blank"><img loading="lazy" src="${v[o]}"></a>
       <figcaption>${o.replace(/_/g, ' ')}</figcaption></figure>`
    : `<figure class="miss"><div class="ph">not captured</div><figcaption>${o.replace(/_/g,' ')}</figcaption></figure>`
  ).join('')}</div>`).join('')

writeFileSync(join(D, 'directions.html'), `<!doctype html><meta charset="utf-8">
<title>Design directions — ${Object.keys(byScreen).length} screens x 5</title>
<style>:root{--bg:#0f1211;--fg:#e6ebe8;--dim:#8b968f;--card:#171b19;--line:#242a27}
*{box-sizing:border-box}body{margin:0;padding:32px;background:var(--bg);color:var(--fg);
font:14px/1.5 -apple-system,BlinkMacSystemFont,system-ui,sans-serif}
h1{font-size:22px;margin:0 0 4px}.sub{color:var(--dim);margin-bottom:28px}
h2{font-size:14px;margin:34px 0 12px;padding-bottom:8px;border-bottom:1px solid var(--line);
font-weight:600;letter-spacing:.03em}
.row{display:grid;grid-template-columns:repeat(5,1fr);gap:16px}
figure{margin:0;background:var(--card);border:1px solid var(--line);border-radius:10px;overflow:hidden}
figure.miss{opacity:.35}.ph{aspect-ratio:411/891;display:grid;place-items:center;color:var(--dim);font-size:11px}
img{width:100%;display:block;background:#fff}
figcaption{padding:8px 10px;font-size:11px;color:var(--dim);text-transform:capitalize}
a{color:inherit}</style>
<h1>Design directions</h1>
<div class="sub">${Object.keys(byScreen).length} screens &times; 5 directions &middot; same screen across a row &middot;
Refined Ember first as the baseline &middot; <a href="index.html">all captures</a></div>
${rows || '<p class="sub">No dir_*.png captures yet — the render workflow is still running.</p>'}`)

console.log(`directions gallery: ${Object.keys(byScreen).length} screens, ${files.length} captures`)
