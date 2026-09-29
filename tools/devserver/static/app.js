/* Balancing-Zentrale - Oberflaeche. Kein Framework: Hash-Router, Seiten als Funktionen, die HTML
   liefern, und ein Wert-Editor, der ungespeicherte Aenderungen als Entwurf haelt (auch ueber ein
   Neuladen hinweg, im localStorage), bis der Besitzer sie im Speichern-Dialog bestaetigt. */
'use strict';

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
const h = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const LS_DRAFTS = 'bz-drafts-v1';
const LS_PREFS = 'bz-prefs-v1';

let S = null;          // Zustand vom Server (/api/state)
let V = {};            // Werte nach Id
let drafts = {};       // Id -> {value, expected} | {reset: true, expected}
let page = null;       // aktuelle Seite {onDraft}
let prefs = loadJson(LS_PREFS, { stat: 'mean', mode: 'targeted' });
let overviewCache = null;

function loadJson(key, fallback) { try { const v = JSON.parse(localStorage.getItem(key)); return v ?? fallback; } catch (e) { return fallback; } }
function saveJson(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* ohne Storage */ } }

// ---------------------------------------------------------------------------------------------
// Server
// ---------------------------------------------------------------------------------------------
async function api(path, body) {
  const opt = body === undefined ? {} : { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Balance-Client': '1' }, body: JSON.stringify(body) };
  const res = await fetch(path, opt);
  let data;
  try { data = await res.json(); } catch (e) { data = { error: `Antwort ist kein JSON (${res.status})` }; }
  if (!res.ok) { const err = new Error(data.error || res.statusText); err.status = res.status; err.details = data.details || []; throw err; }
  return data;
}

async function loadState() {
  S = await api('/api/state');
  V = S.snapshot.values;
  overviewCache = null;
  baseOverviewCache = null;
  const saved = loadJson(LS_DRAFTS, null);
  if (saved && saved.drafts) drafts = saved.drafts;
  for (const id of Object.keys(drafts)) if (!rec(id) && !(id in S.store.entries)) delete drafts[id];
  buildSearch();
  renderChrome();
}

// ---------------------------------------------------------------------------------------------
// Zahlen und Werte
// ---------------------------------------------------------------------------------------------
const DE = 'de-DE';
function num(x, digits = 4) {
  if (x === null || x === undefined || Number.isNaN(x)) return '–';
  if (!isFinite(x)) return '∞';
  return Number.isInteger(x) ? x.toLocaleString(DE) : x.toLocaleString(DE, { maximumFractionDigits: digits });
}
function pct(p) {
  if (p === null || p === undefined) return '–';
  const x = p * 100;
  if (x === 0) return '0 %';
  if (x >= 10) return x.toLocaleString(DE, { maximumFractionDigits: 1 }) + ' %';
  return x.toLocaleString(DE, { maximumSignificantDigits: 3 }) + ' %';
}
function hours(t) {
  if (t === null || t === undefined) return '–';  // keine passende Quelle (oder nie)
  if (!isFinite(t)) return '∞';
  if (t < 1) return Math.max(1, Math.round(t * 60)) + ' min';
  if (t < 100) return t.toLocaleString(DE, { maximumFractionDigits: 1 }) + ' h';
  return Math.round(t).toLocaleString(DE) + ' h';
}
function fmtVal(v, type) {
  if (v === null || v === undefined) return '–';
  if (type === 'prob') return pct(v);
  if (type === 'bool') return v ? 'an' : 'aus';
  if (type === 'json') return Array.isArray(v) ? `${v.length} Einträge` : (v.label || v.kind || 'Objekt');
  if (typeof v === 'number') return num(v);
  return String(v);
}
function inputText(v, type) {
  if (v === null || v === undefined) return '';
  if (type === 'prob') return (+(v * 100).toPrecision(10)).toString().replace('.', ',');
  if (typeof v === 'number') return (+v.toPrecision(12)).toString().replace('.', ',');
  return String(v);
}
function same(a, b) {
  if (typeof a === 'number' && typeof b === 'number') return Math.abs(a - b) <= 1e-12 + 1e-9 * Math.abs(b);
  return JSON.stringify(a) === JSON.stringify(b);
}

function rec(id) {
  if (!id) return null;
  if (V[id]) return V[id];
  let m = /^param:rate\.([a-z_]+)\.([a-z_]+)@(.+)$/.exec(id);
  if (m && V[`param:rate.${m[1]}.${m[2]}`]) {
    const base = V[`param:rate.${m[1]}.${m[2]}`];
    return Object.assign({}, base, { id, value: null, nullable: true, label: `${base.label} (nur dieses Item)` });
  }
  if (id.startsWith('source:')) return { id, category: 'source', type: 'json', label: 'geplante Quelle', apply: 'plan', value: null, nullable: true, source: {}, refs: {}, note: 'neue Quelle = neuer Pool im Code - Übergabe an einen Lauf' };
  if (id.startsWith('sourceoff:')) return { id, category: 'source', type: 'bool', label: 'Quelle abgeschaltet', apply: 'plan', value: false, source: {}, refs: {}, note: 'eine vorhandene Beute-Quelle schaltest du ab, indem du ihr Gewicht auf 0 setzt' };
  return null;
}
function storedEntry(id) { return S.store.entries[id]; }
function baseValue(id) { const e = storedEntry(id); if (e) return e.value; const r = rec(id); return r ? r.value : undefined; }
function resetTarget(id) {
  const r = rec(id); const e = storedEntry(id);
  if (r && r.apply === 'mod' && e && e.origin !== undefined && e.origin !== null) return e.origin;
  return r ? r.value : null;
}
function curValue(id) { const d = drafts[id]; if (d) return d.reset ? resetTarget(id) : d.value; return baseValue(id); }
function statusOf(id) { return S.store.status[id] || null; }

function parseInput(r, text) {
  const t = String(text).trim().replace(/ /g, '').replace(/\s/g, '');
  if (t === '') { if (r.nullable) return { ok: true, value: null }; return { ok: false, error: 'Wert fehlt' }; }
  if (r.type === 'string') return { ok: true, value: String(text) };
  let s = t.replace('%', '');
  if (s.includes(',') && !s.includes('.')) s = s.replace(',', '.');
  else if (s.includes(',') && s.includes('.')) s = s.replace(/\./g, '').replace(',', '.');
  if (!/^[-+]?(\d+\.?\d*|\.\d+)(e[-+]?\d+)?$/i.test(s)) return { ok: false, error: 'keine Zahl' };
  let x = parseFloat(s);
  if (r.type === 'prob') x = x / 100;
  if (r.type === 'int' && !Number.isInteger(x)) return { ok: false, error: 'ganze Zahl nötig' };
  let lo = r.min, hi = r.max;
  if (r.type === 'prob') { lo = lo ?? 0; hi = hi ?? 1; }
  if (lo !== null && lo !== undefined && x < lo) return { ok: false, error: `mindestens ${fmtVal(lo, r.type)}` };
  if (hi !== null && hi !== undefined && x > hi) return { ok: false, error: `höchstens ${fmtVal(hi, r.type)}` };
  if (r.type === 'prob') x = +x.toPrecision(10);
  return { ok: true, value: x };
}

// ---------------------------------------------------------------------------------------------
// Entwuerfe
// ---------------------------------------------------------------------------------------------
function persistDrafts() { saveJson(LS_DRAFTS, { base: S ? S.store.version : null, drafts }); }
function setDraft(id, value) {
  const base = baseValue(id);
  if (same(value, base)) delete drafts[id]; else drafts[id] = { value, expected: base };
  draftsChanged();
}
function setReset(id) {
  if (!storedEntry(id)) { delete drafts[id]; } else drafts[id] = { reset: true, expected: baseValue(id) };
  draftsChanged();
}
function dropDraft(id) { delete drafts[id]; draftsChanged(); }
let draftTimer = null;
function draftsChanged() {
  persistDrafts();
  updatePending();
  renderSidebar();
  clearTimeout(draftTimer);
  draftTimer = setTimeout(() => { overviewCache = null; if (page && page.onDraft) page.onDraft(); }, 280);
}
function overrides() {
  const out = {};
  for (const [id, d] of Object.entries(drafts)) out[id] = d.reset ? { reset: true } : d.value;
  return out;
}

// ---------------------------------------------------------------------------------------------
// Bausteine
// ---------------------------------------------------------------------------------------------
function linesLabel(lines) {
  const l = lines || [];
  if (!l.length) return '';
  const main = l.filter((x) => x !== '1.21.11');
  const txt = main.length > 1 ? `${main[0]}–${main[main.length - 1]}` : (main[0] || '');
  return txt + (l.includes('1.21.11') ? (txt ? ' + ' : '') + '1.21.11' : '');
}
function needsDatagen(r) {
  const src = (r && r.source) || {}; const refs = (r && r.refs) || {};
  return Array.isArray(src.generated) || (r && r.category === 'loot') || !!(refs.usedByItems && refs.usedByItems.length) || !!refs.material || !!(r && r.alias);
}
function applyBadge(r) {
  if (!r) return '';
  if (r.alias) {
    const t = rec(r.alias.id);
    if (!t || t.apply !== 'mod') return `<span class="badge b-p2" title="${h(r.note || '')}">nur Planung</span>`;
    return `<span class="badge b-mod" title="${h(`Wert = ${r.derived || t.label}. Ändern ändert ${t.group} ${t.label} (${linesLabel(t.lines)}); der Item-Export folgt mit Datagen.`)}">wirkt in Mod</span><span class="badge b-lines" title="über diese Stelle">über ${h(t.label)}</span>`;
  }
  if (r.apply === 'mod') {
    const twins = ((r.source || {}).twinNotes || []);
    const tip = `Speichern schreibt den Wert in die Mod-Quelle – Linien ${linesLabel(r.lines)}${needsDatagen(r) ? '; danach Datagen (erzeugte Dateien)' : ''}${twins.length ? '\n' + twins.join('\n') : ''}`;
    return `<span class="badge b-mod" title="${h(tip)}">wirkt in Mod</span>${r.lines && r.lines.length ? `<span class="badge b-lines" title="${h('Linien: ' + r.lines.join(', '))}">${h(linesLabel(r.lines))}</span>` : ''}`;
  }
  if (r.apply === 'tool') return '<span class="badge b-tool" title="Annahme der Rechner - wirkt nie in der Mod">Rechner</span>';
  return `<span class="badge b-p2" title="${h(r.note || 'Nur Planung - die Zentrale kann diesen Wert nicht schreiben')}">nur Planung</span>`;
}
function statusBadge(id) {
  const st = statusOf(id);
  let out = '';
  if (drafts[id]) out += '<span class="badge b-draft">ungespeichert</span>';
  if (st) {
    if (st.status === 'planned') out += `<span class="badge b-planned" title="Gespeicherter Plan weicht vom Mod-Wert ab">geplant</span>`;
    if (st.status === 'applied') out += `<span class="badge b-mod" title="Gespeicherter Wert steht so in der Mod">in Mod</span>`;
    if (st.status === 'planned' && st.written) out += `<span class="badge b-danger" title="Angewendet, aber im Code steht inzwischen etwas anderes - checkBalance schlägt fehl">Code weicht ab</span>`;
    if (st.status === 'orphan') out += `<span class="badge b-danger" title="Diesen Wert gibt es in der Mod nicht mehr">verwaist</span>`;
    if (st.drift) out += `<span class="badge b-drift" title="Der Mod-Wert hat sich seit dem Speichern geändert (war ${h(fmtVal(st.modAtSave, (rec(id) || {}).type))})">Mod geändert</span>`;
  }
  return out;
}
function srcRef(r) {
  const s = (r && r.source) || {};
  if (!s.file) return '';
  const where = s.file + (s.line ? ':' + s.line : '');
  return `<span class="srcref" data-copy="${h(where)}" title="Klicken kopiert den Pfad">${h(where.split('/').slice(-2).join('/'))}</span>`;
}
function ed(id, opts = {}) {
  const r = rec(id);
  if (!r) return '<span class="muted">–</span>';
  const t = r.alias ? rec(r.alias.id) : null;
  if (t && t.apply === 'mod' && !t.readonly && t.type !== 'bool') {
    // bearbeitet wird die Konstante dahinter; angezeigt wird Konstante x Faktor
    const f = r.alias.factor;
    return `<span class="ve${drafts[t.id] ? ' changed' : ''}" data-ve="${h(t.id)}" data-factor="${h(f)}" data-alias="${h(id)}">${edInner(t.id, t, Object.assign({}, opts, { factor: f, aliasOf: r }))}</span>`;
  }
  return `<span class="ve${drafts[id] ? ' changed' : ''}" data-ve="${h(id)}">${edInner(id, r, opts)}</span>`;
}
function edInner(id, r, opts = {}) {
  const f = opts.factor || 1;
  const scale = (v) => (typeof v === 'number' && f !== 1 ? +(v * f).toPrecision(12) : v);
  const cur = scale(curValue(id));
  const base = scale(baseValue(id));
  const changed = !!drafts[id];
  const shown = opts.aliasOf || r;
  const unit = shown.type === 'prob' ? '%' : (shown.unit || '');
  let html = '';
  if (opts.aliasOf) {
    r = Object.assign({}, r, { type: opts.aliasOf.type, label: opts.aliasOf.label, min: null, max: null });
  }
  if (changed) html += `<del class="old" title="gespeicherter Wert">${h(fmtVal(base, r.type))}</del>`;
  if (r.readonly) {
    html += `<span class="ro" title="${h(r.derived ? 'berechnet: ' + r.derived : (r.note || 'nur lesbar'))}">${h(fmtVal(cur, r.type))}</span>${unit && r.type !== 'prob' ? `<span class="unit">${h(unit)}</span>` : ''}${r.derived ? `<span class="badge" title="${h('berechnet aus ' + r.derived)}">berechnet</span>` : ''}`;
  } else if (r.type === 'bool') {
    html += `<label class="switch"><input type="checkbox" data-vid="${h(id)}" ${cur ? 'checked' : ''} aria-label="${h(r.label)}"><span></span></label><span class="tiny muted">${cur ? 'an' : 'aus'}</span>`;
  } else if (r.type === 'json') {
    html += `<span class="ro">${h(fmtVal(cur, r.type))}</span>`;
  } else {
    const ph = r.nullable && (cur === null || cur === undefined) ? (opts.placeholder || 'auto') : '';
    const aliasAttr = opts.aliasOf ? ` data-factor="${h(f)}" title="${h(`= ${opts.aliasOf.derived || ''} – ändert die Quelle (${(rec(id) || {}).group || ''} ${(rec(id) || {}).label || id})`)}"` : '';
    html += `<input type="text" inputmode="decimal" class="${r.type === 'string' || opts.wide ? 'wide' : ''}" data-vid="${h(id)}"${aliasAttr} value="${h(inputText(cur, r.type))}" placeholder="${h(ph)}" aria-label="${h(r.label)}" spellcheck="false">`;
    if (unit) html += `<span class="unit">${h(unit)}</span>`;
  }
  if (changed) html += `<button class="undo" data-undo="${h(id)}" title="Entwurf verwerfen">↺</button>`;
  else if (storedEntry(id) && !r.readonly && opts.reset !== false) html += `<button class="undo" data-reset="${h(id)}" title="Plan verwerfen: zurück auf den Mod-Wert (${h(fmtVal(resetTarget(id), r.type))})">⟲</button>`;
  if (opts.aliasOf && opts.aliasOf.generatedStale) html += '<span class="badge b-drift" title="Der Item-Export (items.json) ist älter als der Code - Datagen fehlt">Export alt</span>';
  if (opts.badges) html += ' ' + statusBadge(id) + (opts.apply === false ? '' : applyBadge(opts.aliasOf || r));
  return html;
}
function refreshEditor(id) {
  for (const wrap of $$(`[data-ve="${CSS.escape(id)}"]`)) {
    const focused = wrap.contains(document.activeElement) && document.activeElement.tagName === 'INPUT' && document.activeElement.type === 'text';
    if (focused) {
      wrap.classList.toggle('changed', !!drafts[id]);
      let del = wrap.querySelector('del.old');
      const fac = +(wrap.dataset.factor || 1);
      const shownBase = typeof baseValue(id) === 'number' ? +(baseValue(id) * fac).toPrecision(12) : baseValue(id);
      if (drafts[id] && !del) { wrap.insertAdjacentHTML('afterbegin', `<del class="old">${h(fmtVal(shownBase, rec(id).type))}</del>`); }
      if (!drafts[id] && del) del.remove();
      const oldDel = wrap.querySelector('del.old'); if (oldDel) oldDel.textContent = fmtVal(shownBase, rec(id).type);
      continue;
    }
    const badges = !!wrap.querySelector('.badge.b-p2, .badge.b-mod, .badge.b-tool');
    wrap.classList.toggle('changed', !!drafts[id]);
    wrap.classList.remove('invalid');
    const extra = wrap.dataset.alias ? { factor: +wrap.dataset.factor, aliasOf: rec(wrap.dataset.alias) } : {};
    wrap.innerHTML = edInner(id, rec(id), Object.assign({ badges }, extra));
  }
  for (const row of $$(`tr[data-row="${CSS.escape(id)}"]`)) row.classList.toggle('drafted', !!drafts[id]);
}

function slot(key, size = '') {
  const d = (S.snapshot.display || {})[key] || {};
  const name = d.name ? d.name.de : key.split(':').pop();
  const book = key.startsWith('book:') || d.book;
  const letters = h(name.split(/\s+/).map((w) => w[0]).join('').slice(0, 3));
  const img = d.icon ? `<img src="/${h(d.icon)}"${d.anim ? ' class="anim" title="animierte Textur: erstes Bild"' : ''} alt="" loading="lazy" onerror="this.replaceWith(Object.assign(document.createElement('span'),{className:'slot-text',textContent:'${letters}'}))">` : `<span class="slot-text">${letters}</span>`;
  return `<span class="slot ${size}${book ? ' book' : ''}" title="${h(name)}">${img}</span>`;
}
function nameOf(key) {
  const d = (S.snapshot.display || {})[key];
  if (d && d.name) return d.name.de;
  const b = (S.snapshot.books || []).find((x) => x.key === key);
  if (b) return b.name.de;
  return String(key || '').split(':').pop().replace(/_/g, ' ');
}
function itemRef(key, opts = {}) {
  if (!key) return '<span class="muted">leer</span>';
  const linkable = key.startsWith('simplebuilding:') || key.startsWith('book:');
  const inner = `${slot(key, opts.size || 'sm')}<span class="nm">${h(nameOf(key))}</span>${opts.sub ? `<span class="sub">${h(opts.sub)}</span>` : ''}`;
  return linkable ? `<a class="itemref" href="#/item/${encodeURIComponent(key)}">${inner}</a>` : `<span class="itemref">${inner}</span>`;
}
function toast(text, bad = false) {
  const el = document.createElement('div');
  el.className = 'toast' + (bad ? ' bad' : '');
  el.innerHTML = text;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), bad ? 7000 : 3500);
}
function errorBox(err) {
  const details = (err.details || []).map((d) => `<li>${h(d.message || JSON.stringify(d))}</li>`).join('');
  return `<div class="box box-danger"><div class="box-title">Fehler</div>${h(err.message)}${details ? `<ul>${details}</ul>` : ''}</div>`;
}
function sortable(tableSel) {
  const table = typeof tableSel === 'string' ? $(tableSel) : tableSel;
  if (!table) return;
  $$('th.sortable', table).forEach((th, _i) => {
    th.addEventListener('click', () => {
      const idx = Array.from(th.parentNode.children).indexOf(th);
      const asc = !(th.classList.contains('sorted') && th.classList.contains('asc'));
      $$('th', table).forEach((x) => x.classList.remove('sorted', 'asc'));
      th.classList.add('sorted'); if (asc) th.classList.add('asc');
      const body = table.tBodies[0];
      const attached = new Map();  // Item-Zeile -> ihre Aufklapp-Zeile (tr.tchg-row) direkt darunter
      const rows = Array.from(body.rows).filter((r) => { if (r.classList.contains('tchg-row')) { if (r.previousElementSibling) attached.set(r.previousElementSibling, r); return false; } return true; });
      const key = (r) => { const c = r.cells[idx]; const v = c ? (c.dataset.sort ?? c.textContent.trim()) : ''; const n = parseFloat(v); return isNaN(n) ? v.toLowerCase() : n; };
      rows.sort((a, b) => { const x = key(a), y = key(b); const r = (typeof x === 'number' && typeof y === 'number') ? x - y : String(x).localeCompare(String(y), DE); return asc ? r : -r; });
      rows.forEach((r) => { body.appendChild(r); if (attached.has(r)) body.appendChild(attached.get(r)); });
    });
  });
}
function eras() { const v = curValue('param:eras'); return Array.isArray(v) ? v : []; }
function eraFor(t) {
  if (t === null || t === undefined || !isFinite(t)) return '';
  const list = eras().slice().sort((a, b) => a.hours - b.hours);
  let best = null;
  for (const e of list) if (e.hours >= t * 0.999) { best = e; break; }
  return best ? `vor „${best.name}“ (~${num(best.hours)} h)` : `nach „${(list[list.length - 1] || {}).name || '–'}“`;
}
function kindLabel(kind) {
  return { structure: 'Truhen', wandering: 'fahrender Händler', villager: 'Dorfbewohner', mob: 'Mob', block: 'Block', recipe: 'Rezept', custom: 'eigene Quelle' }[kind] || kind;
}

// ---------------------------------------------------------------------------------------------
// Zeiten bearbeiten: eine Zielzeit eintippen -> die Zentrale rechnet die Stellwerte aus
// (POST /api/solve-time, sbdev/solver.py) und übernimmt sie als Entwürfe. Gespeichert wird wie
// immer erst im Speichern-Dialog. Je Item merkt sich timeEdits, welche Werte eine Zeit-Änderung
// gesetzt hat (für den Aufklapp-Bereich "Automatische Änderungen").
// ---------------------------------------------------------------------------------------------
const LS_TIME = 'bz-time-edits-v1';
let timeEdits = loadJson(LS_TIME, {});
const openChanges = new Set();  // Aufklapp-Bereiche, die der Besitzer geöffnet hat (sonst immer zu)
function saveTimeEdits() { saveJson(LS_TIME, timeEdits); }
const STAT_LABEL = { mean: 'Mittel', median: 'Median', p90: '90 %' };
function parseHours(text) {
  const t = String(text || '').trim().toLowerCase().replace(/\s+/g, ' ');
  const m = /^([0-9]+(?:[.,][0-9]+)?)\s*(min|m|h|std|stunden?|d|tage?)?$/.exec(t);
  if (!m) return null;
  const x = parseFloat(m[1].replace(',', '.'));
  const unit = m[2] || 'h';
  if (unit === 'min' || unit === 'm') return x / 60;
  if (unit.startsWith('d') || unit.startsWith('tag')) return x * 24;
  return x;
}
function timeDiffers(a, b) {
  if (a === undefined) return false;
  if (a === null || b === null) return a !== b;
  return Math.abs(a - b) > 5e-4 * Math.max(Math.abs(a), Math.abs(b), 1e-9);
}
function strategyFor(item) { return ((prefs.strategies || {})[item]) || 'proportional'; }
/* Eine Zeitzelle: neuer Wert als Eingabefeld (wenn bearbeitbar), der gespeicherte Stand rot
   durchgestrichen daneben, sobald Entwürfe ihn verändern. */
function timeCell(t, old, spec, cls = '') {
  const changed = timeDiffers(old, t);
  let inner = changed ? `<del class="old" title="gespeicherter Stand (ohne Entwürfe)">${h(hours(old))}</del>` : '';
  if (spec && t !== null && t !== undefined && isFinite(t)) {
    inner += `<input class="tin" type="text" inputmode="decimal" value="${h(hours(t))}" data-t="${t}" data-item="${h(spec.item)}" data-row="${h(spec.row)}" data-stat="${h(spec.stat)}" data-k="${spec.k}" data-label="${h(spec.label || '')}" data-tunables="${h((spec.tunables || []).join(' '))}" title="${h(`Zielzeit eintippen (z. B. 12, 12,5 h oder 30 min) – die Zentrale rechnet die Werte aus, die ${spec.label || 'diese Zeile'} so schnell machen (${STAT_LABEL[spec.stat]}, ${spec.k}. Stück), und übernimmt sie als Entwurf`)}" aria-label="${h(`Zeit ${nameOf(spec.item)} – ${spec.label || ''} – ${spec.k}. Stück ${STAT_LABEL[spec.stat]}`)}" spellcheck="false">`;
  } else {
    inner += `<span${spec === null ? ' title="rechnet aus den Zutaten – ändere die Zeit auf der Seite der Zutat"' : ''}>${h(hours(t))}</span>`;
  }
  return `<td class="num time tcell ${cls}${changed ? ' tchanged' : ''}${t === null ? ' never' : ''}" data-sort="${t ?? 1e12}">${inner}</td>`;
}
function sourceLabel(item, s) {
  if (s.kind === 'structure') return (S.snapshot.structures[s.structure] || {}).label || s.structure;
  if (s.kind === 'wandering' || s.kind === 'villager') {
    const t = S.snapshot.trades.find((x) => x.id === s.trade);
    return t ? `${t.professionDe}${t.level ? ' Stufe ' + t.level : ''}` : s.trade;
  }
  return s.label || s.key;
}
function strategySelect(item, report) {
  const cur = strategyFor(item);
  const srcs = (S.snapshot.sources[item] || []).filter((s) => ['structure', 'wandering', 'mob', 'block'].includes(s.kind));
  let opts = `<option value="proportional">alle Quellen proportional (Standard)</option>`;
  opts += srcs.map((s) => `<option value="source:${h(s.key)}" ${cur === 'source:' + s.key ? 'selected' : ''}>nur Quelle: ${h(sourceLabel(item, s))} (${h(kindLabel(s.kind))})</option>`).join('');
  const seen = new Set();
  const tun = report ? report.rows.flatMap((r) => r.tunables || []).filter((t) => !seen.has(t.id) && seen.add(t.id)) : [];
  opts += tun.map((t) => `<option value="value:${h(t.id)}" ${cur === 'value:' + t.id ? 'selected' : ''}>nur Wert: ${h(t.label)}</option>`).join('');
  if (cur.startsWith('value:') && !tun.some((t) => 'value:' + t.id === cur)) opts += `<option value="${h(cur)}" selected>nur Wert: ${h((rec(cur.slice(6)) || {}).label || cur.slice(6))}</option>`;
  return `<label class="tstrat-l tiny"><span class="muted">Verteilung bei mehreren Quellen</span> <select class="tstrat" data-item="${h(item)}" title="Wie eine eingetippte Zeit auf die Werte verteilt wird: proportional = alle Stellwerte der Quellen mit demselben Faktor; nur Quelle = nur deren Werte; nur Wert = genau dieser Wert">${opts}</select></label>`;
}
function setDrafts(values) {
  for (const [id, value] of Object.entries(values)) {
    const base = baseValue(id);
    if (same(value, base)) delete drafts[id]; else drafts[id] = { value, expected: base };
  }
  draftsChanged();
  for (const id of Object.keys(values)) refreshEditor(id);
}
async function editTime(input) {
  const d = input.dataset;
  const target = parseHours(input.value);
  if (target === null || !(target > 0)) { input.classList.add('bad'); toast('Zeit bitte als Zahl in Stunden (z. B. 12, 12,5 h oder 30 min).', true); return; }
  input.classList.remove('bad');
  if (!timeDiffers(+d.t, target)) { input.value = hours(+d.t); return; }
  let strategy = strategyFor(d.item);
  const single = !d.row.startsWith('__');
  if (single && strategy.startsWith('source:')) strategy = 'proportional';
  if (single && strategy.startsWith('value:') && !(d.tunables || '').split(' ').includes(strategy.slice(6))) strategy = 'proportional';
  input.disabled = true;
  input.insertAdjacentHTML('afterend', '<span class="spinner tspin"></span>');
  let res;
  try {
    res = await api('/api/solve-time', { item: d.item, row: d.row, stat: d.stat, k: +d.k, hours: target, strategy, overrides: overrides() });
  } catch (err) { input.disabled = false; const sp = input.parentNode.querySelector('.tspin'); if (sp) sp.remove(); toast(h(err.message), true); return; }
  const lines = res.lines || [];
  if (!lines.length) {
    input.disabled = false; input.value = hours(+d.t); const sp = input.parentNode.querySelector('.tspin'); if (sp) sp.remove();
    toast(h(res.message || 'Keine Änderung nötig.'), !res.feasible);
    return;
  }
  const values = {};
  for (const l of lines) values[l.id] = res.values[l.id];
  (timeEdits[d.item] = timeEdits[d.item] || []).push({
    row: d.row, label: d.label, stat: d.stat, k: +d.k, old: res.current, hours: target, achieved: res.achieved, strategy,
    feasible: res.feasible, message: res.message, sharedUsed: res.sharedUsed, at: Date.now(),
    lines: lines.map((l) => ({ id: l.id, label: l.label, group: l.group, type: l.type, apply: l.apply, sites: l.sites, lines: l.lines,
      file: l.file, line: l.line, clamped: l.clamped, alsoAffects: l.alsoAffects, warnings: l.warnings, error: l.error, datagen: l.datagen })),
  });
  saveTimeEdits();
  openChanges.delete(d.item);  // der Bereich bleibt zu, bis der Besitzer ihn öffnet
  setDrafts(values);
  toast(`${res.feasible ? '' : '⚠ '}${lines.length} Wert${lines.length === 1 ? '' : 'e'} als Entwurf: ${h(d.label || '')} ${d.k}. Stück ${STAT_LABEL[d.stat]} → ${hours(res.achieved)}${res.message ? '<br><span class="tiny">' + h(res.message) + '</span>' : ''}<br><span class="tiny muted">Welche Werte: „Automatische Änderungen“ am Item aufklappen.</span>`, !res.feasible);
}
/* Der Aufklapp-Bereich je Item: welche Werte eine Zeit-Änderung automatisch gesetzt hat - alt (rot
   durchgestrichen) -> neu, Stellen je Linie, wirkt in Mod / Rechner. Standardmäßig zu. */
function changesBox(item, report, opts = {}) {
  const edits = timeEdits[item] || [];
  const info = {};
  for (const e of edits) for (const l of e.lines) info[l.id] = l;
  const ids = Object.keys(info).filter((id) => drafts[id]);
  if (edits.length && !ids.length) { delete timeEdits[item]; saveTimeEdits(); }
  const live = ids.length ? edits : [];
  const open = openChanges.has(item);
  const where = (l) => (l.sites && l.sites.length ? l.sites.map((x) => `${x.mc}: ${String(x.file || '').split('/').pop()}${x.line ? ':' + x.line : ''}`).join(' · ') : (l.file ? `${String(l.file).split('/').pop()}${l.line ? ':' + l.line : ''}` : ''));
  const body = `<div class="tchg-body">
      ${opts.strategy === false ? '' : `<div style="margin:.3rem 0 .5rem">${strategySelect(item, report)}</div>`}
      ${live.length ? `<ul class="tchg-edits">${live.map((e) => `<li><b>${h(e.label || e.row)}</b> · ${e.k}. Stück · ${STAT_LABEL[e.stat] || e.stat}: <del class="diff-old">${h(hours(e.old))}</del> → <span class="diff-new">${h(hours(e.hours))}</span>${timeDiffers(e.hours, e.achieved) ? ` <span class="muted">(erreicht ${h(hours(e.achieved))})</span>` : ''} <span class="badge">${h(e.strategy === 'proportional' ? 'proportional' : e.strategy.startsWith('source:') ? 'nur eine Quelle' : 'nur ein Wert')}</span>${e.sharedUsed ? ' <span class="badge b-drift" title="Die eigenen Werte des Items reichten nicht - auch geteilte Werte (Würfe, Angebots-Chance eines Buch-Angebots) wurden angepasst">auch geteilte Werte</span>' : ''}${e.message ? `<div class="tiny" style="color:var(--warn-text)">${h(e.message)}</div>` : ''}</li>`).join('')}</ul>` : ''}
      ${ids.length ? `<div class="table-wrap"><table class="table tchg-table"><thead><tr><th>Wert</th><th class="num">alt</th><th></th><th>neu</th><th>Stelle</th><th>wirkt</th><th></th></tr></thead><tbody>${ids.map((id) => {
        const l = info[id]; const r = rec(id) || { label: l.label, type: l.type, group: l.group };
        const oldV = baseValue(id), newV = curValue(id);
        const fmt = (v) => (r.type === 'json' && v && typeof v === 'object' ? `Chance ${pct(v.chance)}` : fmtVal(v, r.type));
        return `<tr><td><b>${h(r.label || l.label)}</b><div class="sub">${h(r.group || l.group || '')}</div>${l.clamped ? `<span class="badge b-drift">an der Grenze (${l.clamped === 'max' ? 'Höchstwert' : 'Kleinstwert'})</span>` : ''}${(l.alsoAffects || []).length ? `<div class="tiny muted" title="${h(l.alsoAffects.join(', '))}">wirkt auch auf ${l.alsoAffects.length} weitere: ${h(l.alsoAffects.slice(0, 5).map(nameOf).join(', '))}${l.alsoAffects.length > 5 ? ' …' : ''}</div>` : ''}${l.error ? `<div class="tiny" style="color:var(--danger)">${h(l.error)}</div>` : ''}</td>
          <td class="num diff-old">${h(fmt(oldV))}</td><td class="diff-arrow">→</td><td class="diff-new">${h(fmt(newV))}</td>
          <td class="sub">${where(l) ? `<span class="srcref" data-copy="${h(where(l))}">${h(where(l))}</span>` : '–'}${l.datagen && (r.apply === 'mod') ? '<div class="tiny muted">danach Datagen</div>' : ''}</td>
          <td>${applyBadge(rec(id) || { apply: l.apply })}</td><td><button class="btn small" data-undo="${h(id)}" title="Diesen Entwurf verwerfen">↺</button></td></tr>`;
      }).join('')}</tbody></table></div>
      <div class="row-actions" style="display:flex;gap:.5rem;margin-top:.5rem;flex-wrap:wrap"><button class="btn small primary" data-tsave="1">Speichern …</button><button class="btn small danger" data-tdrop="${h(item)}">Diese Änderungen verwerfen</button><span class="tiny muted">Nichts ist gespeichert, bevor du im Speichern-Dialog bestätigst (neue Version, danach ggf. Datagen).</span></div>`
        : '<p class="tiny muted">Noch keine automatischen Änderungen. Tippe eine Zeit ein (Felder in der Tabelle) – hier stehen dann die Werte, die sich dadurch ändern, mit altem und neuem Wert, Datei und Zeile.</p>'}
    </div>`;
  return `<details class="tchanges${ids.length ? ' has' : ''}" data-item="${h(item)}" ${open ? 'open' : ''}><summary>Automatische Änderungen${ids.length ? ` <span class="badge b-draft">${ids.length} Wert${ids.length === 1 ? '' : 'e'}</span>` : ''}</summary>${body}</details>`;
}

// ---------------------------------------------------------------------------------------------
// Rahmen: Kopf, Seitenleiste, Leiste unten
// ---------------------------------------------------------------------------------------------
const NAV = [
  ['Start', [['#/', '⌂', 'Übersicht']]],
  ['Werte', [['#/items', '◆', 'Gegenstände & Rechner'], ['#/loot', '▣', 'Beute (Truhen)', 'loot'], ['#/trades', '⇄', 'Handel', 'trade'],
    ['#/drops', '☠', 'Mob-, Block-Drops & Erze', 'worldgen'], ['#/stats', '⚒', 'Werkzeuge & Rüstung', 'item'], ['#/enchant', '✦', 'Verzauberungen', 'enchant'],
    ['#/recipes', '⌗', 'Rezepte', 'recipe'], ['#/constants', 'ƒ', 'Code-Konstanten', 'constant'], ['#/config', '⚙', 'Config-Standards', 'config'],
    ['#/potionpads', '⚗', 'Trank-Pads', 'potionpad']]],
  ['Rechner', [['#/calc', '⏱', 'Seltenheit & Zeitalter'], ['#/params', '≈', 'Annahmen', 'param']]],
  ['Verlauf', [['#/versions', '↶', 'Versionen'], ['#/phase2', '→', 'Übergabe (nur Planung)']]],
  ['Wissen', [['#/docs', '§', 'Dokumentation'], ['#/report', '!', 'Auslese-Bericht']]],
];
function renderChrome() {
  renderSidebar();
  const st = S.store;
  const saved = st.savedAt ? st.savedAt.replace('T', ' ').slice(0, 16) : 'noch nie';
  $('#verpill').innerHTML = `<b>v${st.version}</b><span class="long">· gespeichert ${h(saved)}</span>`;
  updatePending();
}
function renderSidebar() {
  if (!S) return;
  const counts = S.snapshot.counts;
  const draftCats = new Set(Object.keys(drafts).map((id) => (rec(id) || {}).category));
  const current = location.hash.split('?')[0] || '#/';
  let html = '';
  for (const [title, links] of NAV) {
    html += `<div class="sb-group"><div class="sb-title">${h(title)}</div>`;
    for (const [href, ico, label, cat] of links) {
      const active = current === href || (href !== '#/' && current.startsWith(href.replace(/s$/, '')) && href.length > 2);
      const n = cat ? `<span class="n">${counts[cat] ?? ''}</span>` : '';
      const dot = cat && draftCats.has(cat) ? '<span class="dot" title="ungespeicherte Änderungen"></span>' : '';
      html += `<a href="${href}" class="${active ? 'active' : ''}"><span class="sb-ico">${ico}</span>${h(label)}${dot || n}</a>`;
    }
    html += '</div>';
  }
  html += `<div class="sb-group tiny muted" style="padding:0 .6rem">Eingelesen ${h(S.snapshot.builtAt)} (${num(S.snapshot.buildSeconds)} s) · Linie ${h(S.snapshot.line)}</div>`;
  $('#sidebar').innerHTML = html;
}
function updatePending() {
  const n = Object.keys(drafts).length;
  $('#pending').hidden = n === 0;
  $('#pending-count').textContent = n;
}

// ---------------------------------------------------------------------------------------------
// Suche
// ---------------------------------------------------------------------------------------------
let searchIndex = [];
function buildSearch() {
  const idx = [];
  const pages = NAV.flatMap(([, links]) => links.map(([href, , label]) => ({ t: label, s: 'Seite', href, k: 'Seite' })));
  idx.push(...pages);
  for (const it of S.snapshot.items) idx.push({ t: it.name.de, s: `${it.name.en} · ${it.id}`, href: `#/item/${encodeURIComponent(it.id)}`, k: 'Item', key: it.id });
  for (const b of S.snapshot.books) idx.push({ t: b.name.de + ' (Buch)', s: b.key, href: `#/item/${encodeURIComponent(b.key)}`, k: 'Buch', key: b.key });
  for (const r of Object.values(V)) {
    if (r.category === 'item' || r.category === 'recipe') continue;
    idx.push({ t: r.label, s: `${r.group || ''} · ${r.id}`, href: pageFor(r) + '?f=' + encodeURIComponent(r.id), k: (S.categories[r.category] || '').split(' ')[0] });
  }
  for (const d of S.docs) idx.push({ t: d.title, s: 'docs/' + d.name, href: `#/docs/${d.name}`, k: 'Doku' });
  searchIndex = idx.map((e) => Object.assign(e, { n: (e.t + ' ' + e.s).toLowerCase() }));
}
function pageFor(r) {
  return { loot: '#/loot', trade: '#/trades', worldgen: '#/drops', mobdrop: '#/drops', blockdrop: '#/drops', item: '#/stats', enchant: '#/enchant',
    recipe: '#/recipes', constant: '#/constants', config: '#/config', param: '#/params', potionpad: '#/potionpads' }[r.category] || '#/';
}
function runSearch(q) {
  const box = $('#qs');
  q = q.trim().toLowerCase();
  if (!q) { box.hidden = true; return; }
  const words = q.split(/\s+/);
  const hits = [];
  for (const e of searchIndex) {
    if (!words.every((w) => e.n.includes(w))) continue;
    const score = (e.t.toLowerCase().startsWith(q) ? 0 : 1) + (e.k === 'Seite' ? -0.5 : 0) + (e.k === 'Item' ? -0.2 : 0);
    hits.push([score, e]);
    if (hits.length > 400) break;
  }
  hits.sort((a, b) => a[0] - b[0] || a[1].t.localeCompare(b[1].t, DE));
  const top = hits.slice(0, 14).map(([, e]) => e);
  box.innerHTML = top.length ? top.map((e, i) => `<a class="qs-item" href="${h(e.href)}" ${i === 0 ? 'aria-selected="true"' : ''}>${e.key ? slot(e.key, 'sm') : ''}<span class="qs-main"><span class="qs-title">${h(e.t)}</span><span class="qs-sub">${h(e.s)}</span></span><span class="qs-sec">${h(e.k)}</span></a>`).join('') : '<div class="qs-empty">Nichts gefunden.</div>';
  box.hidden = false;
}

// ---------------------------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------------------------
const routes = [
  [/^#\/?$/, pageHome], [/^#\/items$/, pageItems], [/^#\/item\/(.+)$/, pageItem], [/^#\/loot$/, pageLoot], [/^#\/trades$/, pageTrades],
  [/^#\/drops$/, pageDrops], [/^#\/stats$/, pageStats], [/^#\/enchant$/, pageEnchant], [/^#\/recipes$/, pageRecipes],
  [/^#\/constants$/, pageConstants], [/^#\/config$/, pageConfig], [/^#\/calc$/, pageCalc], [/^#\/params$/, pageParams],
  [/^#\/potionpads$/, pagePotionPads],
  [/^#\/versions$/, pageVersions], [/^#\/version\/(\d+)$/, pageVersion], [/^#\/phase2$/, pagePhase2], [/^#\/docs$/, pageDocs],
  [/^#\/docs\/(.+)$/, pageDoc], [/^#\/report$/, pageReport],
];
async function route() {
  if (!S) return;
  const [path, query] = (location.hash || '#/').split('?');
  const params = new URLSearchParams(query || '');
  document.body.classList.remove('nav-open');
  $('#qs').hidden = true;
  page = null;
  const main = $('#main');
  for (const [re, fn] of routes) {
    const m = re.exec(path);
    if (!m) continue;
    try {
      page = {};
      const out = await fn(main, ...m.slice(1).map(decodeURIComponent));
      if (typeof out === 'string') main.innerHTML = out;
    } catch (err) {
      console.error(err);
      main.innerHTML = errorBox(err);
    }
    renderSidebar();
    const focus = params.get('f');
    if (focus) setTimeout(() => focusValue(focus), 60);
    else window.scrollTo(0, 0);
    return;
  }
  main.innerHTML = `<div class="empty">Seite nicht gefunden: <code>${h(path)}</code> – <a href="#/">zur Übersicht</a></div>`;
}
function focusValue(id) {
  const el = $(`[data-ve="${CSS.escape(id)}"]`) || $(`[data-row="${CSS.escape(id)}"]`);
  if (!el) return;
  const details = el.closest('details'); if (details) details.open = true;
  el.scrollIntoView({ block: 'center' });
  const row = el.closest('tr') || el;
  row.animate([{ background: 'var(--warn-bg)' }, { background: 'transparent' }], { duration: 2200 });
  const input = el.querySelector('input'); if (input) input.focus({ preventScroll: true });
}

// ---------------------------------------------------------------------------------------------
// Seite: Uebersicht
// ---------------------------------------------------------------------------------------------
async function pageHome(main) {
  const snap = S.snapshot;
  const entries = S.store.entries;
  const statuses = Object.entries(S.store.status);
  const planned = statuses.filter(([id, s]) => s.status === 'planned' && (rec(id) || {}).apply !== 'tool').length;
  const drift = statuses.filter(([, s]) => s.drift);
  const orphans = statuses.filter(([, s]) => s.status === 'orphan');
  const nMod = Object.values(V).filter((r) => r.apply === 'mod').length;
  const nPlan = Object.values(V).filter((r) => r.apply === 'plan').length;
  const pendingApply = statuses.filter(([id, s]) => s.status === 'planned' && (rec(id) || {}).apply === 'mod' && !s.written);
  const cats = Object.entries(S.categories).filter(([k]) => snap.counts[k]);
  main.innerHTML = `
    <h1>Balancing-Zentrale</h1>
    <p class="lead">Alle Balance-Werte von SimpleBuilding an einem Ort – direkt aus dem Repo gelesen, mit Plan-Versionen, Rückgängig und Rechnern für die Beschaffungszeit. Geänderte Werte zeigen den alten Wert <del class="diff-old">rot durchgestrichen</del>; gespeichert wird erst nach Bestätigung.</p>
    ${S.store.warnings.length ? `<div class="box box-warn"><div class="box-title">Hinweise der Ablage</div><ul>${S.store.warnings.map((w) => `<li>${h(w)}</li>`).join('')}</ul></div>` : ''}
    <div class="tiles">
      <div class="tile"><div class="big">${num(Object.keys(V).length)}</div><div class="lbl">Werte eingelesen</div></div>
      <div class="tile" title="Speichern schreibt diese Werte in die Mod-Quelle (JSON/Java, alle Linien)"><div class="big">${num(nMod)}</div><div class="lbl">wirken in der Mod</div></div>
      <a class="tile" href="#/report" title="Werte, die die Zentrale nicht schreiben kann - jeder mit Grund"><div class="big">${num(nPlan)}</div><div class="lbl">nur Planung</div></a>
      <a class="tile" href="#/phase2"><div class="big">${num(planned)}</div><div class="lbl">geplant, nicht in der Mod</div></a>
      <div class="tile ${Object.keys(drafts).length ? 'warn' : ''}"><div class="big">${num(Object.keys(drafts).length)}</div><div class="lbl">ungespeichert</div></div>
      <a class="tile" href="#/versions"><div class="big">v${S.store.version}</div><div class="lbl">${S.history.length - 1} Versionen</div></a>
      <a class="tile ${snap.report.problems.length ? 'warn' : ''}" href="#/report"><div class="big">${snap.report.problems.length}</div><div class="lbl">nicht auslesbare Stellen</div></a>
    </div>
    <div class="card"><div class="legend">
      <span><span class="badge b-mod">wirkt in Mod</span><span class="badge b-lines">26.2–26.4 + 1.21.11</span> Speichern schreibt den Wert in die Mod-Quelle dieser Linien (danach ggf. Datagen)</span>
      <span><span class="badge b-p2">nur Planung</span> die Zentrale kann ihn nicht schreiben (Grund im Tooltip / Auslese-Bericht)</span>
      <span><span class="badge b-tool">Rechner</span> Annahme, wirkt nie in der Mod</span>
      <span><span class="badge b-planned">geplant</span> gespeicherter Plan ≠ Mod</span>
      <span><span class="badge b-drift">Mod geändert</span> Mod-Wert hat sich seit dem Speichern bewegt</span>
    </div></div>
    ${drift.length || orphans.length ? `<div class="box box-danger"><div class="box-title">Abweichungen</div>
      ${drift.length ? `<p>${drift.length} geplante Werte, deren Mod-Wert sich seit dem Speichern geändert hat (z. B. durch einen parallelen Lauf):</p><ul>${drift.slice(0, 12).map(([id, s]) => `<li><a href="${pageFor(rec(id))}?f=${encodeURIComponent(id)}">${h(rec(id).group)} – ${h(rec(id).label)}</a>: Mod jetzt ${h(fmtVal(rec(id).value, rec(id).type))}, beim Speichern ${h(fmtVal(s.modAtSave, rec(id).type))}, Plan ${h(fmtVal(entries[id].value, rec(id).type))}</li>`).join('')}</ul>` : ''}
      ${orphans.length ? `<p>${orphans.length} gespeicherte Werte gibt es in der Mod nicht mehr (umbenannt?). Sie bleiben erhalten, bis du sie bewusst entfernst:</p><ul>${orphans.map(([id]) => `<li><code>${h(id)}</code> = ${h(JSON.stringify(entries[id].value))} <button class="btn small" data-reset="${h(id)}">entfernen (als Entwurf)</button></li>`).join('')}</ul>` : ''}
    </div>` : ''}
    ${pendingApply.length ? `<div class="box box-warn"><div class="box-title">Noch nicht angewendet</div>${pendingApply.length} gespeicherte Werte stehen noch nicht in den Mod-Dateien. <button class="btn small primary" id="apply-planned">Jetzt anwenden …</button></div>` : ''}
    <div class="card" id="modstate"><div class="card-head"><h2>Mod-Stand: checkBalance & Datagen</h2><span class="sp"></span><button class="btn small" id="check-btn">Prüfen</button></div><div id="check-box"><span class="spinner"></span></div><div id="job-box"></div></div>
    <div class="grid2">
      <div class="card"><h2>Baukerne auf einen Blick</h2><div id="cores"><span class="spinner"></span></div>
        <p class="tiny muted">Erstes Stück, beste einzelne Quelle gezielt vs. normales Spiel (alle Quellen, normale Raten). Modell wie docs/KERNE-SELTENHEIT.md Abschnitt 2.</p></div>
      <div class="card"><h2>Bereiche</h2><table class="table"><tbody>
        ${cats.map(([k, label]) => `<tr><td><a href="${pageFor({ category: k })}">${h(label)}</a></td><td class="num">${num(snap.counts[k])}</td></tr>`).join('')}
      </tbody></table></div>
    </div>`;
  const btn = $('#apply-planned'); if (btn) btn.onclick = openApplyPlanned;
  $('#check-btn').onclick = () => fillCheck();
  fillCheck();
  pollJob(true);
  const cores = ['copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite'].map((c) => `simplebuilding:${c}_core`);
  page.onDraft = () => fillCores(cores);
  fillCores(cores);
}
// checkBalance und Datagen (Übersicht)
async function fillCheck() {
  const box = $('#check-box'); if (!box) return;
  box.innerHTML = '<span class="spinner"></span> prüfe …';
  let res;
  try { res = await api('/api/check'); } catch (err) { box.innerHTML = errorBox(err); return; }
  const st = res.stats;
  const datagen = res.errors.some((e) => e.kind === 'datagen');
  box.innerHTML = `${res.ok ? `<div class="box box-info"><b>checkBalance: ok</b> – ${st.entries} gespeicherte Werte (${st.applied} stehen so in der Mod), ${st.generatedChecked} erzeugte Stellen passen zum Code.</div>`
    : `<div class="box box-danger"><div class="box-title">checkBalance: ${res.errors.length} Fehler – das Gate (gradlew check) wäre rot</div><ul>${res.errors.slice(0, 10).map((e) => `<li>${h(e.message)}</li>`).join('')}${res.errors.length > 10 ? `<li>… ${res.errors.length - 10} weitere</li>` : ''}</ul></div>`}
    ${res.warnings.length ? `<details><summary class="tiny muted">${res.warnings.length} Hinweise (geplant, nicht angewendet / verwaist)</summary><ul class="tiny">${res.warnings.map((w) => `<li>${h(w.message)}</li>`).join('')}</ul></details>` : ''}
    <div class="row-actions" style="display:flex;gap:.6rem;align-items:center;flex-wrap:wrap;margin-top:.5rem">
      <button class="btn ${datagen ? 'primary' : ''}" id="datagen-btn">Datagen starten …</button>
      <label class="tiny"><input type="checkbox" id="dg-264"> auch 26.4-Snapshot</label>
      <span class="tiny muted">baut die erzeugten Dateien aller Linien neu (Beute-Tabellen, Verzauberungen, Rezepte, Erze, Item-Export, Wiki) – dauert einige Minuten</span></div>`;
  $('#datagen-btn').onclick = () => startDatagen(false);
}
async function startDatagen(force) {
  const box = $('#job-box');
  try {
    await api('/api/datagen', { force, include264: !!($('#dg-264') || {}).checked });
  } catch (err) {
    if (err.status === 409 && !force && /Dev-Client/.test(err.message)) {
      openModal(`<h2>Datagen trotzdem starten?</h2>${errorBox(err)}<div class="foot"><button class="btn" onclick="closeModal()">Abbrechen</button><button class="btn danger" id="dg-force">Trotzdem starten</button></div>`);
      $('#dg-force').onclick = () => { closeModal(); startDatagen(true); };
      return;
    }
    if (box) box.innerHTML = errorBox(err);
    return;
  }
  pollJob();
}
let jobTimer = null;
async function pollJob(quiet) {
  clearTimeout(jobTimer);
  const box = $('#job-box'); if (!box) return;
  let job;
  try { job = await api('/api/datagen'); } catch (err) { box.innerHTML = errorBox(err); return; }
  if (job.status === 'idle') { if (!quiet) box.innerHTML = ''; return; }
  const icon = { waiting: '·', running: '<span class="spinner"></span>', ok: '✓', failed: '✗', skipped: '–', cancelled: '✗' };
  const res = job.result || {};
  box.innerHTML = `<h3>Datagen ${job.status === 'running' ? 'läuft' : job.status === 'ok' ? 'fertig' : job.status === 'cancelled' ? 'abgebrochen' : 'fehlgeschlagen'} (${num(job.seconds, 0)} s)</h3>
    <table class="table"><tbody>${job.steps.map((st) => `<tr><td>${icon[st.status] || ''}</td><td>${h(st.label)}</td><td class="num sub">${st.seconds !== null && st.seconds !== undefined ? num(st.seconds, 0) + ' s' : ''}</td></tr>`).join('')}</tbody></table>
    ${job.status === 'running' ? '<button class="btn small danger" id="job-cancel">Abbrechen</button>' : ''}
    <details ${job.status === 'failed' ? 'open' : ''}><summary class="tiny muted">Ausgabe (letzte Zeilen)</summary><pre class="joblog">${h(job.log.join('\n'))}</pre></details>
    ${res.diff ? `<div class="box box-info"><div class="box-title">Geänderte Dateien im Arbeitsbaum (${res.diff.count ?? 0})</div>${res.diff.error ? h(res.diff.error) : `<pre class="joblog">${h(res.diff.stat || '(keine)')}</pre>`}</div>` : ''}
    ${res.check ? (res.check.ok ? '<div class="box box-info"><b>checkBalance nach dem Datagen: ok</b> – Code, Ablage und erzeugte Dateien passen zusammen.</div>' : `<div class="box box-danger"><div class="box-title">checkBalance nach dem Datagen: ${res.check.errors.length} Fehler</div><ul>${res.check.errors.slice(0, 8).map((e) => `<li>${h(e.message)}</li>`).join('')}</ul></div>`) : ''}`;
  const cancel = $('#job-cancel'); if (cancel) cancel.onclick = () => api('/api/datagen/cancel', {}).catch((err) => toast(h(err.message), true));
  const log = box.querySelector('pre.joblog'); if (log) log.scrollTop = log.scrollHeight;
  if (job.status === 'running') jobTimer = setTimeout(() => pollJob(), 1500);
  else if (!quiet) { await loadState(); fillCheck(); toast(job.status === 'ok' ? 'Datagen fertig – neu eingelesen.' : 'Datagen nicht erfolgreich – siehe Ausgabe.', job.status !== 'ok'); }
}

async function getOverview() {
  if (!overviewCache) overviewCache = await api('/api/overview', { overrides: overrides() });
  return overviewCache;
}
let baseOverviewCache = null;  // gespeicherter Stand ohne Entwürfe (für die alten Zeiten)
async function getBaseOverview() {
  if (!baseOverviewCache) baseOverviewCache = await api('/api/overview', { overrides: {} });
  return baseOverviewCache;
}
async function fillCores(cores) {
  const el = $('#cores'); if (!el) return;
  const ov = await getOverview();
  const rows = Object.fromEntries(ov.rows.map((r) => [r.item, r]));
  el.innerHTML = `<div class="table-wrap"><table class="table"><thead><tr><th>Kern</th><th>beste Quelle</th><th class="num">gezielt Ø</th><th class="num">Median</th><th class="num">normal Ø</th></tr></thead><tbody>
    ${cores.map((c) => { const r = rows[c] || {}; return `<tr><td>${itemRef(c)}</td><td class="sub">${h(r.bestLabel || '–')}</td><td class="num time">${hours(r.bestMean)}</td><td class="num time">${hours(r.bestMedian)}</td><td class="num time">${hours(r.normalMean)}</td></tr>`; }).join('')}
  </tbody></table></div>`;
}

// ---------------------------------------------------------------------------------------------
// Seite: Gegenstaende (Liste) und Item (Detail mit Rechner)
// ---------------------------------------------------------------------------------------------
async function pageItems(main) {
  const f = loadJson('bz-items-filter', { q: '', only: 'sources' });
  main.innerHTML = `<div class="listhead"><div><h1>Gegenstände & Rechner</h1><p class="lead">Jedes Item mit seinen Werten und – wo es Quellen gibt – der Zeit bis zum ersten Stück. Klick öffnet Rechner und Quellen.</p></div>
    <div class="filters"><input type="search" id="iq" placeholder="Filtern …" value="${h(f.q)}">
      <span class="seg" id="ionly"><button data-v="sources" aria-pressed="${f.only === 'sources'}">mit Quellen</button><button data-v="books" aria-pressed="${f.only === 'books'}">Bücher</button><button data-v="all" aria-pressed="${f.only === 'all'}">alle</button></span></div></div>
    <div class="card"><div class="table-wrap"><table class="table" id="itab"><thead><tr><th class="sortable">Item</th><th class="sortable">beste Quelle</th><th class="num sortable">1. Stück gezielt Ø</th><th class="num sortable">Median</th><th class="num sortable">normal Ø</th><th class="sortable">Zeitalter</th><th class="num sortable">Quellen</th><th>Werte</th></tr></thead><tbody id="ibody"><tr><td colspan="8"><span class="spinner"></span></td></tr></tbody></table></div></div>`;
  const ov = await getOverview();
  const byItem = Object.fromEntries(ov.rows.map((r) => [r.item, r]));
  const items = S.snapshot.items.map((i) => ({ key: i.id, name: i.name.de, en: i.name.en, stats: Object.keys(i.stats).length }))
    .concat(S.snapshot.books.map((b) => ({ key: b.key, name: b.name.de + ' (Buch)', en: b.name.en, stats: 0, book: true })));
  const statsCount = {};
  for (const r of Object.values(V)) { const k = r.refs && (r.refs.item || r.refs.gives); if (k) statsCount[k] = (statsCount[k] || 0) + 1; }
  function draw() {
    const q = $('#iq').value.trim().toLowerCase();
    const only = $('#ionly [aria-pressed="true"]').dataset.v;
    saveJson('bz-items-filter', { q, only });
    const rows = items.filter((i) => (only === 'all' || (only === 'books' ? i.book : byItem[i.key])) && (!q || (i.name + ' ' + i.en + ' ' + i.key).toLowerCase().includes(q)));
    $('#ibody').innerHTML = rows.length ? rows.map((i) => { const r = byItem[i.key] || {}; return `<tr><td data-sort="${h(i.name)}">${itemRef(i.key)}</td><td class="sub">${h(r.bestLabel || '')}</td><td class="num time" data-sort="${r.bestMean ?? 1e12}">${r.item ? hours(r.bestMean) : ''}</td><td class="num time" data-sort="${r.bestMedian ?? 1e12}">${r.item ? hours(r.bestMedian) : ''}</td><td class="num time" data-sort="${r.normalMean ?? 1e12}">${r.item ? hours(r.normalMean) : ''}</td><td class="sub">${r.item ? h(eraFor(r.bestMean)) : ''}</td><td class="num">${r.sources ?? ''}</td><td class="num">${statsCount[i.key] || ''}</td></tr>`; }).join('') : '<tr><td colspan="8" class="empty">Keine Treffer.</td></tr>';
  }
  $('#iq').oninput = draw;
  $$('#ionly button').forEach((b) => b.onclick = () => { $$('#ionly button').forEach((x) => x.setAttribute('aria-pressed', x === b)); draw(); });
  draw();
  sortable('#itab');
  page.onDraft = async () => { await getOverview(); };
}

function valuesForItem(key) {
  const out = [];
  for (const r of Object.values(V)) {
    const refs = r.refs || {};
    if (refs.item === key || refs.gives === key) { out.push(r); continue; }
    if (key.startsWith('book:') && refs.enchantment && key === `book:${refs.enchantment}:${refs.level}`) out.push(r);
  }
  // Handels-Buecher: Pool-Gewichte fuer genau diese Verzauberung
  if (key.startsWith('book:')) {
    const [, ench, level] = /^book:(.+):(\d+)$/.exec(key) || [];
    for (const t of S.snapshot.trades) for (const e of t.enchantPool || []) if (e.enchantment === ench && String(e.level) === level && V[e.id] && !out.includes(V[e.id])) out.push(V[e.id]);
  }
  return out;
}

async function pageItem(main, key) {
  const d = (S.snapshot.display || {})[key] || {};
  const name = nameOf(key);
  const en = d.name ? d.name.en : '';
  const values = valuesForItem(key);
  const item = S.snapshot.items.find((i) => i.id === key);
  main.innerHTML = `<div class="crumbs"><a href="#/items">Gegenstände</a> › ${h(name)}</div>
    <div class="hero">${slot(key, 'big')}<div><h1>${h(name)}</h1><div class="muted">${h(en)}</div><div class="idline"><code>${h(key)}</code> ${item ? `<span class="badge">${h(item.family)}</span>` : ''}</div></div></div>
    <div class="card" id="calc-card"><div class="card-head"><h2>Zeit bis 1 – 6 Stück</h2><span class="sp"></span>
      <span class="seg" id="stat"><button data-v="mean" aria-pressed="${prefs.stat === 'mean'}">Mittel</button><button data-v="median" aria-pressed="${prefs.stat === 'median'}">Median</button><button data-v="p90" aria-pressed="${prefs.stat === 'p90'}">90 %</button></span></div>
      <div id="tstrat-box" class="tiny" style="margin:-.2rem 0 .5rem"></div>
      <div id="calc"><span class="spinner"></span></div></div>
    <div class="card" id="rev-card"><h2>Rückwärts: welche Chance für eine Zielzeit?</h2><div id="rev"></div></div>
    <div class="card"><div class="card-head"><h2>Quellen</h2><span class="sp"></span><button class="btn small" id="add-src">+ Quelle planen</button></div><div id="srcs"></div><div id="src-form"></div></div>
    <div class="card"><h2>Werte zu diesem Item</h2>${values.length ? valueTable(values, { showGroup: true }) : '<p class="muted">Keine eigenen Balance-Werte (nur Rezept/Beschaffung).</p>'}</div>`;
  let report = null;
  let baseReport = null;  // gespeicherter Stand ohne Entwürfe - für die alten (durchgestrichenen) Zeiten
  async function recalc() {
    try {
      const withDrafts = Object.keys(drafts).length > 0;
      [report, baseReport] = await Promise.all([api('/api/calc', { item: key, overrides: overrides() }), withDrafts ? api('/api/calc', { item: key, overrides: {} }) : null]);
      if (!baseReport) baseReport = report;
    } catch (err) { $('#calc').innerHTML = errorBox(err); return; }
    drawCalc();
    drawSources();
    drawReverse();
  }
  function drawCalc() {
    const stat = prefs.stat;
    const rows = report.rows;
    if (!rows.length && !report.normal) { $('#calc').innerHTML = '<p class="muted">Keine Quelle bekannt – füge unten eine geplante Quelle hinzu, dann rechnet die Tabelle.</p>'; return; }
    const bestKey = report.best && report.best.key;
    const ks = report.k;
    const baseRows = Object.fromEntries((baseReport || report).rows.map((r) => [r.key, r]));
    const cell = (arr, i, old, spec) => timeCell(arr[i], old ? old[i] : null, spec ? Object.assign({ item: key, stat, k: ks[i] }, spec) : spec);
    let body = rows.map((r) => {
      const info = r.kind === 'structure' ? (r.detail || []).map((x) => `${x.container}: ${num(x.rate, 2)}/h × ${pct(x.perOpening)}`).join(' · ')
        : r.offerChance !== undefined ? `im Angebot ${pct(r.offerChance)} · ${r.perVisit} Stück je Auffüllung` : (r.note || '');
      const ass = (r.assumptions || []).filter((a) => rec(a));
      return `<tr class="${r.key === bestKey ? 'best' : ''} ${r.disabled ? 'off' : ''}"><td><b>${h(r.label)}</b> <span class="badge">${h(kindLabel(r.kind))}</span>${r.planned ? ' <span class="badge b-planned">geplant</span>' : ''}${r.disabled ? ' <span class="badge b-danger">abgeschaltet</span>' : ''}
        <div class="sub">${h(info)}</div>
        ${ass.length ? `<details class="tiny"><summary class="muted">Annahmen (${ass.length})</summary>${ass.map((a) => `<div style="display:flex;gap:.5rem;align-items:center;justify-content:space-between;margin:.2rem 0"><span>${h(rec(a).label)}</span>${ed(a, { placeholder: 'auto' })}</div>`).join('')}</details>` : ''}
        </td>${ks.map((_, i) => cell(r[stat], i, (baseRows[r.key] || {})[stat], r.kind === 'recipe' ? null : r.disabled ? undefined : { row: r.key, label: r.label, tunables: (r.tunables || []).map((t) => t.id) })).join('')}</tr>`;
    }).join('');
    const baseTogether = (baseReport || report).together, baseNormal = (baseReport || report).normal;
    if (report.together) body += `<tr><td><b>Alle Quellen gezielt</b> <span class="badge">Summe</span><div class="sub">${h(report.together.note)}</div></td>${ks.map((_, i) => cell(report.together[stat], i, baseTogether ? baseTogether[stat] : undefined, { row: '__together__', label: 'Alle Quellen gezielt' })).join('')}</tr>`;
    if (report.normal) body += `<tr><td><b>Normales Spiel</b> <span class="badge">alle, normale Raten</span><div class="sub">${h(report.normal.note)}</div></td>${ks.map((_, i) => cell(report.normal[stat], i, baseNormal ? baseNormal[stat] : undefined, { row: '__normal__', label: 'Normales Spiel' })).join('')}</tr>`;
    $('#tstrat-box').innerHTML = strategySelect(key, report);
    $('#calc').innerHTML = `<div class="table-wrap"><table class="table calc-table"><thead><tr><th>Quelle (gezielt)</th>${ks.map((k) => `<th class="num">${k}.</th>`).join('')}</tr></thead><tbody>${body}</tbody></table></div>
      <p class="tiny muted">Die Zeiten sind Eingabefelder: Zielzeit eintippen (Enter) – die Zentrale rechnet die Werte dahinter aus (Kisten-Chance/Gewicht, Angebots-Chance, …), übernimmt sie als Entwurf und rechnet alle anderen Zeiten neu. Alter Stand <del class="diff-old">rot durchgestrichen</del>. Rezept-Zeilen rechnen aus den Zutaten.</p>
      ${changesBox(key, report, { strategy: false })}
      ${chart(report, stat)}
      <p class="tiny muted">${stat === 'mean' ? 'Mittelwert' : stat === 'median' ? 'Median (die Hälfte der Spieler ist schneller)' : '90 % der Spieler sind schneller als'} in Stunden Spielzeit. Grün = schnellste einzelne Quelle. Zahlen deterministisch aus Pools, Gewichten, Mengen und den Annahmen (zusammengesetzter Poisson-Prozess).</p>`;
  }
  function drawSources() {
    const srcs = (S.snapshot.sources[key] || []);
    const custom = Object.keys(Object.assign({}, S.store.entries, drafts)).filter((id) => id.startsWith(`source:${key}:`));
    let html = '<table class="table"><thead><tr><th>Quelle</th><th>Art</th><th>Details</th><th class="num">Planung</th></tr></thead><tbody>';
    for (const s of srcs) {
      const offId = `sourceoff:${key}:${s.key}`;
      const row = (report ? report.rows : []).find((r) => r.key === s.key) || {};
      html += `<tr data-row="${h(offId)}"><td><b>${h(row.label || s.label || s.structure || s.trade || s.key)}</b></td><td>${h(kindLabel(s.kind))}</td><td class="sub">${h(row.poolText || (s.kind === 'structure' ? (row.detail || []).map((x) => `${x.container}: ${num(x.rate, 2)}/h × ${pct(x.perOpening)}`).join(' · ') : s.note || ''))}</td>
        <td class="num"><span class="tiny muted">abschalten</span> ${ed(offId, { reset: true })}</td></tr>`;
    }
    for (const id of custom) {
      const spec = curValue(id);
      if (!spec) { html += `<tr class="drafted"><td colspan="3"><del class="diff-old">geplante Quelle</del> <span class="badge b-draft">wird entfernt</span></td><td class="num"><button class="btn small" data-undo="${h(id)}">behalten</button></td></tr>`; continue; }
      html += `<tr class="planned ${drafts[id] ? 'drafted' : ''}"><td><b>${h(spec.label)}</b> <span class="badge b-planned">geplant</span> ${statusBadge(id)}</td><td>${h(S.sourceKinds[spec.kind] || spec.kind)}</td>
        <td class="sub">Chance ${pct(spec.chance)} · Anzahl ${spec.countMin}–${spec.countMax}${spec.rate !== undefined ? ` · ${num(spec.rate)}/h` : ''}${spec.structure ? ' · ' + h(S.snapshot.structures[spec.structure].label) : ''}${spec.note ? ' · ' + h(spec.note) : ''}</td>
        <td class="num"><button class="btn small" data-edit-src="${h(id)}">bearbeiten</button> <button class="btn small danger" data-del-src="${h(id)}">entfernen</button></td></tr>`;
    }
    html += '</tbody></table>';
    if (!srcs.length && !custom.length) html = '<p class="muted">Keine Quelle in der Mod.</p>';
    html += '<p class="tiny muted">Abschalten und geplante Quellen sind Planung (Phase 2): die Rechner zeigen die Wirkung sofort; die Übergabe-Seite listet sie für die Umsetzung.</p>';
    $('#srcs').innerHTML = html;
    $$('[data-edit-src]').forEach((b) => b.onclick = () => sourceForm(key, b.dataset.editSrc));
    $$('[data-del-src]').forEach((b) => b.onclick = () => { if (storedEntry(b.dataset.delSrc)) setDraft(b.dataset.delSrc, null); else dropDraft(b.dataset.delSrc); drawSources(); });
  }
  function drawReverse() {
    const el = $('#rev');
    const rows = report.rows.filter((r) => !r.disabled);
    const tunableRows = rows.filter((r) => (r.tunables || []).length);
    if (!tunableRows.length) { el.innerHTML = '<p class="muted">Keine einstellbare Chance in den Quellen (z. B. nur Rezepte oder Annahmen).</p>'; return; }
    const saved = loadJson('bz-rev', {});
    const erasOpt = eras().map((e) => `<option value="${h(e.hours)}">${h(e.name)} (~${num(e.hours)} h)</option>`).join('');
    el.innerHTML = `<div class="reverse">
      <div class="field"><label>Zeitalter (optional)</label><select id="rv-era"><option value="">– eigene Zeit –</option>${erasOpt}</select></div>
      <div class="field"><label>Zielzeit (h)</label><input id="rv-h" type="text" inputmode="decimal" value="${h(saved.hours || '')}" placeholder="z. B. 25"></div>
      <div class="field"><label>für das …</label><select id="rv-k">${[1, 2, 3, 4, 5, 6].map((k) => `<option value="${k}">${k}. Stück</option>`).join('')}</select></div>
      <div class="field"><label>Maß</label><select id="rv-stat"><option value="mean">Mittel</option><option value="median">Median</option></select></div>
      <div class="field"><label>Zeile</label><select id="rv-row">${tunableRows.map((r) => `<option value="${h(r.key)}">${h(r.label)} (gezielt)</option>`).join('')}<option value="__normal__">Normales Spiel (alle)</option></select></div>
      <div class="field"><label>Stellwert</label><select id="rv-t"></select></div>
      <div class="field"><label>&nbsp;</label><button class="btn primary" id="rv-go">Vorschlag rechnen</button></div>
    </div><div id="rv-out"></div>
    <p class="tiny muted">Mit Zeitalter: Zielzeit = Zeitalter × ${num(curValue('param:eraTargetRatio'))} (Annahme „Zielanteil am Zeitalter“, Regel aus KERNE-SELTENHEIT 5.3). Gerechnet wird mit allen anderen Werten wie gerade eingestellt (inkl. Entwürfe).</p>`;
    const fillTunables = () => {
      const rk = $('#rv-row').value;
      const row = rk === '__normal__' ? { tunables: rows.flatMap((r) => r.tunables || []) } : rows.find((r) => r.key === rk);
      const seen = new Set();
      $('#rv-t').innerHTML = (row.tunables || []).filter((t) => !seen.has(t.id) && seen.add(t.id)).map((t) => `<option value="${h(t.id)}" data-type="${h(t.type)}" data-field="${h(t.field || '')}">${h(t.label)} (jetzt ${h(fmtVal(t.value, t.type))})</option>`).join('');
    };
    $('#rv-row').onchange = fillTunables; fillTunables();
    $('#rv-era').onchange = () => { const v = $('#rv-era').value; if (v) $('#rv-h').value = inputText(+v * curValue('param:eraTargetRatio'), 'float'); };
    $('#rv-go').onclick = async () => {
      const opt = $('#rv-t').selectedOptions[0];
      const hoursVal = parseFloat(String($('#rv-h').value).replace(',', '.'));
      saveJson('bz-rev', { hours: $('#rv-h').value });
      const out = $('#rv-out');
      if (!opt) { out.innerHTML = '<p class="muted">Kein Stellwert gewählt.</p>'; return; }
      if (!(hoursVal > 0)) { out.innerHTML = '<div class="box box-danger">Bitte eine Zielzeit in Stunden eingeben (oder ein Zeitalter wählen).</div>'; return; }
      out.innerHTML = '<span class="spinner"></span>';
      const rowKey = $('#rv-row').value;
      const tunable = { id: opt.value, type: opt.dataset.type, field: opt.dataset.field || undefined };
      try {
        const res = await api('/api/reverse', { item: key, row: rowKey, mode: rowKey === '__normal__' ? 'normal' : 'targeted', tunable, k: +$('#rv-k').value, stat: $('#rv-stat').value, hours: hoursVal, overrides: overrides() });
        const t = rec(tunable.id) || { type: tunable.type };
        const now = curValue(tunable.id);
        const nowVal = tunable.field ? (now || {})[tunable.field] : now;
        out.innerHTML = `<div class="box ${res.feasible ? 'box-info' : 'box-warn'}">${res.feasible ? `Vorschlag: <span class="result-big">${h(fmtVal(res.value, tunable.field === 'chance' ? 'prob' : t.type))}</span> <span class="muted">(jetzt ${h(fmtVal(nowVal, tunable.field === 'chance' ? 'prob' : t.type))})</span> → ergibt ${hours(res.achieved)} statt Ziel ${hours(hoursVal)}.
          <button class="btn small primary" id="rv-take">Als Entwurf übernehmen</button> ${applyBadge(t)}` : h(res.message)}</div>`;
        const take = $('#rv-take');
        if (take) take.onclick = () => {
          if (tunable.field) { const spec = Object.assign({}, curValue(tunable.id)); spec[tunable.field] = res.value; setDraft(tunable.id, spec); } else setDraft(tunable.id, res.value);
          toast('Als Entwurf übernommen – oben rechnet die Tabelle neu. Speichern unten.');
        };
      } catch (err) { out.innerHTML = errorBox(err); }
    };
  }
  $$('#stat button').forEach((b) => b.onclick = () => { prefs.stat = b.dataset.v; saveJson(LS_PREFS, prefs); $$('#stat button').forEach((x) => x.setAttribute('aria-pressed', x === b)); drawCalc(); });
  $('#add-src').onclick = () => sourceForm(key, null);
  page.onDraft = () => { recalc(); for (const id of Object.keys(drafts)) refreshEditor(id); };
  recalc();
}

function chart(report, stat) {
  const series = [];
  const colors = ['var(--c1)', 'var(--c2)', 'var(--c3)', 'var(--c4)', 'var(--c5)', 'var(--c6)'];
  for (const r of report.rows.filter((x) => !x.disabled).slice(0, 5)) series.push({ label: r.label, data: r[stat] });
  if (report.together) series.push({ label: 'Alle gezielt', data: report.together[stat], dash: true });
  if (report.normal) series.push({ label: 'Normales Spiel', data: report.normal[stat], dash: true });
  const vals = series.flatMap((s) => s.data).filter((x) => x !== null && isFinite(x) && x > 0);
  if (!vals.length) return '';
  // Logarithmische Zeitachse: 1 h, 10 h und 1000 h bleiben gleichzeitig lesbar
  const lo = Math.pow(10, Math.floor(Math.log10(Math.min(...vals)))), hi = Math.pow(10, Math.ceil(Math.log10(Math.max(...vals) * 1.05)));
  const W = 760, H = 230, L = 50, R = 150, T = 10, B = 24;
  const ks = report.k;
  const x = (i) => L + (W - L - R) * (i / (ks.length - 1 || 1));
  const y = (v) => T + (H - T - B) * (1 - (Math.log10(v) - Math.log10(lo)) / (Math.log10(hi) - Math.log10(lo)));
  let svg = `<svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Zeit bis k Stück je Quelle, logarithmisch">`;
  for (let d = lo; d <= hi * 1.001; d *= 10) {
    for (const m of [1, 2, 5]) { const t = d * m; if (t > hi * 1.001) break; svg += `<line class="grid" x1="${L}" x2="${W - R}" y1="${y(t)}" y2="${y(t)}" ${m === 1 ? '' : 'stroke-opacity=".45"'}/>${m === 1 || (Math.log10(hi / lo) <= 2) ? `<text x="${L - 6}" y="${y(t) + 3}" text-anchor="end">${num(t)} h</text>` : ''}`; }
  }
  ks.forEach((k, i) => { svg += `<text x="${x(i)}" y="${H - 6}" text-anchor="middle">${k}.</text>`; });
  let lastY = -99;
  for (const e of eras().slice().sort((a, b) => b.hours - a.hours)) {
    if (e.hours < lo || e.hours > hi) continue;
    const yy = y(e.hours);
    if (Math.abs(yy - lastY) < 12) continue;
    lastY = yy;
    svg += `<line class="era" x1="${L}" x2="${W - R}" y1="${yy}" y2="${yy}"><title>${h(e.name)} (~${e.hours} h)</title></line><text x="${W - R - 4}" y="${yy - 3}" text-anchor="end" style="fill:var(--warn-text);font-size:9px">${h(e.name.slice(0, 26))}</text>`;
  }
  const labels = [];
  series.forEach((s, si) => {
    const pts = s.data.map((v, i) => (v === null || !isFinite(v) || v <= 0 ? null : [x(i), y(v)])).filter(Boolean);
    if (!pts.length) return;
    const c = colors[si % colors.length];
    svg += `<polyline class="line" stroke="${c}" ${s.dash ? 'stroke-dasharray="5 4"' : ''} points="${pts.map((p) => p.join(',')).join(' ')}"/>`;
    for (const p of pts) svg += `<circle cx="${p[0]}" cy="${p[1]}" r="2.6" fill="${c}"/>`;
    labels.push({ y: pts[pts.length - 1][1] + 3, c, text: s.label.slice(0, 24) });
  });
  // Beschriftungen rechts nicht uebereinander: nach Hoehe sortieren und mindestens 12 px Abstand
  labels.sort((a, b) => a.y - b.y);
  for (let i = 0; i < labels.length; i++) labels[i].y = Math.max(labels[i].y, i ? labels[i - 1].y + 12 : T + 8);
  const overflow = labels.length ? labels[labels.length - 1].y - (H - B) : 0;
  if (overflow > 0) labels.forEach((l) => { l.y -= overflow; });
  for (const l of labels) svg += `<text x="${W - R + 8}" y="${l.y}" style="fill:${l.c}">${h(l.text)}</text>`;
  return svg + '</svg><p class="tiny muted">Zeitachse logarithmisch. Gestrichelte waagerechte Linien: Zeitalter (Annahmen, Seite „Annahmen“).</p>';
}
function sourceForm(itemKey, editId) {
  const spec = editId ? curValue(editId) || {} : { kind: 'structure', chance: 0.01, countMin: 1, countMax: 1 };
  const structs = Object.entries(S.snapshot.structures);
  const el = $('#src-form');
  el.innerHTML = `<div class="card" style="margin-top:.8rem;background:var(--panel-2)"><h3 style="margin-top:0">${editId ? 'Geplante Quelle bearbeiten' : 'Neue Quelle planen'}</h3>
    <div class="form-grid">
      <div class="field"><label>Art</label><select id="sf-kind">${Object.entries(S.sourceKinds).map(([k, l]) => `<option value="${k}" ${spec.kind === k ? 'selected' : ''}>${h(l)}</option>`).join('')}</select></div>
      <div class="field"><label>Name</label><input id="sf-label" value="${h(spec.label || '')}" placeholder="z. B. Hexe"></div>
      <div class="field sf-struct"><label>Struktur</label><select id="sf-struct">${structs.map(([k, s]) => `<option value="${k}" ${spec.structure === k ? 'selected' : ''}>${h(s.label)}</option>`).join('')}</select></div>
      <div class="field sf-struct"><label>Behälter</label><select id="sf-cont"></select></div>
      <div class="field"><label id="sf-chance-l">Chance je Ereignis (%)</label><input id="sf-chance" inputmode="decimal" value="${h(inputText(spec.chance, 'prob'))}"></div>
      <div class="field"><label>Anzahl min</label><input id="sf-min" inputmode="numeric" value="${h(spec.countMin || 1)}"></div>
      <div class="field"><label>Anzahl max</label><input id="sf-max" inputmode="numeric" value="${h(spec.countMax || 1)}"></div>
      <div class="field sf-rate"><label>Ereignisse/h gezielt</label><input id="sf-rate" inputmode="decimal" value="${h(spec.rate ?? 10)}"></div>
      <div class="field sf-rate"><label>Ereignisse/h normal (leer = ×Faktor)</label><input id="sf-rn" inputmode="decimal" value="${h(spec.rateNormal ?? '')}"></div>
      <div class="field" style="grid-column:1/-1"><label>Notiz (für Phase 2)</label><input id="sf-note" value="${h(spec.note || '')}" placeholder="z. B. Hexe droppt beim Tod durch Spieler"></div>
    </div>
    <div class="foot" style="display:flex;gap:.5rem;margin-top:.7rem"><button class="btn primary" id="sf-ok">${editId ? 'Übernehmen' : 'Als Entwurf hinzufügen'}</button><button class="btn" id="sf-cancel">Abbrechen</button><span id="sf-err" class="tiny" style="color:var(--danger)"></span></div></div>`;
  const kindSel = $('#sf-kind');
  const fillCont = () => { const s = S.snapshot.structures[$('#sf-struct').value]; $('#sf-cont').innerHTML = Object.entries(s.containers).map(([k, c]) => `<option value="${k}" ${spec.container === k ? 'selected' : ''}>${h(c.label)}</option>`).join(''); };
  const sync = () => {
    const k = kindSel.value;
    $$('.sf-struct', el).forEach((x) => x.style.display = k === 'structure' ? '' : 'none');
    $$('.sf-rate', el).forEach((x) => x.style.display = (k === 'structure' || k === 'trader') ? 'none' : '');
    $('#sf-chance-l').textContent = k === 'structure' ? 'Chance je Kiste (%)' : k === 'trader' ? 'Chance je Händlerbesuch (%)' : k === 'mob' ? 'Chance je Tötung (%)' : 'Chance je Ereignis (%)';
  };
  kindSel.onchange = sync; $('#sf-struct').onchange = fillCont; fillCont(); sync();
  $('#sf-cancel').onclick = () => { el.innerHTML = ''; };
  $('#sf-ok').onclick = () => {
    const chance = parseInput({ type: 'prob', min: 0, max: 1 }, $('#sf-chance').value);
    const lo = parseInt($('#sf-min').value, 10), hi = parseInt($('#sf-max').value, 10);
    const err = !chance.ok || !(chance.value > 0) ? 'Chance muss größer als 0 % sein.' : !(lo >= 1 && hi >= lo && hi <= 64) ? 'Anzahl: 1 ≤ min ≤ max ≤ 64.' : '';
    const k = kindSel.value;
    const out = { kind: k, label: $('#sf-label').value.trim() || S.sourceKinds[k], chance: chance.value, countMin: lo, countMax: hi };
    if ($('#sf-note').value.trim()) out.note = $('#sf-note').value.trim();
    if (k === 'structure') { out.structure = $('#sf-struct').value; out.container = $('#sf-cont').value; }
    if (k !== 'structure' && k !== 'trader') {
      const rate = parseFloat($('#sf-rate').value.replace(',', '.'));
      if (!(rate >= 0)) { $('#sf-err').textContent = 'Ereignisse/h: Zahl ≥ 0.'; return; }
      out.rate = rate;
      const rn = $('#sf-rn').value.trim(); if (rn) out.rateNormal = parseFloat(rn.replace(',', '.'));
    }
    if (err) { $('#sf-err').textContent = err; return; }
    const id = editId || `source:${itemKey}:q${Date.now().toString(36)}`;
    setDraft(id, out);
    el.innerHTML = '';
    toast('Quelle als Entwurf hinzugefügt – die Tabelle rechnet neu.');
  };
}

function valueTable(records, opts = {}) {
  if (!records.length) return '<p class="muted">Keine Werte.</p>';
  let html = `<div class="table-wrap"><table class="table"><thead><tr>${opts.showGroup ? '<th>Bereich</th>' : ''}<th>Wert</th><th class="num">Einstellung</th><th>Status</th><th>Quelle</th></tr></thead><tbody>`;
  for (const r of records) {
    html += `<tr data-row="${h(r.id)}" class="${drafts[r.id] ? 'drafted' : ''}">${opts.showGroup ? `<td class="sub">${h((S.categories[r.category] || '').split(' (')[0])} · ${h(r.group)}</td>` : ''}<td><b>${h(r.label)}</b>${r.note && opts.notes !== false ? `<div class="sub" title="${h(r.note)}">${h(r.note.length > 140 ? r.note.slice(0, 140) + ' …' : r.note)}</div>` : ''}</td>
      <td class="num">${ed(r.id)}</td><td>${statusBadge(r.id)}${applyBadge(r)}</td><td>${srcRef(r)}</td></tr>`;
  }
  return html + '</tbody></table></div>';
}

// ---------------------------------------------------------------------------------------------
// Seite: Beute
// ---------------------------------------------------------------------------------------------
function rollDist(rolls) {
  const v = (field) => { const id = (rolls.ids || {})[field]; return id ? curValue(id) : rolls[field]; };
  if (rolls.type === 'exactly') return [[v('n'), 1]];
  if (rolls.type === 'uniform') { const lo = v('min'), hi = v('max'); const out = []; for (let n = lo; n <= hi; n++) out.push([n, 1 / (hi - lo + 1)]); return out.length ? out : [[lo, 1]]; }
  const n = v('n'), p = Math.min(1, Math.max(0, v('p')));
  const out = []; for (let k = 0; k <= n; k++) out.push([k, comb(n, k) * p ** k * (1 - p) ** (n - k)]); return out;
}
function comb(n, k) { let r = 1; for (let i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; }
function poolStats(pool) {
  const dist = rollDist(pool.rolls);
  const expRolls = dist.reduce((a, [n, p]) => a + n * p, 0);
  const weights = pool.entries.map((e) => (e.ids && e.ids.weight ? curValue(e.ids.weight) : e.weight) || 0);
  const total = weights.reduce((a, b) => a + b, 0);
  return pool.entries.map((e, i) => {
    const share = total > 0 ? weights[i] / total : 0;
    const lo = e.ids && e.ids.min ? curValue(e.ids.min) : e.count[0];
    const hi = e.ids && e.ids.max ? curValue(e.ids.max) : e.count[1];
    const atLeast = 1 - dist.reduce((a, [n, p]) => a + p * (1 - share) ** n, 0);
    return { share, atLeast, perChest: expRolls * share * (lo + hi) / 2 };
  });
}
function pageLoot(main) {
  const tables = S.snapshot.loot.tables;
  const cores = Object.values(V).filter((r) => r.category === 'loot' && r.group === 'Kern-Chancen');
  const mult = 'config:worldGen.buildingCoreLootChanceMultiplier';
  const draw = () => {
    let html = `<div class="listhead"><div><h1>Beute (Truhen, Tresore, Angeln)</h1><p class="lead">Die Pools, die die Mod an Vanilla-Tabellen hängt – gelesen aus <code>ModLootTableModifications.java</code>. Speichern schreibt jede Zahl dort hinein (26.2–26.4 und die 1.21.11-Kopie); die Inject-Tabellen unter <code>loot_table/inject/</code> baut danach Datagen. Anteil, Chance je Kiste und Ø rechnen live mit deinen Entwürfen.</p></div>
      <div class="filters"><input type="search" id="lq" placeholder="Tabelle oder Item …"></div></div>
      <div class="card" id="cores-card"><h2>Kern-Chancen</h2><p class="tiny muted">Je Kern ein eigener Pool mit einem Wurf; die Chance gilt je Kiste. Darauf wirkt der Config-Faktor ${ed(mult)} (Standard 1,0).</p>
      ${valueTable(cores.sort((a, b) => a.label.localeCompare(b.label)), { notes: true })}</div>`;
    for (const t of tables) {
      if (t.kind === 'mob') continue;
      html += `<div class="card loot-table" data-search="${h((t.label + ' ' + t.id + ' ' + t.pools.flatMap((p) => p.entries.map((e) => e.name ? e.name.de : '')).join(' ')).toLowerCase())}">
        <div class="card-head"><h2>${h(t.label)}</h2><code class="tiny">${h(t.id)}</code><span class="sp"></span>${t.gatedBy ? `<span class="badge" title="nur wenn ${h(t.gatedBy)} an ist">${h(t.gatedBy.split('.').pop())}</span>` : ''}<span class="badge b-mod" title="Speichern schreibt die Zahl in ModLootTableModifications.java (26.2–26.4 und 1.21.11); die Inject-Tabellen entstehen mit Datagen">wirkt in Mod</span><span class="badge b-lines">26.2–26.4 + 1.21.11</span></div>`;
      t.pools.forEach((pool, pi) => {
        const st = poolStats(pool);
        const r = pool.rolls;
        const rollsEd = r.type === 'exactly' ? `genau ${ed(r.ids.n)}` : r.type === 'uniform' ? `${ed(r.ids.min)} bis ${ed(r.ids.max)}` : `${ed(r.ids.n)} × Chance ${ed(r.ids.p)}`;
        const shared = pool.block && pool.block.includes('+') ? `<span class="badge" title="Derselbe Pool steht im Code für mehrere Tabellen - eine Änderung gilt für alle">gilt für ${h(pool.block.split('+').length)} Tabellen</span>` : '';
        html += `<h3>${pool.rareCore ? 'Kern-Pool' : `Pool ${pi + 1}`} · Würfe ${pool.rareCore ? `1 × ${ed(r.ids.p)}` : rollsEd} ${shared} ${pool.condition ? `<span class="badge">nur ${h(pool.condition.victim)}</span>` : ''} <span class="srcref" data-copy="${h('common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java:' + pool.line)}">Zeile ${pool.line}</span></h3>`;
        if (pool.rareCore) return;
        html += `<div class="table-wrap"><table class="table"><thead><tr><th>Eintrag</th><th class="num">Gewicht</th><th class="num">Anteil</th><th class="num">Anzahl</th><th class="num">Chance je Kiste</th><th class="num">Ø je Kiste</th></tr></thead><tbody>`;
        pool.entries.forEach((e, ei) => {
          const key = e.enchantment ? `book:${e.enchantment}:${e.level}` : e.item;
          const s = st[ei];
          html += `<tr data-row="${h((e.ids || {}).weight || '')}"><td>${e.item ? itemRef(key) : '<span class="muted">leer (nichts)</span>'}${e.randomEnchant ? ' <span class="badge">zufällig verzaubert</span>' : ''}</td>
            <td class="num">${e.ids && e.ids.weight ? ed(e.ids.weight) : num(e.weight)}</td><td class="num">${pct(s.share)}</td>
            <td class="num">${e.ids && e.ids.min ? `${ed(e.ids.min)} – ${ed(e.ids.max)}` : e.item ? '1' : ''}</td>
            <td class="num">${e.item ? pct(s.atLeast) : ''}</td><td class="num">${e.item ? num(s.perChest, 3) : ''}</td></tr>`;
        });
        html += '</tbody></table></div>';
      });
      html += '</div>';
    }
    return html;
  };
  main.innerHTML = draw();
  const filter = () => { const q = ($('#lq').value || '').toLowerCase(); $$('.loot-table').forEach((c) => c.style.display = !q || c.dataset.search.includes(q) ? '' : 'none'); };
  $('#lq').oninput = filter;
  page.onDraft = () => { const q = $('#lq').value; const y = window.scrollY; const act = document.activeElement && document.activeElement.dataset.vid; main.innerHTML = draw(); $('#lq').value = q; $('#lq').oninput = filter; filter(); window.scrollTo(0, y); if (act) { const i = $(`input[data-vid="${CSS.escape(act)}"]`); if (i) { i.focus(); i.setSelectionRange(i.value.length, i.value.length); } } };
}

// ---------------------------------------------------------------------------------------------
// Seite: Handel
// ---------------------------------------------------------------------------------------------
async function pageTrades(main) {
  const trades = S.snapshot.trades;
  const groups = {};
  for (const t of trades) { const g = (t.pools[0] && t.pools[0].label) || t.professionDe; (groups[g] = groups[g] || []).push(t); }
  const order = Object.keys(groups).sort((a, b) => (a.startsWith('Fahrender') ? 1 : 0) - (b.startsWith('Fahrender') ? 1 : 0) || a.localeCompare(b, DE));
  const draw = (offers) => {
    let html = `<h1>Handel</h1><p class="lead">Dorfbewohner- und Händler-Angebote aus <code>villager_trade/*.json</code>. <span class="badge b-mod">wirkt in Mod</span> Speichern schreibt diese Werte in die JSON-Dateien (26.2 und 26.3) und, wo die Zuordnung eindeutig ist, in <code>ModTradeDefinitions.java</code> (1.21.11) – die Linien stehen am Wert.</p>`;
    for (const g of order) {
      const pool = groups[g][0].pools[0];
      html += `<div class="card"><div class="card-head"><h2>${h(g)}</h2><span class="sp"></span>${pool ? `<span class="tiny muted">${pool.vanilla.length} Vanilla + ${pool.mod.length} Mod-Angebote, ${pool.amount ?? '?'} werden gezogen</span>` : ''}</div>
        <div class="table-wrap"><table class="table"><thead><tr><th>Gibt</th><th class="num">Menge</th><th class="num">Preis</th><th class="num">2. Preis</th><th class="num">Nutzungen</th><th class="num">XP</th><th class="num">Rabatt</th><th class="num">Angebots-Chance</th><th class="num" title="Wahrscheinlichkeit, dass ein Händler/Dorfbewohner dieses Pools das Angebot hat">im Angebot</th></tr></thead><tbody>`;
      for (const t of groups[g]) {
        const gives = (t.gives || {}).id;
        const o = offers ? offers[t.id] : null;
        html += `<tr data-row="${h(t.ids.price || '')}"><td>${itemRef(gives)}<div class="sub"><span class="srcref" data-copy="${h(t.file)}">${h(t.id.split(':')[1])}</span>${t.configFlags.length ? ' · ' + h(t.configFlags.join(', ')) : ''}</div></td>
          <td class="num">${t.ids.gives ? ed(t.ids.gives) : '1'}</td>
          <td class="num">${t.ids.price ? ed(t.ids.price) : ''}<div class="sub">${h(t.wantsName ? t.wantsName.de : '')}</div></td>
          <td class="num">${t.ids.price2 ? ed(t.ids.price2) + `<div class="sub">${h(t.alsoWantsName ? t.alsoWantsName.de : '')}</div>` : ''}</td>
          <td class="num">${ed(t.ids.maxUses)}</td><td class="num">${ed(t.ids.xp)}</td><td class="num">${ed(t.ids.discount)}</td>
          <td class="num">${t.ids.offerChance ? ed(t.ids.offerChance) : '–'}</td><td class="num"><b>${o === null || o === undefined ? '<span class="spinner"></span>' : pct(o)}</b></td></tr>`;
        if ((t.enchantPool || []).length) {
          const total = t.enchantPool.reduce((a, e) => a + (curValue(e.id) || 0), 0);
          html += `<tr><td colspan="9" style="padding-left:2.4rem"><details><summary class="muted tiny">Verzauberungs-Pool (${t.enchantPool.length})</summary><table class="table"><tbody>${t.enchantPool.map((e) => `<tr data-row="${h(e.id)}"><td>${h(e.name ? e.name.de : e.enchantment)}</td><td class="num">${ed(e.id)}</td><td class="num">${pct(total ? curValue(e.id) / total : 0)}</td></tr>`).join('')}</tbody></table></details></td></tr>`;
        }
      }
      html += '</tbody></table></div></div>';
    }
    return html;
  };
  main.innerHTML = draw(null);
  const refresh = async () => {
    try { const ov = await getOverview(); const y = window.scrollY; const act = document.activeElement && document.activeElement.dataset.vid; main.innerHTML = draw(ov.offers); window.scrollTo(0, y); if (act) { const i = $(`input[data-vid="${CSS.escape(act)}"]`); if (i) i.focus(); } } catch (err) { main.insertAdjacentHTML('afterbegin', errorBox(err)); }
  };
  page.onDraft = refresh;
  refresh();
}

// ---------------------------------------------------------------------------------------------
// Seite: Drops und Erze
// ---------------------------------------------------------------------------------------------
function pageDrops(main) {
  const snap = S.snapshot;
  const wg = Object.values(V).filter((r) => r.category === 'worldgen');
  const byFeature = {};
  for (const r of wg) (byFeature[r.group] = byFeature[r.group] || []).push(r);
  main.innerHTML = `<h1>Mob-, Block-Drops & Erze</h1>
    <div class="card"><h2>Mob-Drops der Mod</h2><p class="tiny muted">Aus <code>ModLootTableModifications</code> (charged_creeper/root). Die Rate (Tötungen je Stunde) ist eine Annahme; eigene Mob-Quellen planst du auf der Item-Seite („+ Quelle planen“).</p>
      <table class="table"><thead><tr><th>Item</th><th>Wie</th><th>Opfer</th><th class="num">Annahme</th></tr></thead><tbody>
      ${snap.mobDrops.map((m) => `<tr><td>${itemRef(m.item)}</td><td>Explosion eines geladenen Creepers</td><td>${h(m.name.de)}</td><td class="num">${ed('param:mob.charged_creeper.' + m.victim.split(':')[1])}</td></tr>`).join('')}</tbody></table></div>
    <div class="card"><h2>Block-Drops</h2><p class="tiny muted">Blöcke, die etwas anderes als sich selbst fallen lassen (aus den erzeugten Block-Loot-Tabellen).</p>
      <table class="table"><thead><tr><th>Block</th><th>Drop</th><th class="num">Anzahl</th><th>Glück</th><th class="num">Annahme (abgebaut/h)</th></tr></thead><tbody>
      ${snap.blockDrops.map((d) => `<tr><td>${itemRef(d.block)}</td><td>${itemRef(d.item)}</td><td class="num">${d.count[0]}${d.count[1] !== d.count[0] ? '–' + d.count[1] : ''}</td><td>${h(d.fortune || '–')}</td><td class="num">${ed('param:block.' + d.block)}</td></tr>`).join('')}</tbody></table></div>
    <div class="card"><h2>Erz-Generierung</h2><span class="tiny muted">aus <code>ModWorldGen.java</code> (26.2, Overlay 26.3/26.4, 1.21.11); die Dateien unter <code>worldgen/</code> baut Datagen</span>
      ${Object.entries(byFeature).map(([g, rs]) => `<h3>${h(g)}</h3>${valueTable(rs, { notes: false })}`).join('')}</div>`;
}

// ---------------------------------------------------------------------------------------------
// Seite: Werkzeuge & Ruestung
// ---------------------------------------------------------------------------------------------
function pageStats(main) {
  const props = [['durability', 'Haltbarkeit'], ['attackDamage', 'Angriff'], ['attackSpeed', 'Tempo'], ['enchantability', 'Verzauberbarkeit'], ['cooldownTicks', 'Abklingzeit'], ['bundleCapacityItems', 'Kapazität'], ['wandSquareDiameter', 'Baustab-Fläche'], ['maxStackSize', 'Stapel']];
  const families = {};
  for (const it of S.snapshot.items) if (Object.keys(it.stats).length) (families[it.family] = families[it.family] || []).push(it);
  const used = props.filter(([p]) => S.snapshot.items.some((i) => i.stats[p]));
  let html = `<h1>Werkzeuge & Rüstung</h1><p class="lead">Aus dem Item-Export (<code>src/main/generated/wiki/items.json</code>, WikiDataProvider). Jeder Wert zeigt auf seine Quelle im Code: eine Konstante (<code>ModItems.DURABILITY_IRON</code>, Material, Config-Standard der Ladungen) oder eine Zahl im Registrierungs-Code. Ändern hier ändert diese Quelle – und damit jedes Item, das sie nutzt (steht im Speichern-Dialog); der Export folgt mit Datagen. Angriffswerte und Kapazitäten sind berechnet.</p>`;
  for (const [fam, items] of Object.entries(families).sort()) {
    const cols = used.filter(([p]) => items.some((i) => i.stats[p]));
    html += `<div class="card"><div class="card-head"><h2>${h(fam)}</h2></div><div class="table-wrap"><table class="table"><thead><tr><th>Item</th>${cols.map(([, l]) => `<th class="num">${h(l)}</th>`).join('')}</tr></thead><tbody>
      ${items.map((i) => `<tr><td>${itemRef(i.id)}</td>${cols.map(([p]) => `<td class="num">${i.stats[p] ? ed(i.stats[p]) : ''}</td>`).join('')}</tr>`).join('')}</tbody></table></div></div>`;
  }
  main.innerHTML = html;
}

function pageEnchant(main) {
  let html = `<h1>Verzauberungen</h1><p class="lead">Gewicht, Stufen, Kosten und Wirkungswerte aus <code>ModEnchantments.java</code> (26.2–26.4 und 1.21.11); die Dateien unter <code>enchantment/</code> baut Datagen daraus. Wirkungen, die nur im Java-Code stecken, fehlen (siehe Auslese-Bericht).</p>`;
  for (const e of S.snapshot.enchantments) {
    const rs = Object.values(e.ids).map((id) => V[id]).filter(Boolean);
    html += `<details class="card"><summary><b>${h(e.name.de)}</b> <span class="muted">${h(e.name.en)} · max. Stufe ${e.maxLevel} · Gewicht ${e.weight}</span></summary>${valueTable(rs, { notes: false })}</details>`;
  }
  main.innerHTML = html;
}

function pageRecipes(main) {
  const recipes = S.snapshot.recipes.filter((r) => !r.easter);
  const types = [...new Set(recipes.map((r) => r.type.split(':').pop()))].sort();
  main.innerHTML = `<div class="listhead"><div><h1>Rezepte</h1><p class="lead">Ergebnis-Mengen, Garzeiten und Erfahrung aus <code>ModRecipeProvider.java</code> (26.2/26.3 und 1.21.11), wo eine eigene Zahl im Code steht; Mengen aus Vanilla-Mustern (Treppe, Stufe, Steinsäge) sind nur Planung. Zutaten zur Übersicht. Danach Datagen.</p></div>
    <div class="filters"><input type="search" id="rq" placeholder="Rezept oder Item …"><select id="rt"><option value="">alle Arten</option>${types.map((t) => `<option>${h(t)}</option>`).join('')}</select></div></div>
    <div class="card"><div class="table-wrap"><table class="table" id="rtab"><thead><tr><th class="sortable">Ergebnis</th><th class="num">Menge</th><th class="sortable">Art</th><th>Zutaten</th><th class="num">Garzeit</th><th class="num">XP</th></tr></thead><tbody id="rbody"></tbody></table></div></div>`;
  const ingr = (list) => list.map((i) => `${i.count}× ${h(i.id.split(' / ').map((x) => x.startsWith('#') ? x : nameOf(x)).join(' / '))}`).join(', ');
  const draw = () => {
    const q = $('#rq').value.toLowerCase(), t = $('#rt').value;
    const rows = recipes.filter((r) => (!t || r.type.endsWith(':' + t)) && (!q || (r.short + ' ' + (r.name ? r.name.de : '') + ' ' + r.ingredients.map((i) => i.id + ' ' + nameOf(i.id)).join(' ')).toLowerCase().includes(q)));
    $('#rbody').innerHTML = rows.slice(0, 500).map((r) => `<tr data-row="${h(r.ids.count || '')}"><td data-sort="${h(r.name ? r.name.de : r.short)}">${itemRef(r.result.id)}<div class="sub">${h(r.short)}</div></td><td class="num">${r.ids.count ? ed(r.ids.count) : r.result.count}</td><td class="sub">${h(r.type.split(':').pop())}</td><td class="sub">${ingr(r.ingredients)}</td><td class="num">${r.ids.cookingtime ? ed(r.ids.cookingtime) : ''}</td><td class="num">${r.ids.experience ? ed(r.ids.experience) : ''}</td></tr>`).join('') + (rows.length > 500 ? `<tr><td colspan="6" class="muted">… ${rows.length - 500} weitere – bitte filtern.</td></tr>` : '');
  };
  $('#rq').oninput = draw; $('#rt').onchange = draw; draw(); sortable('#rtab');
}

function pageConstants(main) {
  const byClass = {};
  for (const r of Object.values(V)) if (r.category === 'constant') (byClass[r.group] = byClass[r.group] || []).push(r);
  main.innerHTML = `<div class="listhead"><div><h1>Code-Konstanten</h1><p class="lead">Ladungen, Abklingzeiten, Reichweiten, Tempo, Haltbarkeiten – jede <code>static final</code>-Zahl im gemeinsamen Code, deren Name nach Balance klingt. Speichern ersetzt die Zahl im Code (26.2–26.4 und die 1.21.11-Kopie). Bei Ausdrücken wie <code>64*4</code> oder <code>190 * BASE_DURABILITY_MULTIPLIER</code> wird das Literal zurückgerechnet; <code>NAME * 2</code> zeigt auf NAME.</p></div>
    <div class="filters"><input type="search" id="cq" placeholder="Klasse, Name, Beschreibung …"></div></div><div id="clist"></div>`;
  const draw = () => {
    const q = $('#cq').value.toLowerCase();
    let html = '';
    for (const [cls, rs] of Object.entries(byClass).sort()) {
      const hit = rs.filter((r) => !q || (cls + ' ' + r.label + ' ' + (r.note || '')).toLowerCase().includes(q));
      if (!hit.length) continue;
      html += `<details class="card" ${q ? 'open' : ''}><summary><b>${h(cls)}</b> <span class="muted tiny">${hit.length} Werte · ${h((rs[0].source.file || '').split('/').slice(-2).join('/'))}</span></summary>${valueTable(hit)}</details>`;
    }
    $('#clist').innerHTML = html || '<div class="empty">Keine Treffer.</div>';
  };
  $('#cq').oninput = draw; draw();
}

function pageConfig(main) {
  const byTab = {};
  for (const r of Object.values(V)) if (r.category === 'config') (byTab[r.group] = byTab[r.group] || []).push(r);
  let html = `<h1>Config-Standardwerte</h1><p class="lead">Die Standards aus <code>SimplebuildingConfig.java</code>, <code>TweaksConfig.java</code> und <code>ServerTuningConfig.java</code> (Reiter Server & Modpack Tuning) – das, was ein neuer Server bekommt. Speichern ersetzt den Feld-Initialisierer (26.2–26.4 und 1.21.11); eine bestehende <code>config/simplebuilding.json</code> behält ihre Werte. Grenzen aus <code>@BoundedDiscrete</code> und <code>validate()</code>. Spieltests, die einen Standard festhalten, nennt der Speichern-Dialog.</p>`;
  for (const [tab, rs] of Object.entries(byTab)) {
    html += `<div class="card"><div class="card-head"><h2>${h(tab)}</h2></div><div class="table-wrap"><table class="table"><thead><tr><th>Option</th><th class="num">Standard</th><th>Seite</th><th>Bereich</th><th>Status</th></tr></thead><tbody>`;
    for (const r of rs) html += `<tr data-row="${h(r.id)}"><td><b>${h(r.label)}</b> <code class="tiny">${h(r.refs.path)}</code>${r.refs.subgroup && r.refs.subgroup !== tab ? ` <span class="badge">${h(r.refs.subgroup)}</span>` : ''}<div class="sub" title="${h(r.note || '')}">${h((r.note || '').slice(0, 160))}${(r.note || '').length > 160 ? ' …' : ''}</div></td>
      <td class="num">${ed(r.id)}</td><td>${h(r.refs.side)}${r.refs.onReload ? ' <span class="badge">bei /reload</span>' : ''}</td><td class="sub nowrap">${r.min !== null || r.max !== null ? `${r.min ?? '–∞'} … ${r.max ?? '∞'}` : ''}</td><td>${statusBadge(r.id)}${applyBadge(r)}</td></tr>`;
    html += '</tbody></table></div></div>';
  }
  main.innerHTML = html;
}

function pagePotionPads(main) {
  const t = S.snapshot.potionPads || { rows: [] };
  if (!t.rows.length) {
    main.innerHTML = `<h1>Trank-Pads</h1><div class="card empty">Die Regeln je Wirkung (<code>PotionPadRules.java</code>, Lauf AA) gibt es in diesem Stand noch nicht. Sobald die Datei da ist, stehen die Regeln hier – lesbar und änderbar wie alle Werte.</div>`;
    return;
  }
  main.innerHTML = `<h1>Trank-Pads: Regeln je Wirkung</h1><p class="lead">Aus <code>${h(t.file)}</code> (Tabelle <code>TABLE</code> und <code>DEFAULT</code>). Speichern schreibt die Zahl in die Regel. Wer eine Wirkung bekommt, steuern die Tags <code>#simplebuilding:potion_pad/*</code> (Datenpaket).</p>
    <div class="card"><div class="table-wrap"><table class="table"><thead><tr><th>Wirkung</th>${t.fields.map((f) => `<th class="num">${h(f.label)}</th>`).join('')}</tr></thead><tbody>
    ${t.rows.map((r) => `<tr data-row="${h(r.ids[t.fields[0].key])}"><td><b>${h(r.effect === 'default' ? 'jede andere Wirkung (DEFAULT)' : r.effect)}</b><div class="sub">Zeile ${r.line}</div></td>${t.fields.map((f) => `<td class="num">${ed(r.ids[f.key])}</td>`).join('')}</tr>`).join('')}
    </tbody></table></div></div>`;
}

// ---------------------------------------------------------------------------------------------
// Seite: Seltenheit & Zeitalter, Annahmen
// ---------------------------------------------------------------------------------------------
async function pageCalc(main) {
  main.innerHTML = `<h1>Seltenheit & Zeitalter</h1><p class="lead">Für jedes Item mit Quellen: wie lange bis zum ersten Stück – beste einzelne Quelle gezielt und normales Spiel – und zu welchem Zeitalter das passt. Alles rechnet mit deinen Entwürfen.</p>
    <div class="box box-info"><b>Zeiten sind bearbeitbar.</b> Tippe eine Zielzeit in ein Zeitfeld (z. B. <code>12</code>, <code>12,5 h</code>, <code>30 min</code>) und drücke Enter: die Zentrale rechnet aus, welche Werte diese Zeit ergeben (Kisten-Chance/Gewicht, Angebots-Chance, Drop-Annahme …), übernimmt sie als Entwurf und rechnet alle anderen Zeiten neu. Der gespeicherte Stand steht <del class="diff-old">rot durchgestrichen</del> daneben. Welche Werte sich ändern (alt → neu, Datei und Zeile, wirkt in Mod), steht je Item unter „Automatische Änderungen“ (zugeklappt). Bei mehreren Quellen ist die Verteilung dort wählbar – Standard ist proportional. Gespeichert wird erst im Speichern-Dialog.</div>
    <div class="card"><h2>Baukerne: Median bis zum k-ten Kern (gezielt, beste Quelle)</h2><div id="coretab"><span class="spinner"></span></div></div>
    <div class="card"><div class="card-head"><h2>Alle Items mit Quellen</h2><span class="sp"></span><input type="search" id="cq2" placeholder="Filtern …" style="height:32px;border:1px solid var(--border);border-radius:7px;padding:0 .6rem;background:var(--panel)"></div><div id="ovtab"><span class="spinner"></span></div></div>`;
  let seq = 0;
  const draw = async () => {
    const my = ++seq;
    const withDrafts = Object.keys(drafts).length > 0;
    const [ov, base] = await Promise.all([getOverview(), withDrafts ? getBaseOverview() : null]);
    if (my !== seq || !$('#ovtab')) return;
    const baseBy = Object.fromEntries(((base || ov).rows).map((r) => [r.item, r]));
    const q = ($('#cq2') || {}).value || '';
    const rows = ov.rows.filter((r) => !q || nameOf(r.item).toLowerCase().includes(q.toLowerCase()));
    const cell = (r, field, spec) => { const b = baseBy[r.item] || {}; return timeCell(r[field], base ? (b[field] ?? null) : undefined, spec); };
    const y = window.scrollY;
    $('#ovtab').innerHTML = `<div class="table-wrap"><table class="table" id="ovt"><thead><tr><th class="sortable">Item</th><th class="sortable">beste Quelle</th><th class="num sortable">gezielt Ø</th><th class="num sortable">Median</th><th class="num sortable">normal Ø</th><th class="num sortable">normal Median</th><th>Zeitalter (gezielt Ø)</th></tr></thead><tbody>
      ${rows.map((r) => {
        const b = baseBy[r.item] || {};
        const best = r.bestKey ? { item: r.item, row: r.bestKey, label: r.bestLabel, k: 1 } : undefined;
        const normal = { item: r.item, row: '__normal__', label: 'Normales Spiel', k: 1 };
        return `<tr><td data-sort="${h(nameOf(r.item))}" class="titem">${itemRef(r.item)}</td><td class="sub">${h(r.bestLabel || '')}${base && b.bestLabel && b.bestLabel !== r.bestLabel ? `<div class="tiny"><del class="diff-old">${h(b.bestLabel)}</del></div>` : ''}</td>${cell(r, 'bestMean', best && Object.assign({ stat: 'mean' }, best))}${cell(r, 'bestMedian', best && Object.assign({ stat: 'median' }, best))}${cell(r, 'normalMean', r.normalMean !== null ? Object.assign({ stat: 'mean' }, normal) : undefined)}${cell(r, 'normalMedian', r.normalMedian !== null ? Object.assign({ stat: 'median' }, normal) : undefined)}<td class="sub">${h(eraFor(r.bestMean))}${base && timeDiffers(b.bestMean ?? null, r.bestMean) && eraFor(b.bestMean) !== eraFor(r.bestMean) ? `<div class="tiny"><del class="diff-old">${h(eraFor(b.bestMean))}</del></div>` : ''}</td></tr>
          <tr class="tchg-row"><td colspan="7">${changesBox(r.item, null)}</td></tr>`;
      }).join('')}</tbody></table></div>`;
    sortable('#ovt');
    window.scrollTo(0, y);
    const cores = ['iron', 'gold', 'diamond', 'netherite', 'enderite', 'copper'].map((c) => `simplebuilding:${c}_core`);
    const [reports, baseReports] = await Promise.all([
      Promise.all(cores.map((c) => api('/api/calc', { item: c, overrides: overrides() }))),
      withDrafts ? Promise.all(cores.map((c) => api('/api/calc', { item: c, overrides: {} }))) : null]);
    if (my !== seq || !$('#coretab')) return;
    $('#coretab').innerHTML = `<div class="table-wrap"><table class="table"><thead><tr><th>Kern</th><th>Quelle</th><th class="num">Mittel 1.</th>${[1, 2, 3, 4, 5, 6].map((k) => `<th class="num">${k}.</th>`).join('')}<th>Zeitalter (Mittel 1.)</th></tr></thead><tbody>
      ${reports.map((r, i) => {
        if (!r.best) return '';
        const b = baseReports ? (baseReports[i].best || {}) : null;
        const tun = ((r.rows.find((x) => x.key === r.best.key) || {}).tunables || []).map((t) => t.id);
        const spec = (stat, k) => ({ item: cores[i], row: r.best.key, label: r.best.label, stat, k, tunables: tun });
        return `<tr><td class="titem">${itemRef(cores[i])}</td><td class="sub">${h(r.best.label)}${b && b.label && b.label !== r.best.label ? `<div class="tiny"><del class="diff-old">${h(b.label)}</del></div>` : ''}</td>${timeCell(r.best.mean[0], b ? (b.mean ? b.mean[0] : null) : undefined, spec('mean', 1), 'strong')}${r.best.median.map((m, k) => timeCell(m, b ? (b.median ? b.median[k] : null) : undefined, spec('median', k + 1))).join('')}<td class="sub">${h(eraFor(r.best.mean[0]))}</td></tr>
          <tr class="tchg-row"><td colspan="10">${changesBox(cores[i], r)}</td></tr>`;
      }).join('')}</tbody></table></div>
      <p class="tiny muted">Wie docs/KERNE-SELTENHEIT.md Abschnitt 5.3 (dort Eisen mit Anwesen + Mine zusammen – siehe Item-Seite „Alle Quellen gezielt“). Jede Zeit ist ein Eingabefeld.</p>`;
  };
  $('#cq2').oninput = () => draw();
  page.onDraft = draw;
  await draw();
}

function pageParams(main) {
  const byGroup = {};
  for (const r of Object.values(V)) if (r.category === 'param' && r.type !== 'json') (byGroup[r.group] = byGroup[r.group] || []).push(r);
  const drawEras = () => {
    const list = eras();
    return `<table class="table" id="eras"><thead><tr><th>Zeitalter</th><th class="num">Spielzeit (h)</th><th></th></tr></thead><tbody>
      ${list.map((e, i) => `<tr><td><input class="era-name" data-i="${i}" value="${h(e.name)}" style="width:100%;height:28px;border:1px solid var(--border);border-radius:5px;padding:0 .4rem;background:var(--bg)"></td><td class="num"><input class="era-h" data-i="${i}" value="${h(inputText(e.hours, 'float'))}" style="width:6em;height:28px;border:1px solid var(--border);border-radius:5px;padding:0 .4rem;text-align:right;background:var(--bg)"></td><td class="num"><button class="btn small danger" data-era-del="${i}">×</button></td></tr>`).join('')}
    </tbody></table><button class="btn small" id="era-add">+ Zeitalter</button> ${drafts['param:eras'] ? '<span class="badge b-draft">ungespeichert</span> <button class="btn small" data-undo="param:eras">verwerfen</button>' : ''}`;
  };
  main.innerHTML = `<h1>Annahmen der Rechner</h1><p class="lead">Wie oft ein Spieler welche Kisten öffnet, Händler trifft, Mobs tötet … <span class="badge b-tool">Rechner</span> Diese Werte wirken nie in der Mod, sind aber versioniert wie alles andere. Standards aus docs/KERNE-SELTENHEIT.md Abschnitt 2, der Rest geschätzt (steht in der Beschreibung). „normal“ leer = gezielt × Faktor normales Spiel.</p>
    <div class="card"><h2>Zeitalter</h2><div id="eras-box">${drawEras()}</div></div>
    ${Object.entries(byGroup).map(([g, rs]) => `<div class="card"><h2>${h(g)}</h2>${valueTable(rs)}</div>`).join('')}`;
  const bind = () => {
    const update = () => {
      const list = $$('#eras tbody tr').map((tr) => ({ name: tr.querySelector('.era-name').value.trim(), hours: parseFloat(tr.querySelector('.era-h').value.replace(',', '.')) }));
      if (list.some((e) => !e.name || !(e.hours > 0))) return;
      const base = eras();
      setDraft('param:eras', list.map((e, i) => Object.assign({}, base[i] || { id: 'e' + Date.now().toString(36) + i }, e)));
    };
    $$('#eras input').forEach((i) => i.onchange = update);
    $$('[data-era-del]').forEach((b) => b.onclick = () => { const list = eras().slice(); list.splice(+b.dataset.eraDel, 1); setDraft('param:eras', list); $('#eras-box').innerHTML = drawEras(); bind(); });
    $('#era-add').onclick = () => { const list = eras().slice(); list.push({ id: 'e' + Date.now().toString(36), name: 'Neues Zeitalter', hours: 50 }); setDraft('param:eras', list); $('#eras-box').innerHTML = drawEras(); bind(); };
  };
  bind();
  page.onDraft = () => { if (!document.activeElement || !document.activeElement.closest('#eras')) { $('#eras-box').innerHTML = drawEras(); bind(); } };
}

// ---------------------------------------------------------------------------------------------
// Seite: Versionen, Phase 2
// ---------------------------------------------------------------------------------------------
function pageVersions(main) {
  const hist = S.history.slice().reverse();
  main.innerHTML = `<h1>Versionen</h1><p class="lead">Jede bestätigte Speicherung ist eine Version; keine wird je gelöscht oder überschrieben. Zurücksetzen legt eine <em>neue</em> Version mit dem alten Stand an – mit derselben Vorschau und Bestätigung. Ablage: <code>${h(S.store.path)}/</code></p>
    <div class="card"><table class="table"><thead><tr><th>Version</th><th>Gespeichert</th><th>Notiz</th><th class="num">Änderungen</th><th class="num">geplante Werte</th><th></th></tr></thead><tbody>
    ${hist.map((v) => `<tr><td><a href="#/version/${v.version}"><b>v${v.version}</b></a>${v.version === S.store.version ? ' <span class="badge b-mod">aktuell</span>' : ''}${v.corrupt ? ' <span class="badge b-danger">nicht lesbar</span>' : ''}</td><td class="sub">${h((v.savedAt || '').replace('T', ' '))}</td><td>${h(v.message || '')}${v.rollbackOf !== null && v.rollbackOf !== undefined ? ` <span class="badge">Rückgängig auf v${v.rollbackOf}</span>` : ''}${v.applied ? ` <span class="badge b-mod">${v.applied} in Mod geschrieben</span>` : ''}</td>
      <td class="num">${v.changes ?? ''}</td><td class="num">${v.entries ?? ''}</td><td class="num">${v.version !== S.store.version && !v.corrupt ? `<button class="btn small" data-rollback="${v.version}">auf diesen Stand …</button>` : ''}</td></tr>`).join('')}
    </tbody></table></div>`;
}
async function pageVersion(main, n) {
  const v = await api('/api/version/' + n);
  const changes = v.changes || [];
  main.innerHTML = `<div class="crumbs"><a href="#/versions">Versionen</a> › v${v.version}</div><h1>Version v${v.version}</h1>
    <p class="lead">${h(v.message || '')} <span class="muted">${h((v.savedAt || '').replace('T', ' '))}</span></p>
    ${Number(n) !== S.store.version ? `<button class="btn" data-rollback="${v.version}">Auf diesen Stand zurücksetzen …</button>` : ''}
    <div class="card"><h2>Änderungen in dieser Version (${changes.length})</h2>${changeTable(changes.map((c) => ({ id: c.id, old: c.old, new: c.removed ? null : c.new, reset: c.removed })))}</div>
    ${(v.applied || []).length ? `<div class="card"><h2>In Mod-Dateien geschrieben (${v.applied.length} Stellen)</h2><table class="table"><tbody>${v.applied.map((a) => `<tr><td><code>${h(a.file)}</code>${a.mc ? ` <span class="badge b-lines">${h(a.mc)}</span>` : ''}</td><td>${h(a.path ? a.path.join('/') : 'Zeile ' + (a.line ?? '?'))}</td><td><span class="diff-old">${h(a.old)}</span> → <span class="diff-new">${h(a.new)}</span></td></tr>`).join('')}</tbody></table></div>` : ''}
    <div class="card"><h2>Geplante Werte in diesem Stand (${Object.keys(v.entries).length})</h2>${changeTable(Object.entries(v.entries).map(([id, e]) => ({ id, old: e.mod, new: e.value })), 'Mod beim Speichern')}</div>`;
}
function changeTable(items, oldLabel = 'vorher') {
  if (!items.length) return '<p class="muted">Keine.</p>';
  return `<div class="table-wrap"><table class="table"><thead><tr><th>Wert</th><th class="num">${h(oldLabel)}</th><th></th><th>neu</th><th>wirkt</th></tr></thead><tbody>${items.map((c) => {
    const r = rec(c.id) || { label: c.id, type: typeof c.new === 'number' ? 'float' : 'json', group: '' };
    return `<tr><td><b>${h(r.label)}</b><div class="sub">${h(r.group || '')} · <code>${h(c.id)}</code></div></td><td class="num diff-old">${h(fmtVal(c.old, r.type))}</td><td class="diff-arrow">→</td><td class="diff-new">${c.reset ? 'Mod-Wert' : h(fmtVal(c.new, r.type))}</td><td>${applyBadge(rec(c.id))}</td></tr>`;
  }).join('')}</tbody></table></div>`;
}
async function pagePhase2(main) {
  const data = await api('/api/phase2');
  const groups = Object.entries(data.groups);
  main.innerHTML = `<div class="listhead"><div><h1>Übergabe: was (noch) nicht in der Mod wirkt</h1><p class="lead">Gespeicherte Pläne, die die Zentrale nicht schreiben kann (neue Quellen, Werte ohne eigene Zahl im Code) oder die noch nicht angewendet sind – mit Grund, Datei und Zeile, damit ein Lauf sie von Hand umsetzen kann. Siehe <a href="#/docs/BALANCING-ZENTRALE.md">docs/BALANCING-ZENTRALE.md</a>.</p></div>
    <a class="btn" href="/api/phase2" download="balance-phase2.json">Als JSON herunterladen</a></div>
    ${groups.length ? groups.map(([cat, list]) => `<div class="card"><h2>${h(S.categories[cat] || cat)} (${list.length})</h2><div class="table-wrap"><table class="table"><thead><tr><th>Wert</th><th class="num">Mod</th><th></th><th>Plan</th><th>Stelle</th></tr></thead><tbody>
      ${list.map((e) => { const r = rec(e.id) || { type: 'json' }; return `<tr><td><b>${h(e.label || e.id)}</b><div class="sub">${h(e.group || '')}</div>${e.why ? `<div class="sub" style="color:var(--warn-text)">${h(e.why)}</div>` : ''}</td><td class="num diff-old">${h(fmtVal(e.mod, r.type))}</td><td class="diff-arrow">→</td><td class="diff-new">${h(r.type === 'json' ? JSON.stringify(e.planned) : fmtVal(e.planned, r.type))}</td><td>${e.source && e.source.file ? `<span class="srcref" data-copy="${h(e.source.file + (e.source.line ? ':' + e.source.line : ''))}">${h(e.source.file.split('/').slice(-2).join('/'))}${e.source.line ? ':' + e.source.line : ''}</span>` : ''}</td></tr>`; }).join('')}
    </tbody></table></div></div>`).join('') : '<div class="card empty">Nichts offen – alle gespeicherten Pläne stehen schon so in der Mod (oder es gibt noch keine).</div>'}`;
}

// ---------------------------------------------------------------------------------------------
// Seite: Doku, Bericht
// ---------------------------------------------------------------------------------------------
function pageDocs(main) {
  const groups = {};
  for (const d of S.docs) (groups[d.group] = groups[d.group] || []).push(d);
  main.innerHTML = `<h1>Dokumentation</h1><p class="lead">Die Entwickler-Dokumente aus <code>docs/</code>, live gerendert (eine Quelle: die Datei im Repo). Wiki-Themen stehen im Wiki.</p>
    ${Object.entries(groups).map(([g, ds]) => `<div class="card"><h2>${h(g)}</h2><div class="tiles">${ds.map((d) => `<a class="tile" href="#/docs/${h(d.name)}"><div style="font-weight:700">${h(d.title)}</div><div class="lbl">docs/${h(d.name)}</div></a>`).join('')}</div></div>`).join('')}`;
}
async function pageDoc(main, name) {
  const d = await api('/api/docs/' + encodeURIComponent(name));
  main.innerHTML = `<div class="crumbs"><a href="#/docs">Dokumentation</a> › ${h(d.title)} <span class="srcref" data-copy="${h(d.file)}">${h(d.file)}</span></div><article class="doc card">${d.html}</article>`;
}
function pageReport(main) {
  const r = S.snapshot.report;
  main.innerHTML = `<h1>Auslese-Bericht</h1><p class="lead">Was die Zentrale <em>nicht</em> aus dem Repo lesen konnte – und warum. Nichts davon ist eine stille Lücke.</p>
    <div class="card"><h2>Stellen im Code (${r.problems.length})</h2>${r.problems.length ? `<div class="table-wrap"><table class="table"><thead><tr><th>Bereich</th><th>Was</th><th>Warum</th><th>Wo</th></tr></thead><tbody>${r.problems.map((p) => `<tr><td>${h(p.area)}</td><td>${h(p.message)}</td><td class="sub">${h(p.why || '')}</td><td>${p.file ? `<span class="srcref" data-copy="${h(p.file + (p.line ? ':' + p.line : ''))}">${h(p.file.split('/').pop())}${p.line ? ':' + p.line : ''}</span>` : ''}</td></tr>`).join('')}</tbody></table></div>` : '<p class="muted">Keine.</p>'}</div>
    <div class="card"><h2>Grundsätzliche Grenzen</h2><table class="table"><tbody>${r.gaps.map((g) => `<tr><td><b>${h(g.area)}</b></td><td>${h(g.message)}<div class="sub">${h(g.why)}</div></td></tr>`).join('')}</tbody></table></div>
    ${r.notes.length ? `<div class="card"><h2>Hinweise beim Einlesen</h2><ul>${r.notes.map((n) => `<li>${h(n)}</li>`).join('')}</ul></div>` : ''}
    <div class="card"><h2>Zählung</h2><table class="table"><tbody>${Object.entries(S.snapshot.counts).map(([k, n]) => `<tr><td>${h(S.categories[k])}</td><td class="num">${n}</td></tr>`).join('')}</tbody></table></div>`;
}

// ---------------------------------------------------------------------------------------------
// Dialoge: Speichern, Rollback, Entwuerfe, Anwenden
// ---------------------------------------------------------------------------------------------
function openModal(html) { $('#modal-body').innerHTML = html; $('#modal').hidden = false; const f = $('#modal-body [autofocus]'); if (f) f.focus(); }
function closeModal() { $('#modal').hidden = true; $('#modal-body').innerHTML = ''; }
function draftPayload() { return Object.entries(drafts).map(([id, d]) => (d.reset ? { id, reset: true, expected: d.expected } : { id, value: d.value, expected: d.expected })); }

function summaryTable(summary) {
  const byCat = {};
  for (const s of summary) (byCat[s.categoryLabel || s.category] = byCat[s.categoryLabel || s.category] || []).push(s);
  const where = (s) => (s.sites && s.sites.length ? s.sites.map((x) => `${x.mc}: ${String(x.file || '').split('/').pop()}${x.line ? ':' + x.line : ''}`).join(' · ') : (s.file ? `${s.file.split('/').pop()}${s.line ? ':' + s.line : ''}` : ''));
  return Object.entries(byCat).map(([cat, list]) => `<h3>${h(cat)} (${list.length})</h3><div class="table-wrap"><table class="table"><tbody>${list.map((s) => `<tr><td><b>${h(s.label)}</b><div class="sub">${h(s.group || '')}${where(s) ? ' · ' + h(where(s)) : ''}</div>${s.datagen && s.apply === 'mod' ? '<div class="tiny muted">danach Datagen (erzeugte Dateien)</div>' : ''}${s.warnings.map((w) => `<div class="tiny" style="color:var(--warn-text)">⚠ ${h(w)}</div>`).join('')}</td>
    <td class="num diff-old">${h(fmtVal(s.old, s.type))}</td><td class="diff-arrow">→</td><td class="diff-new">${h(fmtVal(s.new, s.type))}${s.reset ? ' <span class="badge">Plan verworfen</span>' : ''}</td>
    <td>${s.applyNow ? `<span class="badge b-mod apply-now">wird in die Mod geschrieben</span>${s.lines && s.lines.length ? `<span class="badge b-lines">${h(linesLabel(s.lines))}</span>` : ''}` : s.apply === 'tool' ? '<span class="badge b-tool">Rechner</span>' : s.apply === 'mod' ? (s.reset ? '<span class="badge">steht so in der Mod</span>' : '<span class="badge b-planned">nur geplant</span>') : '<span class="badge b-p2">nur Planung</span>'}</td></tr>`).join('')}</tbody></table></div>`).join('');
}

async function openSave() {
  if (!Object.keys(drafts).length) return;
  openModal('<h2>Speichern</h2><span class="spinner"></span> Prüfe die Änderungen …');
  let pv;
  try { pv = await api('/api/preview', { baseVersion: S.store.version, changes: draftPayload(), applyToMod: true }); } catch (err) { openModal(`<h2>Speichern</h2>${errorBox(err)}<div class="foot"><button class="btn" onclick="closeModal()">Schließen</button></div>`); return; }
  const hasMod = pv.summary.some((s) => s.apply === 'mod');
  const blocked = pv.errors.length || pv.conflicts.length || !pv.summary.length;
  openModal(`<h2>Als v${pv.nextVersion} speichern?</h2>
    <p class="muted">${pv.summary.length} Änderungen. Alte Werte <span class="diff-old">rot</span>, neue <span class="diff-new">grün</span>. Nichts ist gespeichert, bevor du bestätigst.</p>
    ${pv.stale ? `<div class="box box-warn">Die Ablage ist inzwischen bei v${pv.baseVersion} (deine Entwürfe stammen von einem älteren Stand). Werte, die sich dazwischen geändert haben, stehen unten als Konflikt.</div>` : ''}
    ${pv.errors.length ? `<div class="box box-danger"><div class="box-title">Ungültig – so nicht speicherbar</div><ul>${pv.errors.map((e) => `<li>${h(e.message)}</li>`).join('')}</ul></div>` : ''}
    ${pv.conflicts.length ? `<div class="box box-danger"><div class="box-title">Konflikte</div><ul>${pv.conflicts.map((c) => `<li>${h(c.message)} <button class="btn small" data-rebase="${h(c.id)}">meinen Entwurf auf den neuen Stand setzen</button></li>`).join('')}</ul></div>` : ''}
    ${pv.skipped.length ? `<p class="tiny muted">${pv.skipped.length} Entwürfe entsprechen schon dem gespeicherten Wert und werden verworfen.</p>` : ''}
    ${summaryTable(pv.summary)}
    <div class="field" style="margin-top:.8rem"><label for="save-msg">Notiz zu dieser Version (was und warum?)</label><input id="save-msg" autofocus placeholder="z. B. Enderit-Kern seltener, Zeitalter B"></div>
    ${hasMod ? `<label style="display:flex;gap:.5rem;align-items:center;margin-top:.6rem"><input type="checkbox" id="save-apply" checked> Werte direkt in die Mod schreiben (JSON- und Java-Quellen aller angezeigten Linien; ohne Haken bleiben sie „geplant“)</label>` : ''}
    ${pv.summary.some((s) => s.applyNow && s.datagen) ? '<p class="tiny muted">Einige Werte erzeugen Dateien (Beute-Tabellen, Verzauberungen, Rezepte, Item-Export): danach auf der Übersicht „Datagen starten“ – bis dahin meldet checkBalance die Abweichung.</p>' : ''}
    <div class="foot"><span class="sp tiny muted">Ablage: ${h(S.store.path)}/versions/</span><button class="btn" id="save-cancel">Abbrechen</button><button class="btn primary" id="save-ok" ${blocked ? 'disabled' : ''}>Bestätigen & als v${pv.nextVersion} speichern</button></div>`);
  $('#save-cancel').onclick = closeModal;
  const applyToggle = $('#save-apply');
  if (applyToggle) applyToggle.onchange = () => $$('.apply-now').forEach((b) => { b.textContent = applyToggle.checked ? 'wird in Mod-Datei geschrieben' : 'nur geplant'; b.className = 'badge apply-now ' + (applyToggle.checked ? 'b-mod' : 'b-planned'); });
  $$('[data-rebase]').forEach((b) => b.onclick = () => { const id = b.dataset.rebase; const d = drafts[id]; if (d) { d.expected = baseValue(id); persistDrafts(); } openSave(); });
  $('#save-ok').onclick = async () => {
    $('#save-ok').disabled = true;
    const applyBox = $('#save-apply');
    const sent = draftPayload();
    try {
      const res = await api('/api/save', { baseVersion: pv.baseVersion, changes: sent, message: $('#save-msg').value, applyToMod: applyBox ? applyBox.checked : true });
      for (const c of sent) delete drafts[c.id];
      persistDrafts();
      closeModal();
      await loadState();
      route();
      toast(`<b>v${res.version} gespeichert.</b> ${res.applied ? `${res.applied} Werte in die Mod geschrieben.` : ''}${res.datagen ? ' Datagen nötig – <a href="#/">Übersicht</a>.' : ''}`);
    } catch (err) {
      $('#save-ok').disabled = false;
      $('#modal-body').insertAdjacentHTML('afterbegin', errorBox(err));
      $('#modal').scrollTop = 0;
    }
  };
}

async function openRollback(target) {
  openModal('<h2>Zurücksetzen</h2><span class="spinner"></span>');
  let pv;
  try { pv = await api('/api/rollback/preview', { target }); } catch (err) { openModal(`<h2>Zurücksetzen</h2>${errorBox(err)}<div class="foot"><button class="btn" onclick="closeModal()">Schließen</button></div>`); return; }
  const hasMod = pv.summary.some((s) => s.apply === 'mod');
  openModal(`<h2>Auf den Stand von v${target} zurücksetzen?</h2>
    <p class="muted">Das legt <b>v${pv.nextVersion}</b> neu an (v${S.store.version} und alle anderen bleiben erhalten). ${pv.summary.length} Werte ändern sich:</p>
    ${Object.keys(drafts).length ? '<div class="box box-warn">Du hast ungespeicherte Entwürfe. Sie bleiben Entwürfe und werden danach gegen den neuen Stand geprüft.</div>' : ''}
    ${pv.errors.length ? `<div class="box box-danger"><ul>${pv.errors.map((e) => `<li>${h(e.message)}</li>`).join('')}</ul></div>` : ''}
    ${pv.summary.length ? summaryTable(pv.summary) : '<p>Keine Änderung – der Stand entspricht schon v' + target + '.</p>'}
    <div class="field" style="margin-top:.8rem"><label for="rb-msg">Notiz</label><input id="rb-msg" autofocus value="Rückgängig: Stand von v${target}"></div>
    ${hasMod ? `<label style="display:flex;gap:.5rem;align-items:center;margin-top:.6rem"><input type="checkbox" id="rb-apply" checked> Werte auch in der Mod zurücksetzen (Original-Literale, alle Linien)</label>` : ''}
    <div class="foot"><button class="btn" id="rb-cancel">Abbrechen</button><button class="btn primary" id="rb-ok" ${pv.summary.length && !pv.errors.length ? '' : 'disabled'}>Bestätigen & als v${pv.nextVersion} speichern</button></div>`);
  $('#rb-cancel').onclick = closeModal;
  $('#rb-ok').onclick = async () => {
    $('#rb-ok').disabled = true;
    try {
      const res = await api('/api/rollback', { target, baseVersion: pv.baseVersion, message: $('#rb-msg').value, applyToMod: $('#rb-apply') ? $('#rb-apply').checked : true });
      closeModal(); await loadState(); route();
      toast(`<b>v${res.version} gespeichert</b> (Stand von v${target}).`);
    } catch (err) { $('#rb-ok').disabled = false; $('#modal-body').insertAdjacentHTML('afterbegin', errorBox(err)); }
  };
}

function openDraftList() {
  const items = Object.entries(drafts).map(([id, d]) => ({ id, old: d.expected, new: d.reset ? resetTarget(id) : d.value, reset: d.reset }));
  openModal(`<h2>Ungespeicherte Änderungen (${items.length})</h2><p class="muted">Liegen nur in diesem Browser (auch nach Neuladen), bis du speicherst.</p>
    <div class="table-wrap"><table class="table"><tbody>${items.map((c) => { const r = rec(c.id) || { label: c.id, type: 'json' }; return `<tr><td><a href="${pageFor(r)}?f=${encodeURIComponent(c.id)}" onclick="closeModal()"><b>${h(r.label)}</b></a><div class="sub">${h(r.group || '')}</div></td><td class="num diff-old">${h(fmtVal(c.old, r.type))}</td><td class="diff-arrow">→</td><td class="diff-new">${h(fmtVal(c.new, r.type))}</td><td>${applyBadge(rec(c.id))}</td><td><button class="btn small" data-undo="${h(c.id)}">verwerfen</button></td></tr>`; }).join('')}</tbody></table></div>
    <div class="foot"><button class="btn" onclick="closeModal()">Schließen</button><button class="btn primary" id="dl-save">Speichern …</button></div>`);
  $('#dl-save').onclick = openSave;
}

async function openApplyPlanned() {
  const data = await api('/api/pending-apply');
  openModal(`<h2>Geplante Werte anwenden?</h2><p class="muted">Diese gespeicherten Werte stehen noch nicht in der Mod. Anwenden schreibt sie hinein – in alle angezeigten Linien (keine neue Version – der Plan ist schon gespeichert).</p>
    <table class="table"><tbody>${data.pending.map((p) => `<tr><td><b>${h(p.label)}</b><div class="sub">${h(p.group)} · ${h(p.file)}${p.lines && p.lines.length ? ' · ' + h(linesLabel(p.lines)) : ''}${p.datagen ? ' · danach Datagen' : ''}</div></td><td class="num diff-old">${h(p.old)}</td><td class="diff-arrow">→</td><td class="diff-new">${h(p.new)}</td></tr>`).join('')}</tbody></table>
    <div class="foot"><button class="btn" onclick="closeModal()">Abbrechen</button><button class="btn primary" id="ap-ok">Bestätigen & schreiben</button></div>`);
  $('#ap-ok').onclick = async () => {
    try { const res = await api('/api/apply-planned', {}); closeModal(); await loadState(); route(); toast(`${res.applied} Werte in die Mod geschrieben.`); } catch (err) { $('#modal-body').insertAdjacentHTML('afterbegin', errorBox(err)); }
  };
}

// ---------------------------------------------------------------------------------------------
// Ereignisse
// ---------------------------------------------------------------------------------------------
document.addEventListener('input', (e) => {
  const t = e.target;
  if (t.matches('input[type="text"][data-vid]')) {
    const id = t.dataset.vid; let r = rec(id);
    const wrap = t.closest('.ve');
    const factor = +(t.dataset.factor || 1);
    if (factor !== 1 || wrap.dataset.alias) r = Object.assign({}, r, { min: null, max: null });
    let res = parseInput(r, t.value);
    if (res.ok && factor !== 1 && res.value !== null) {
      const v = res.value / factor;
      const target = rec(id);
      if (target.type === 'int' && Math.abs(v - Math.round(v)) > 1e-9) res = { ok: false, error: `Vielfaches von ${factor} nötig`, title: `= ${target.label} × ${factor}` };
      else res = parseInput(target, String(target.type === 'int' ? Math.round(v) : +v.toPrecision(12)).replace('.', ','));
    }
    if (res.ok && res.value !== null) {
      const tr = ((rec(id) || {}).source || {}).transform;
      const k = tr && tr.op === '*' ? tr.k : null;
      if (k && rec(id).type === 'int' && Math.abs(res.value / k - Math.round(res.value / k)) > 1e-9) {
        res = { ok: false, error: `Vielfaches von ${k * factor} nötig`, title: `im Code steht ${(rec(id).source || {}).expr || 'ein Produkt'}` };
      }
    }
    wrap.classList.toggle('invalid', !res.ok);
    let err = wrap.querySelector('.err');
    if (!res.ok) { if (!err) { err = document.createElement('span'); err.className = 'err'; wrap.appendChild(err); } err.textContent = res.error; err.title = res.title || ''; return; }
    if (err) err.remove();
    setDraft(id, res.value);
    refreshEditor(id);
  }
});
document.addEventListener('change', (e) => {
  const t = e.target;
  if (t.matches('input[type="checkbox"][data-vid]')) { setDraft(t.dataset.vid, t.checked); refreshEditor(t.dataset.vid); }
  if (t.matches('input[type="text"][data-vid]')) { refreshEditor(t.dataset.vid); }
});
document.addEventListener('change', (e) => {
  const t = e.target;
  if (t.matches('input.tin')) editTime(t);
  if (t.matches('select.tstrat')) {
    prefs.strategies = Object.assign({}, prefs.strategies || {}, { [t.dataset.item]: t.value });
    if (t.value === 'proportional') delete prefs.strategies[t.dataset.item];
    saveJson(LS_PREFS, prefs);
    for (const other of $$(`select.tstrat[data-item="${CSS.escape(t.dataset.item)}"]`)) if (other !== t) other.value = t.value;
    toast('Verteilung gemerkt – gilt für die nächste eingetippte Zeit dieses Items.');
  }
});
document.addEventListener('toggle', (e) => {
  const d = e.target;
  if (d.matches && d.matches('details.tchanges')) { if (d.open) openChanges.add(d.dataset.item); else openChanges.delete(d.dataset.item); }
}, true);
document.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && e.target.matches('input.tin')) { e.preventDefault(); e.target.blur(); }
  if (e.key === 'Escape' && e.target.matches('input.tin')) { e.target.value = hours(+e.target.dataset.t); e.target.blur(); }
});
document.addEventListener('keydown', (e) => {
  if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) { e.preventDefault(); $('#search').focus(); }
  if (e.key === 'Escape') { if (!$('#modal').hidden) closeModal(); $('#qs').hidden = true; }
  if (e.key === 'Enter' && e.target.matches('input[data-vid]')) e.target.blur();
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); openSave(); }
});
document.addEventListener('click', (e) => {
  const tb = e.target.closest('[data-tsave],[data-tdrop]');
  if (tb) {
    if (tb.dataset.tsave) openSave();
    else {
      const item = tb.dataset.tdrop;
      for (const ed of timeEdits[item] || []) for (const l of ed.lines) delete drafts[l.id];
      delete timeEdits[item]; saveTimeEdits(); draftsChanged();
      toast('Automatische Änderungen dieses Items verworfen.');
    }
    return;
  }
  const t = e.target.closest('[data-undo],[data-reset],[data-copy],[data-rollback]');
  if (!t) { if (!e.target.closest('.search')) $('#qs').hidden = true; return; }
  if (t.dataset.undo) { const id = t.dataset.undo; dropDraft(id); refreshEditor(id); if (!$('#modal').hidden && $('#modal-body h2') && $('#modal-body h2').textContent.startsWith('Ungespeichert')) { if (Object.keys(drafts).length) openDraftList(); else closeModal(); } }
  else if (t.dataset.reset) { setReset(t.dataset.reset); refreshEditor(t.dataset.reset); toast('Als Entwurf: zurück auf den Mod-Wert. Speichern unten.'); }
  else if (t.dataset.copy) { navigator.clipboard && navigator.clipboard.writeText(t.dataset.copy); toast('Kopiert: <code>' + h(t.dataset.copy) + '</code>'); }
  else if (t.dataset.rollback) openRollback(+t.dataset.rollback);
});
$('#modal').addEventListener('click', (e) => { if (e.target.id === 'modal') closeModal(); });
$('#pending-save').onclick = openSave;
$('#pending-list').onclick = openDraftList;
$('#pending-discard').onclick = () => {
  const n = Object.keys(drafts).length;
  openModal(`<h2>${n} Entwürfe verwerfen?</h2><p>Die ungespeicherten Änderungen gehen verloren. Gespeicherte Versionen bleiben unberührt.</p><div class="foot"><button class="btn" onclick="closeModal()">Abbrechen</button><button class="btn danger" id="disc-ok">Verwerfen</button></div>`);
  $('#disc-ok').onclick = () => { drafts = {}; draftsChanged(); closeModal(); route(); };
};
$('#search').addEventListener('input', (e) => runSearch(e.target.value));
$('#search').addEventListener('keydown', (e) => {
  const items = $$('#qs .qs-item'); if (!items.length) return;
  let i = items.findIndex((x) => x.getAttribute('aria-selected') === 'true');
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') { e.preventDefault(); items[i] && items[i].removeAttribute('aria-selected'); i = (i + (e.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length; items[i].setAttribute('aria-selected', 'true'); items[i].scrollIntoView({ block: 'nearest' }); }
  if (e.key === 'Enter') { e.preventDefault(); const sel = items[Math.max(0, i)]; location.hash = sel.getAttribute('href'); $('#search').value = ''; $('#qs').hidden = true; $('#search').blur(); }
});
$('#qs').addEventListener('click', () => { $('#search').value = ''; $('#qs').hidden = true; });
$('#theme-btn').onclick = () => {
  const dark = document.documentElement.getAttribute('data-theme') === 'dark' || (!document.documentElement.getAttribute('data-theme') && matchMedia('(prefers-color-scheme: dark)').matches);
  const next = dark ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  saveJson('simplebuilding-wiki-theme', next); try { localStorage.setItem('simplebuilding-wiki-theme', next); } catch (err) { /* */ }
};
$('#reload-btn').onclick = async () => {
  $('#reload-btn').disabled = true;
  try { const r = await api('/api/reload', {}); await loadState(); route(); toast(`Neu eingelesen (${num(r.seconds)} s).`); } catch (err) { toast(h(err.message), true); }
  $('#reload-btn').disabled = false;
};
$('#menu-btn').onclick = () => document.body.classList.toggle('nav-open');
$('#backdrop').onclick = () => document.body.classList.remove('nav-open');
window.addEventListener('hashchange', route);
window.addEventListener('beforeunload', (e) => { if (Object.keys(drafts).length) { persistDrafts(); } });

(async function start() {
  try { await loadState(); route(); } catch (err) { $('#main').innerHTML = errorBox(err) + '<p>Läuft der Server? <code>python tools/devserver/serve.py</code></p>'; }
})();
