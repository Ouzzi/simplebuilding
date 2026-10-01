/* Launch- und Testzentrale - Oberflaeche. Kein Framework: Hash-Router, Bereiche als Funktionen, die DOM
   bauen (nie innerHTML mit Server- oder Testtext), Prozesskarten die an Ort und Stelle aktualisiert werden. */
'use strict';

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

function h(tag, attrs, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'text') el.textContent = v;
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (v === true) el.setAttribute(k, '');
    else el.setAttribute(k, v);
  }
  for (const kid of kids.flat(Infinity)) {
    if (kid === null || kid === undefined || kid === false) continue;
    el.append(kid instanceof Node ? kid : document.createTextNode(String(kid)));
  }
  return el;
}

// ---------------------------------------------------------------------------------------------
// Grundfunktionen
// ---------------------------------------------------------------------------------------------
const LS = 'launchhub-v1';
let prefs = { route: 'launch', launchWorkspace: null, tests: { target: '', group: true, failedOnly: false, flakyOnly: false, q: '' }, history: { mutations: false, redOnly: false }, open: {} };
try { prefs = Object.assign(prefs, JSON.parse(localStorage.getItem(LS)) || {}); } catch (e) { /* ohne Storage */ }
function savePrefs() { try { localStorage.setItem(LS, JSON.stringify(prefs)); } catch (e) { /* ohne Storage */ } }

async function api(path, body) {
  const opt = body === undefined ? {} : { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Hub-Client': '1' }, body: JSON.stringify(body) };
  const res = await fetch(path, opt);
  const type = res.headers.get('Content-Type') || '';
  if (!type.includes('json')) {
    const text = await res.text();
    if (!res.ok) throw Object.assign(new Error(text || res.statusText), { status: res.status });
    return text;
  }
  const data = await res.json();
  if (!res.ok) throw Object.assign(new Error(data.error || res.statusText), { status: res.status, data });
  return data;
}

function fill(el, ...kids) { el.replaceChildren(...kids.flat(Infinity).filter((k) => k !== null && k !== undefined && k !== false)); }

function toast(text, kind = '') {
  const el = h('div', { class: 'toast ' + kind, text });
  $('#toasts').append(el);
  setTimeout(() => el.remove(), kind === 'bad' ? 12000 : 6000);
}

async function copy(text, note = 'Kopiert') {
  try { await navigator.clipboard.writeText(text); }
  catch (e) {
    const ta = h('textarea', { style: 'position:fixed;opacity:0' }); ta.value = text; document.body.append(ta); ta.select();
    try { document.execCommand('copy'); } catch (err) { /* nichts */ } ta.remove();
  }
  toast(note);
}

function fmtAgo(iso) {
  if (!iso) return '-';
  const s = Math.max(0, (Date.now() - Date.parse(iso)) / 1000);
  if (s < 90) return 'gerade eben';
  if (s < 5400) return `vor ${Math.round(s / 60)} min`;
  if (s < 172800) return `vor ${Math.round(s / 3600)} h`;
  return `vor ${Math.round(s / 86400)} Tagen`;
}
function fmtSecs(sec) { sec = Math.round(sec); return sec < 90 ? `${sec} s` : `${Math.floor(sec / 60)} min ${sec % 60} s`; }
function fmtMs(ms) { return ms ? fmtSecs(ms / 1000) : '-'; }
function fmtBytes(b) { if (b === null || b === undefined) return '...'; const u = ['B', 'KB', 'MB', 'GB', 'TB']; let i = 0; while (b >= 1024 && i < 4) { b /= 1024; i++; } return `${b.toFixed(i ? 1 : 0)} ${u[i]}`; }
function shortId(id) { return String(id).replace(/^simplebuilding:/, ''); }
function download(name, text) { const a = h('a', { href: URL.createObjectURL(new Blob([text], { type: 'text/plain' })), download: name }); document.body.append(a); a.click(); a.remove(); }

function spark(values, { w = 110, ht = 26, color = 'var(--c1)' } = {}) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${w} ${ht}`); svg.setAttribute('width', w); svg.setAttribute('height', ht); svg.setAttribute('class', 'spark'); svg.setAttribute('aria-hidden', 'true');
  if (values.length < 2) return svg;
  const max = Math.max(...values), min = Math.min(...values), span = max - min || 1;
  const pts = values.map((v, i) => `${(i / (values.length - 1)) * (w - 4) + 2},${ht - 3 - ((v - min) / span) * (ht - 6)}`).join(' ');
  const line = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
  line.setAttribute('points', pts); line.setAttribute('stroke', color); svg.append(line);
  return svg;
}

// Dialoge -------------------------------------------------------------------------------------
let modalReturn = null;
function closeModal() { const m = $('#modal'); m.hidden = true; $('#modal-body').replaceChildren(); if (modalReturn) { try { modalReturn.focus(); } catch (e) { /* weg */ } modalReturn = null; } }
function openModal(title, content, buttons = [{ label: 'Schliessen' }]) {
  modalReturn = document.activeElement;
  const foot = h('div', { class: 'foot' }, buttons.map((b) => h('button', {
    class: 'btn' + (b.primary ? ' primary' : '') + (b.danger ? ' danger' : ''), disabled: b.disabled, type: 'button',
    onclick: async () => { if (b.action) { const keep = await b.action(); if (keep === true) return; } closeModal(); },
  }, b.label)));
  $('#modal-body').replaceChildren(h('h2', { id: 'modal-title' }, title), content, foot);
  $('#modal').hidden = false;
  const first = $('#modal-body button.primary') || $('#modal-body button');
  if (first) first.focus();
  return { foot };
}
function confirmDialog(title, text, okLabel = 'OK', danger = false) {
  return new Promise((resolve) => {
    let done = false;
    const finish = (v) => { if (!done) { done = true; resolve(v); } };
    openModal(title, h('div', {}, typeof text === 'string' ? h('p', { text }) : text), [
      { label: 'Abbrechen', action: () => finish(false) },
      { label: okLabel, primary: !danger, danger, action: () => finish(true) },
    ]);
    $('#modal').addEventListener('click', function once(e) { if (e.target === this) { finish(false); this.removeEventListener('click', once); } });
  });
}
document.addEventListener('click', (e) => { if (e.target.id === 'modal') closeModal(); });

// Kontextmenue (Tastatur: Pfeile, Pos1/Ende, Enter, Esc) ---------------------------------------
let ctxEl = null;
function closeMenu() { if (ctxEl) { ctxEl.remove(); ctxEl = null; } }
function showMenu(x, y, items, returnFocus) {
  closeMenu();
  const menu = h('div', { class: 'ctx', role: 'menu' });
  for (const it of items) {
    if (it === '-') { menu.append(h('hr', { role: 'separator' })); continue; }
    menu.append(h('button', { type: 'button', role: 'menuitem', disabled: it.disabled, class: it.danger ? 'danger' : '', onclick: () => { closeMenu(); if (returnFocus) returnFocus.focus(); it.action(); } }, it.label));
  }
  document.body.append(menu); ctxEl = menu;
  const r = menu.getBoundingClientRect();
  menu.style.left = Math.max(8, Math.min(x, innerWidth - r.width - 8)) + 'px';
  menu.style.top = Math.max(8, Math.min(y, innerHeight - r.height - 8)) + 'px';
  const buttons = () => $$('button:not(:disabled)', menu);
  (buttons()[0] || menu).focus();
  menu.addEventListener('keydown', (e) => {
    const list = buttons(); const i = list.indexOf(document.activeElement);
    if (e.key === 'ArrowDown') { e.preventDefault(); list[(i + 1) % list.length].focus(); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); list[(i - 1 + list.length) % list.length].focus(); }
    else if (e.key === 'Home') { e.preventDefault(); list[0].focus(); }
    else if (e.key === 'End') { e.preventDefault(); list[list.length - 1].focus(); }
    else if (e.key === 'Escape' || e.key === 'Tab') { e.preventDefault(); closeMenu(); if (returnFocus) returnFocus.focus(); }
  });
}
document.addEventListener('mousedown', (e) => { if (ctxEl && !ctxEl.contains(e.target)) closeMenu(); });
addEventListener('scroll', closeMenu, true);
addEventListener('resize', closeMenu);

// ---------------------------------------------------------------------------------------------
// Zustand, Umfrage, Kopfleiste
// ---------------------------------------------------------------------------------------------
let S = null;          // /api/state
let T = null;          // /api/targets
let jobs = [];         // /api/processes
let prevStatus = new Map();
let view = null;       // aktueller Bereich {cleanup, onJobs, onFinished}
const SECTIONS = [
  ['mods', 'Mods', '?'],
  ['launch', 'Starten', '▶'], ['tests', 'Tests', '✔'], ['failures', 'Fehlschlaege', '✖'],
  ['history', 'Verlauf', '⧖'], ['ai', 'KI-Fixes', '✦'], ['worktrees', 'Worktrees', '⑂'], ['settings', 'Einstellungen', '⚙'],
];

function renderStrip() {
  if (!S) return;
  const g = S.git, d = S.disk, p = S.processes, lr = S.lastRun;
  const pills = [];
  if (S.dryRun) pills.push(h('span', { class: 'pill dry', title: 'Nichts wird gestartet, Befehle werden nur ausgegeben' }, 'Trockenlauf (dry run)'));
  const low = d.freeGb >= 0 && d.freeGb < d.minFreeGb;
  pills.push(h('span', { class: 'pill ' + (low ? 'bad' : d.freeGb < d.minFreeGb * 2 ? 'warn' : 'ok'), title: `Start ab ${d.minFreeGb} GB frei` }, 'Platz ', h('b', {}, d.freeGb < 0 ? '?' : `${d.freeGb} GB`)));
  pills.push(h('span', { class: 'pill ' + (p.active ? 'warn' : '') }, 'Prozesse ', h('b', {}, String(p.active)), p.clients || p.servers ? ` (${p.clients} Client, ${p.servers} Server)` : ''));
  pills.push(h('span', { class: 'pill ' + (lr ? (lr.verdict === 'green' ? 'ok' : lr.verdict === 'red' ? 'bad' : '') : 'warn'), title: lr ? lr.runId : '' }, 'Letzter Lauf ', h('b', {}, lr ? fmtAgo(new Date(Date.now() - lr.ageSeconds * 1000).toISOString()) : 'keiner'), lr ? ` · ${lr.verdict === 'green' ? 'alles gruen' : lr.verdict}` : ''));
  pills.push(h('span', { class: 'pill ' + (g.dirty ? 'warn' : '') }, 'Git ', h('b', {}, g.branch), ' ', g.short, g.dirty ? ` · ${g.dirtyCount} ungesichert` : ' · sauber'));
  if (g.ahead !== null) pills.push(h('span', { class: 'pill ' + (g.behind ? 'warn' : g.ahead ? '' : 'ok'), title: 'gegenueber ' + (g.upstream || '?') + ' (Stand des letzten fetch)' }, g.ahead ? `${g.ahead} nicht gepusht` : 'gepusht', g.behind ? ` · ${g.behind} hinten` : ''));
  $('#strip').replaceChildren(...pills);
}
function renderNav() {
  const failing = S && S.lastRun ? null : null; // Zaehler kommen aus den Bereichen selbst
  const active = jobs.filter((j) => ['starting', 'running', 'stopping'].includes(j.status));
  const nav = $('#sidebar');
  nav.replaceChildren(h('div', { class: 'sb-title' }, 'Bereiche'), ...SECTIONS.map(([id, label, ico]) => {
    let badge = null;
    if (id === 'launch') { const n = active.filter((j) => j.kind === 'launch').length; if (n) badge = h('span', { class: 'n live' }, String(n)); }
    if (id === 'tests') { const n = active.filter((j) => j.kind === 'test' || j.kind === 'check').length; if (n) badge = h('span', { class: 'n live' }, String(n)); }
    if (id === 'ai') { const n = active.filter((j) => j.kind === 'ai').length; if (n) badge = h('span', { class: 'n live' }, String(n)); }
    if (id === 'failures' && failCount !== null && failCount > 0) badge = h('span', { class: 'n bad' }, String(failCount));
    return h('a', { href: '#/' + id, 'aria-current': routeName() === id ? 'page' : null }, h('span', { class: 'sb-ico', 'aria-hidden': 'true' }, ico), label, badge);
  }), h('div', { class: 'sb-title' }, 'Weitere Werkzeuge'),
  h('a', { href: 'http://127.0.0.1:8765/', target: '_blank', rel: 'noopener' }, h('span', { class: 'sb-ico' }, '↗'), 'Wiki'),
  h('a', { href: 'http://127.0.0.1:8770/', target: '_blank', rel: 'noopener' }, h('span', { class: 'sb-ico' }, '↗'), 'Balancing-Zentrale'));
  void failing;
}
let failCount = null;

async function refreshState() {
  try { S = await api('/api/state'); renderStrip(); } catch (e) { $('#strip').replaceChildren(h('span', { class: 'pill bad' }, 'Server nicht erreichbar: ' + e.message)); }
}
async function refreshJobs() {
  let list;
  try { list = (await api('/api/processes')).processes; } catch (e) { return; }
  for (const j of list) {
    const before = prevStatus.get(j.id);
    const activeBefore = before && ['starting', 'running', 'stopping'].includes(before);
    if (activeBefore && !['starting', 'running', 'stopping'].includes(j.status)) announceFinished(j);
    prevStatus.set(j.id, j.status);
  }
  jobs = list;
  renderNav();
  if (view && view.onJobs) view.onJobs(jobs);
}
function announceFinished(j) {
  const v = j.meta && j.meta.verdict;
  let text = `${j.label}: `;
  let kind = '';
  if (j.dry) text += 'Trockenlauf beendet';
  else if (j.kind === 'test' || j.kind === 'check') { text += v === 'green' ? 'alles gruen' : v === 'red' ? 'ROT' : 'beendet (kein Ergebnis im Datensatz)'; kind = v === 'green' ? '' : 'bad'; }
  else if (j.status === 'failed') { text += `fehlgeschlagen (Exit ${j.exitCode})`; kind = 'bad'; }
  else text += 'beendet';
  toast(text, kind);
  if (j.kind === 'test' || j.kind === 'check') { refreshState(); loadFailCount(); if (view && view.onFinished) view.onFinished(j); }
}
async function loadFailCount() { try { failCount = (await api('/api/failures')).rows.length; renderNav(); } catch (e) { /* egal */ } }

// ---------------------------------------------------------------------------------------------
// Prozesskarten und Log
// ---------------------------------------------------------------------------------------------
const ACTIVE = ['starting', 'running', 'stopping'];
const STATUS_TEXT = { starting: 'Startet', running: 'Laeuft', stopping: 'Wird beendet', stopped: 'Beendet', failed: 'Fehlgeschlagen', lost: 'Verloren (Hub neu gestartet)' };

function logView(jobId, { live }) {
  let after = 0; let lines = []; let timer = null; let follow = true;
  const pre = h('pre', { class: 'log', tabindex: '0', 'aria-label': 'Log' });
  const search = h('input', { type: 'search', placeholder: 'Im Log suchen', 'aria-label': 'Im Log suchen', oninput: draw });
  const followBox = h('input', { type: 'checkbox', checked: true, onchange: () => { follow = followBox.checked; } });
  function draw() {
    const q = search.value.toLowerCase();
    pre.replaceChildren();
    const shown = q ? lines.filter((l) => l.toLowerCase().includes(q)) : lines;
    if (!shown.length) { pre.textContent = lines.length ? 'Keine Treffer.' : 'Noch keine Ausgabe.'; return; }
    if (!q) pre.textContent = shown.join('\n');
    else for (const l of shown) { const i = l.toLowerCase().indexOf(q); pre.append(l.slice(0, i), h('mark', {}, l.slice(i, i + q.length)), l.slice(i + q.length) + '\n'); }
    if (follow) pre.scrollTop = pre.scrollHeight;
  }
  async function poll() {
    try {
      const d = await api(`/api/process/${jobId}?after=${after}`);
      if (d.tail.length) { lines = lines.concat(d.tail).slice(-6000); draw(); }
      after = d.next;
      if (!ACTIVE.includes(d.status)) { stop(); }
    } catch (e) { stop(); }
  }
  function stop() { if (timer) { clearInterval(timer); timer = null; } }
  poll(); if (live) timer = setInterval(poll, 1000);
  const box = h('div', { class: 'logbox' },
    h('div', { class: 'logbar' }, search, h('label', { class: 'check tiny' }, followBox, 'Mitlaufen'),
      h('button', { class: 'btn small', type: 'button', onclick: () => copy(lines.join('\n'), 'Log kopiert') }, 'Kopieren'),
      h('a', { class: 'btn small', href: `/api/process/${jobId}/log?download=1`, download: `${jobId}.log` }, 'Herunterladen')),
    pre);
  box.stopLog = stop; box.restart = () => { if (!timer && live) timer = setInterval(poll, 1000); poll(); };
  return box;
}

function statusPill(j) {
  return h('span', { class: 'badge ' + (j.status === 'failed' || j.status === 'lost' ? 'b-bad' : ACTIVE.includes(j.status) ? 'b-warn' : 'b-ok') }, h('span', { class: 'dot ' + j.status }), STATUS_TEXT[j.status] || j.status);
}

function processCard(j) {
  const card = h('div', { class: 'pcard', dataset: { id: j.id } });
  const head = h('div', { class: 'pcard-head' }); const body = h('div', { class: 'pcard-body' });
  const logHost = h('div', {});
  let logShown = prefs.open['log:' + j.id] || false;
  let lv = null;
  card.append(head, body, logHost);
  function toggleLog() {
    logShown = !logShown; prefs.open['log:' + j.id] = logShown; savePrefs();
    if (logShown) { lv = logView(j.id, { live: ACTIVE.includes(j.status) }); logHost.replaceChildren(h('div', { class: 'pcard-body' }, lv)); }
    else { if (lv) lv.stopLog(); logHost.replaceChildren(); lv = null; }
    update(cur);
  }
  let cur = j;
  function update(job) {
    cur = job;
    const active = ACTIVE.includes(job.status);
    card.classList.toggle('active', active);
    const meta = job.meta || {};
    fill(head,
      statusPill(job), h('span', { class: 'title' }, job.label),
      meta.workspace ? h('span', { class: 'badge b-info' }, meta.workspace === 'gate' ? 'Gate-Worktree' : 'Repo') : null,
      job.dry ? h('span', { class: 'badge b-end' }, job.dry === 'sim' ? 'dry run (simuliert)' : 'dry run') : null,
      meta.verdict ? h('span', { class: 'badge ' + (meta.verdict === 'green' ? 'b-ok' : meta.verdict === 'red' ? 'b-bad' : 'b-info') }, meta.verdict === 'green' ? 'alles gruen' : meta.verdict) : null,
      h('span', { class: 'sp' }),
      active ? h('button', { class: 'btn small', type: 'button', onclick: () => stopJob(job.id, 'stop') }, 'Stoppen') : null,
      active && job.canStdin ? h('button', { class: 'btn small', type: 'button', onclick: () => stopJob(job.id, 'graceful') }, 'Server sauber stoppen') : null,
      active ? h('button', { class: 'btn small danger', type: 'button', onclick: () => stopJob(job.id, 'kill') }, 'Beenden erzwingen') : null,
      h('button', { class: 'btn small', type: 'button', 'aria-expanded': String(logShown), onclick: toggleLog }, logShown ? 'Log ausblenden' : 'Log'),
      h('button', { class: 'btn small', type: 'button', title: 'Ordner mit der Logdatei oeffnen', onclick: () => openPath({ what: 'job-log', id: job.id }) }, 'Ordner'));
    const started = Date.parse(job.startedAt); const ended = job.endedAt ? Date.parse(job.endedAt) : Date.now();
    const parts = [h('span', {}, `Start ${fmtAgo(job.startedAt)}`), h('span', {}, `Dauer ${fmtSecs((ended - started) / 1000)}`)];
    if (job.exitCode !== null && job.exitCode !== undefined) parts.push(h('span', {}, `Exit-Code ${job.exitCode}`));
    if (meta.target) parts.push(h('span', {}, meta.target));
    if (job.steps && job.steps.length) parts.push(h('span', { title: job.steps.join('\n') }, `${job.steps.length} Schritt(e)`));
    fill(body, h('div', { class: 'pmeta' }, parts));
    if (job.kind === 'ai') body.append(aiExtras(job));
    if (job.kind === 'test' && meta.runs && meta.runs.length) body.append(h('div', { class: 'pmeta' }, h('span', {}, 'Datensatz: '), ...meta.runs.map((r) => h('a', { href: '#/history', onclick: (e) => { e.preventDefault(); showRun(r); } }, r))));
  }
  card.update = update; update(j);
  if (logShown) { logShown = false; toggleLog(); }
  return card;
}

function aiExtras(job) {
  const m = job.meta || {}; const r = m.result;
  const box = h('div', { style: 'margin-top:.5rem' });
  box.append(h('div', { class: 'pmeta' }, h('span', {}, 'Branch '), h('code', {}, m.branch || '?'), h('span', {}, m.provider), h('span', {}, `${(m.tests || []).length} Test(s)`)));
  if (r) {
    box.append(h('div', { class: 'pmeta', style: 'margin-top:.3rem' },
      h('span', {}, 'Commit ', h('code', {}, r.short || '-')), h('span', {}, `${r.commits} Commit(s)`), h('span', {}, `${r.changed.length} geaenderte Datei(en)`),
      r.uncommitted.length ? h('span', { class: 'badge b-warn' }, `${r.uncommitted.length} ungesicherte Aenderung(en)`) : null,
      r.conflicts === true ? h('span', { class: 'badge b-bad' }, 'Merge-Konflikt mit dem Ausgangspunkt') : r.conflicts === false ? h('span', { class: 'badge b-ok' }, 'Merge-Vorschau: sauber') : null));
    if (r.changed.length) box.append(h('ul', { class: 'tiny', style: 'margin:.4rem 0 0;padding-left:1.1rem' }, r.changed.slice(0, 40).map((c) => h('li', {}, h('code', {}, c.status), ' ', c.path))));
  }
  const btns = h('div', { class: 'btn-row', style: 'margin-top:.5rem' },
    r ? h('button', { class: 'btn small', type: 'button', onclick: () => showDiff(job.id) }, 'Diff oeffnen') : null,
    m.promptFile ? h('button', { class: 'btn small', type: 'button', onclick: () => showAiFile(job.id, 'prompt') }, 'Prompt ansehen') : null,
    m.branch ? h('button', { class: 'btn small', type: 'button', onclick: () => copy(`git merge ${m.branch}`, 'Merge-Befehl kopiert (nichts wurde gemerged)') }, 'Merge-Befehl kopieren') : null,
    m.worktree ? h('button', { class: 'btn small', type: 'button', onclick: () => openPath({ what: 'worktree', path: m.worktree }) }, 'Worktree oeffnen') : null);
  box.append(btns);
  return box;
}

async function stopJob(id, mode) {
  try { await api('/api/process/stop', { id, mode }); toast(mode === 'kill' ? 'Prozessbaum wird beendet' : 'Stopp angefordert'); refreshJobs(); }
  catch (e) { toast(e.message, 'bad'); }
}
async function openPath(body) { try { const r = await api('/api/open', body); toast(r.dryRun ? `dry run: wuerde oeffnen ${r.opened}` : 'Geoeffnet'); } catch (e) { toast(e.message, 'bad'); } }

// Liste von Prozesskarten, die nur ergaenzt/aktualisiert wird
function processList(filter, emptyText) {
  const host = h('div', {}); const cards = new Map();
  const empty = h('div', { class: 'empty' }, h('b', {}, 'Nichts unterwegs'), emptyText);
  const box = h('div', {});
  host.append(empty, box);
  function sync(all) {
    const list = all.filter(filter).slice(0, 25);
    for (const [id, card] of cards) if (!list.find((j) => j.id === id)) { card.remove(); cards.delete(id); }
    let expected = box.firstElementChild;
    for (const j of list) {
      let card = cards.get(j.id);
      if (!card) { card = processCard(j); cards.set(j.id, card); } else card.update(j);
      if (card !== expected) box.insertBefore(card, expected); else expected = expected.nextElementSibling;
    }
    empty.hidden = list.length > 0;
  }
  host.sync = sync; sync(jobs);
  return host;
}

// ---------------------------------------------------------------------------------------------
// Aktionen
// ---------------------------------------------------------------------------------------------
async function post(path, body, okText) {
  try {
    const r = await api(path, body);
    const w = r.warnings || [];
    toast(okText || 'Gestartet', '');
    for (const t of w) toast(t, 'warn');
    refreshJobs(); refreshState();
    return r;
  } catch (e) {
    if (e.data && e.data.needsForce && !body.force) {
      if (await confirmDialog('Trotzdem hier bauen?', e.message, 'Trotzdem starten', true)) return post(path, { ...body, force: true }, okText);
      return null;
    }
    toast(e.message, 'bad'); return null;
  }
}
const launchWs = () => prefs.launchWorkspace || (S && S.settings.launchWorkspace) || 'repo';
const testWs = () => (S && S.settings.testWorkspace) || 'gate';
function doLaunch(target, action) { return post('/api/launch', { target, action, workspace: launchWs() }, { client: 'Client wird gestartet', server: 'Server wird gestartet', client_fresh: 'Client mit frischem Testzentrum wird gestartet' }[action]); }
function doTests(body, text) { return post('/api/tests/run', { workspace: testWs(), ...body }, text || 'Testlauf gestartet'); }
function runAll() { return doTests({ mode: 'targets', preset: 'main' }, 'Alle Tests (26.3) gestartet'); }
function runFailed() { return doTests({ mode: 'failed' }, 'Fehlgeschlagene Tests werden erneut ausgefuehrt'); }

// ---------------------------------------------------------------------------------------------
// Bereich: Starten
// ---------------------------------------------------------------------------------------------
function loaderMenu(entry, anchor, x, y) {
  const running = jobs.filter((j) => ACTIVE.includes(j.status) && j.kind === 'launch' && j.meta.target === entry.id);
  const lastLog = jobs.find((j) => j.kind === 'launch' && j.meta.target === entry.id);
  const items = [
    { label: 'Client + frische Testwelt mit Testzentrum', action: () => doLaunch(entry.id, 'client_fresh') },
    { label: 'Client', action: () => doLaunch(entry.id, 'client') },
    { label: 'Server', action: () => doLaunch(entry.id, 'server') },
    '-',
    { label: 'Tests (Server)', action: () => doTests({ mode: 'targets', targets: [entry.testTarget] }) },
    { label: 'Client-Tests', disabled: !entry.clientTestTarget, action: () => doTests({ mode: 'targets', targets: [entry.clientTestTarget] }) },
    '-',
    ...running.map((j) => ({ label: `Stoppen: ${j.label}`, action: () => stopJob(j.id, 'stop') })),
    ...running.map((j) => ({ label: `Beenden erzwingen: ${j.label}`, danger: true, action: () => stopJob(j.id, 'kill') })),
    running.length ? '-' : null,
    { label: 'Log oeffnen', disabled: !lastLog, action: () => openLogModal(lastLog) },
    { label: 'Laufordner oeffnen', action: () => openPath({ what: 'run-dir', target: entry.id, workspace: launchWs() }) },
  ].filter((i) => i !== null);
  showMenu(x, y, items, anchor);
}
function openLogModal(job) {
  const lv = logView(job.id, { live: ACTIVE.includes(job.status) });
  openModal(job.label, lv, [{ label: 'Schliessen', action: () => { lv.stopLog(); } }]);
}

async function viewLaunch(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' }), ' Ziele werden geladen ...'));
  const modPicker = await launchModPicker();
  const list = processList((j) => ['launch', 'gate'].includes(j.kind), 'Starte oben einen Client oder Server. Jede Instanz bekommt hier eine Karte mit Live-Log.');
  const wsSeg = h('div', { class: 'seg', role: 'group', 'aria-label': 'Arbeitsordner fuer Start' }, ['repo', 'gate'].map((w) => h('button', {
    type: 'button', 'aria-pressed': String(launchWs() === w), title: w === 'repo' ? 'Dieses Repo (wie bisher)' : 'Gate-Worktree im Temp-Ordner',
    onclick: (e) => { prefs.launchWorkspace = w; savePrefs(); $$('button', e.target.parentNode).forEach((b) => b.setAttribute('aria-pressed', String(b === e.target))); },
  }, w === 'repo' ? 'Repo' : 'Gate-Worktree')));
  const low = S && S.disk.freeGb >= 0 && S.disk.freeGb < S.disk.minFreeGb;
  const cards = T.lines.map((line) => h('section', { class: 'card' + (line.main ? ' main-line' : ''), 'aria-label': line.label },
    h('div', { class: 'card-head' }, h('h2', {}, line.label), line.main ? h('span', { class: 'badge b-ok' }, 'Hauptlinie') : null, h('span', { class: 'muted tiny' }, line.note)),
    line.loaders.map((entry) => {
      const row = h('div', { class: 'loader-row', tabindex: '0', dataset: { target: entry.id }, 'aria-label': `${entry.loader} ${line.label}, Kontextmenue mit Rechtsklick oder Umschalt+F10` });
      const live = h('span', { class: 'live', 'data-live': entry.id });
      const mk = (label, fn, primary, title) => h('button', { class: 'btn small' + (primary ? ' primary' : ''), type: 'button', title, onclick: fn }, label);
      row.append(
        h('span', { class: 'lname' }, entry.loader.charAt(0).toUpperCase() + entry.loader.slice(1), live),
        h('span', { class: 'actions' },
          mk('Client + frische Testwelt', () => doLaunch(entry.id, 'client_fresh'), true, 'Standard: entfernt den Fingerabdruck der Testwelt, das Testzentrum baut sich beim Betreten neu'),
          mk('Client', () => doLaunch(entry.id, 'client')), mk('Server', () => doLaunch(entry.id, 'server')),
          mk('Tests', () => doTests({ mode: 'targets', targets: [entry.testTarget] }), false, 'Server-Tests dieses Ziels'),
          mk('Client-Tests', () => doTests({ mode: 'targets', targets: [entry.clientTestTarget] }), false, entry.clientTestTarget ? 'Client-Suite (steuert Maus und Fokus)' : 'Fuer dieses Ziel gibt es keine Client-Suite'),
          h('button', { class: 'btn small', type: 'button', 'aria-haspopup': 'menu', 'aria-label': 'Weitere Aktionen', onclick: (e) => { const r = e.currentTarget.getBoundingClientRect(); loaderMenu(entry, e.currentTarget, r.left, r.bottom + 4); } }, '⋯')));
      if (!entry.clientTestTarget) $$('button', row)[4].disabled = true;
      row.addEventListener('contextmenu', (e) => { e.preventDefault(); const r = row.getBoundingClientRect(); loaderMenu(entry, row, e.clientX || r.left + 20, e.clientY || r.bottom - 4); });
      return row;
    })));
  fill(root, 
    h('div', { class: 'card-head' }, h('h1', {}, 'Starten'), h('span', { class: 'sp' }), h('span', { class: 'muted tiny' }, 'Arbeitsordner:'), wsSeg),
    h('p', { class: 'lead' }, 'Jede Zeile: Rechtsklick (oder ', h('kbd', {}, 'Umschalt+F10'), ') zeigt alle Aktionen inklusive Stoppen, Log und Laufordner. Mehrere Instanzen laufen parallel; zwei Server nicht (gleicher Port).'),
    low ? h('div', { class: 'box box-danger' }, h('div', { class: 'box-title' }, 'Zu wenig Platz'), `Nur ${S.disk.freeGb} GB frei. Unter ${S.disk.minFreeGb} GB startet der Hub nichts (Gradle-Ausgabe, Laufordner und Worktrees brauchen mehrere GB).`) : null,
    h('div', { class: 'box box-warn' }, h('div', { class: 'box-title' }, 'Regel 3'), 'Nicht im Haupt-Repo bauen, solange Besitzer-Clients laufen (NoClassDefFoundError im Spiel). Tests und Checks laufen deshalb standardmaessig im Gate-Worktree.'),
    modPicker, h('div', { class: 'grid2' }, cards), h('h3', {}, 'Instanzen und Jobs'), list);
  view = { onJobs: (all) => { list.sync(all); markLive(all); }, cleanup: () => {} };
  markLive(jobs);
}
async function launchModPicker() {
  const box = h('details', { class: 'card' });
  const summary = h('summary', {}, 'Mods für den Start');
  box.append(summary);
  try {
    const state = await api('/api/mods');
    const selection = structuredClone(state.selection);
    // Normal loader runs always contain SimpleBuilding itself.
    if (!selection.modules.includes('simplebuilding')) selection.modules.unshift('simplebuilding');
    const rows = state.rows.filter(row => row.kind === 'modules');
    const status = h('p', { class: 'muted tiny', role: 'status' });
    const controls = h('div', { class: 'actions' });
    const save = h('button', { class: 'btn primary', type: 'button', onclick: async () => {
      save.disabled = true;
      try {
        await api('/api/mods', { selection });
        status.textContent = 'Gespeichert. Gilt ab dem nächsten Start.';
        updateSummary();
      } catch (e) { status.textContent = e.message; }
      finally { save.disabled = false; }
    } }, 'Auswahl speichern');
    function updateSummary() { summary.textContent = `Mods für den Start · ${selection.modules.length}/${rows.length} ausgewählt`; }
    function draw() {
      controls.replaceChildren(...rows.map(row => h('label', { class: 'check' },
        h('input', { type: 'checkbox', checked: selection.modules.includes(row.id), disabled: row.id === 'simplebuilding',
          onchange: e => {
            selection.modules = e.target.checked ? [...selection.modules, row.id] : selection.modules.filter(id => id !== row.id);
            status.textContent = 'Auswahl geändert – bitte speichern.'; updateSummary();
          } }), row.displayName || row.name)));
      updateSummary();
    }
    draw();
    box.append(h('p', { class: 'muted' }, 'Projektmods für Minecraft 26.3. Standard: alle. SimpleBuilding ist beim normalen Start immer dabei. Ältere Minecraft-Linien verwenden ihre eigenen Mods.'),
      controls, h('div', { class: 'actions' }, h('button', { class: 'btn', type: 'button', onclick: () => {
        selection.modules = rows.map(row => row.id); draw(); status.textContent = 'Alle ausgewählt – bitte speichern.';
      } }, 'Alle auswählen'), save, h('a', { class: 'btn', href: '#/mods' }, 'Entwickler-Mods und Presets')), status);
  } catch (e) { box.append(h('p', { role: 'alert' }, 'Modauswahl konnte nicht geladen werden: ' + e.message)); }
  return box;
}
function markLive(all) {
  for (const el of $$('[data-live]')) {
    const id = el.dataset.live;
    el.replaceChildren(...all.filter((j) => ACTIVE.includes(j.status) && j.kind === 'launch' && j.meta.target === id).map((j) => h('span', { class: 'badge b-warn', title: j.label }, h('span', { class: 'dot running' }), j.meta.kind_of === 'server' ? 'Server' : 'Client')));
  }
}

// ---------------------------------------------------------------------------------------------
// Bereich: Tests
// ---------------------------------------------------------------------------------------------
const selected = new Map(); // "target|id" -> {target,id}
function selKey(r) { return r.target + '|' + r.id; }
function resultsHist(r) { return h('span', { class: 'hist', title: 'letzte Ergebnisse, neueste rechts' }, (r.passedOf || []).map((p) => h('i', { class: p ? '' : 'f' }))); }

function rowActions(r) {
  return h('span', { class: 'btn-row', style: 'gap:.25rem' },
    h('button', { class: 'btn small', type: 'button', title: 'Nur diesen Test erneut ausfuehren', onclick: () => doTests({ mode: 'tests', tests: [{ target: r.target, id: r.id }] }, 'Einzelner Test gestartet') }, 'Nochmal'),
    h('button', { class: 'btn small', type: 'button', title: 'Verlauf dieses Tests', onclick: () => showHistory(r) }, 'Verlauf'),
    r.status === 'failed' ? h('button', { class: 'btn small', type: 'button', title: 'Fehler als Markdown kopieren', onclick: async () => { try { copy(await api(`/api/failure.md?target=${encodeURIComponent(r.target)}&id=${encodeURIComponent(r.id)}`), 'Markdown kopiert'); } catch (e) { toast(e.message, 'bad'); } } }, 'Markdown') : null);
}

function testRow(r, bar) {
  const key = selKey(r);
  const cb = h('input', { type: 'checkbox', 'aria-label': 'Auswaehlen', checked: selected.has(key), onchange: () => { if (cb.checked) selected.set(key, { target: r.target, id: r.id }); else selected.delete(key); bar(); } });
  return h('tr', { class: r.status === 'failed' ? 'failed' : '' },
    h('td', {}, cb), h('td', {}, h('span', { class: 'dot ' + r.status, title: r.status })),
    h('td', {}, h('div', { class: 'tname' }, shortId(r.id)), r.status === 'failed' && r.message ? h('div', { class: 'msg' }, r.message) : null),
    h('td', { class: 'nowrap' }, r.target), h('td', {}, resultsHist(r)),
    h('td', {}, r.flaky ? h('span', { class: 'badge ' + (r.flaky === 'confirmed' ? 'b-bad' : 'b-warn'), title: r.flaky === 'confirmed' ? 'Derselbe saubere Commit war gruen und rot' : 'Mehrfacher Wechsel gruen/rot' }, r.flaky === 'confirmed' ? 'flaky' : 'wackelig') : null,
      r.firstFailedRun ? h('div', { class: 'tiny muted', title: r.firstFailedAt }, 'seit ', r.firstFailedRun.slice(0, 16)) : null),
    h('td', {}, rowActions(r)));
}

function selectionBar(host) {
  return () => {
    host.replaceChildren();
    if (!selected.size) { host.hidden = true; return; }
    host.hidden = false;
    const rows = Array.from(selected.values());
    host.append(h('span', { class: 'badge b-info' }, `${selected.size} ausgewaehlt`),
      h('button', { class: 'btn small primary', type: 'button', onclick: () => doTests({ mode: 'tests', tests: rows }, `${rows.length} Test(s) in Folgelaeufen`) }, 'Auswahl ausfuehren'),
      h('button', { class: 'btn small', type: 'button', onclick: () => openAiDialog(rows) }, 'Mit KI beheben'),
      h('button', { class: 'btn small', type: 'button', onclick: () => { selected.clear(); host.hidden = true; $$('main tbody input[type=checkbox]').forEach((c) => { c.checked = false; }); } }, 'Auswahl leeren'));
  };
}

async function viewTests(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' }), ' Laeufe werden gelesen ...'));
  let ov, pp;
  try { [ov, pp] = await Promise.all([api('/api/overview'), api('/api/prepush')]); } catch (e) { fill(root, errorBox(e)); return; }
  failCount = ov.failing; renderNav();
  const tf = prefs.tests;
  const presets = T.presets;
  let preset = 'main'; const custom = new Set();
  const presetSel = h('select', { 'aria-label': 'Testauswahl', onchange: () => { preset = presetSel.value; targetBox.hidden = preset !== 'custom'; } },
    Object.entries(presets).map(([id, p]) => h('option', { value: id }, p.label)), h('option', { value: 'custom' }, 'Eigene Auswahl ...'));
  const targetBox = h('div', { class: 'filters', hidden: true }, T.testTargets.map((t) => h('label', { class: 'chip' }, h('input', { type: 'checkbox', onchange: (e) => { if (e.target.checked) custom.add(t.id); else custom.delete(t.id); } }), t.id, t.snapshot ? ' (Snapshot)' : '')));
  const filterInput = h('input', { type: 'text', placeholder: 'ein Filtermuster, z. B. simplebuilding:hammer_*', 'aria-label': 'Filtermuster', style: 'min-width:20rem', oninput: () => { warn.hidden = !filterInput.value.includes(','); } });
  const warn = h('div', { class: 'box box-warn', hidden: true }, 'Ein Muster pro Lauf: ein Komma wuerde still nur das erste Muster benutzen. Mehrere Tests waehlst du unten in der Tabelle aus; der Hub reiht dann Folgelaeufe ein.');
  const clientEntries = h('input', { type: 'text', placeholder: 'Client-Testnamen (SIMPLEBUILDING_CLIENT_ONLY), optional', 'aria-label': 'Client-Testnamen', style: 'min-width:16rem' });
  const runSelected = async (useFilter) => {
    const ids = preset === 'custom' ? Array.from(custom) : presets[preset].targets;
    const body = { mode: 'targets', targets: ids };
    if (useFilter && filterInput.value.trim()) body.filter = filterInput.value.trim();
    if (ids.some((i) => i.startsWith('client-')) && clientEntries.value.trim()) body.clientEntries = clientEntries.value.split(',').map((s) => s.trim()).filter(Boolean);
    if (!ids.length) { toast('Keine Ziele ausgewaehlt', 'bad'); return; }
    await doTests(body);
  };
  const controls = h('section', { class: 'card', 'aria-label': 'Tests starten' },
    h('div', { class: 'card-head' }, h('h2', {}, 'Tests starten'), h('span', { class: 'muted tiny' }, `laeuft im ${testWs() === 'gate' ? 'Gate-Worktree' : 'Repo'} (Einstellungen)`)),
    h('div', { class: 'filters' }, presetSel,
      h('button', { class: 'btn primary', type: 'button', title: 'Taste a', onclick: () => runSelected(false) }, 'Alle Tests ausfuehren'),
      h('button', { class: 'btn', type: 'button', title: 'Taste r', onclick: runFailed }, `Nur fehlgeschlagene (${ov.failing})`),
      h('button', { class: 'btn', type: 'button', onclick: () => doTests({ mode: 'targets', targets: ['fabric-263', 'neoforge-263'] }, 'Alle Tests (26.3) gestartet') }, 'Schnell: 26.3')),
    targetBox, h('div', { class: 'filters' }, filterInput, h('button', { class: 'btn', type: 'button', onclick: () => { if (!filterInput.value.trim()) { toast('Erst ein Filtermuster eingeben', 'warn'); return; } runSelected(true); } }, 'Mit Filter ausfuehren'), clientEntries), warn);

  // Kacheln
  const tiles = h('div', { class: 'tiles' }, ov.targets.map((t) => {
    const tr = ov.trend.targets[t.id] || [];
    const red = t.failed > 0 || t.error;
    return h('div', { class: 'tile ' + (red ? 'red' : t.known ? 'green' : '') },
      h('div', { class: 'row' }, h('b', {}, t.label), h('span', { class: 'badge ' + (red ? 'b-bad' : 'b-ok') }, red ? 'rot' : 'gruen')),
      h('div', { class: 'row' }, h('span', { class: 'big' }, `${t.passed}`, h('span', { class: 'muted tiny' }, t.expected ? ` / ${t.expected}` : '')), tr.length > 1 ? spark(tr.map((p) => p.failed), { color: 'var(--danger)' }) : h('span', { class: 'muted tiny' }, 'kein Verlauf')),
      h('div', { class: 'lbl' }, `${t.failed} rot`, t.flaky ? ` · ${t.flaky} wackelig` : '', t.duration ? ` · Median ${fmtMs(t.duration.medianMs)}` : ''),
      h('div', { class: 'lbl' }, t.lastRun ? `${fmtAgo(t.lastRun.at)} · ${t.lastRun.commit || '?'}${t.lastRun.filter ? ' (Filter)' : ''}` : 'nie gelaufen'),
      t.error ? h('div', { class: 'msg' }, t.error.error) : null);
  }));

  const trendPts = ov.trend.runs;
  const trend = h('section', { class: 'card', 'aria-label': 'Verlauf' },
    h('div', { class: 'card-head' }, h('h2', {}, 'Trend'), h('span', { class: 'muted tiny' }, `letzte ${trendPts.length} volle Laeufe`)),
    trendPts.length > 1 ? h('div', { class: 'btn-row', style: 'gap:1.5rem' },
      h('div', {}, h('div', { class: 'tiny muted' }, 'bestanden'), spark(trendPts.map((p) => p.passed), { w: 260, ht: 44 })),
      h('div', {}, h('div', { class: 'tiny muted' }, 'rot'), spark(trendPts.map((p) => p.failed), { w: 260, ht: 44, color: 'var(--danger)' })),
      h('div', { class: 'tiny muted' }, `Gruene Laeufe: ${trendPts.filter((p) => p.verdict === 'green').length} von ${trendPts.length}`)) : h('div', { class: 'empty' }, 'Zu wenige volle Laeufe fuer einen Trend.'));

  const pre = h('section', { class: 'card', 'aria-label': 'Vor dem Push' },
    h('div', { class: 'card-head' }, h('h2', {}, 'Vor dem Push'), h('span', { class: 'badge ' + (pp.ready ? 'b-ok' : 'b-warn') }, pp.ready ? 'Bereit zum Push' : 'Noch nicht bereit'), h('span', { class: 'muted tiny' }, `HEAD ${pp.head.slice(0, 8)} · ${pp.git.branch}`), h('span', { class: 'sp' }),
      h('button', { class: 'btn small', type: 'button', onclick: () => post('/api/check/run', { workspace: testWs() }, 'Gradle check gestartet') }, 'Gradle check starten')),
    h('ul', { style: 'list-style:none;padding:0;margin:0' }, pp.items.map((i) => h('li', { style: 'display:flex;gap:.5rem;align-items:baseline;padding:.2rem 0' }, h('span', { class: 'dot ' + (i.ok ? 'green' : 'red') }), h('b', {}, i.label), h('span', { class: 'muted tiny' }, i.detail)))),
    h('p', { class: 'tiny muted' }, 'Das gruene Abzeichen erscheint erst, wenn Gradle check und die unfilterten 26.3-Server-Suiten fuer genau diesen Commit gruen sind. Beurteilt wird der Datensatz, nie der Exit-Code des Runners.'));

  // Tabelle
  const barHost = h('div', { class: 'filters', hidden: true, style: 'position:sticky;top:calc(var(--topbar-h) + 40px);z-index:20;background:var(--bg);padding:.4rem 0' });
  const bar = selectionBar(barHost);
  const tableHost = h('div', {});
  let offset = 0;
  const q = h('input', { type: 'search', id: 'search-current', placeholder: 'Test, Klasse oder Fehlertext suchen ( / )', 'aria-label': 'Tests durchsuchen', value: tf.q, oninput: () => { tf.q = q.value; savePrefs(); debounceLoad(); } });
  const tsel = h('select', { 'aria-label': 'Ziel', onchange: () => { tf.target = tsel.value; savePrefs(); load(true); } }, h('option', { value: '' }, 'Alle Ziele'), ov.targets.map((t) => h('option', { value: t.id, selected: tf.target === t.id }, t.label)));
  const chip = (label, key, title) => h('button', { class: 'chip', type: 'button', title, 'aria-pressed': String(!!tf[key]), onclick: (e) => { tf[key] = !tf[key]; e.currentTarget.setAttribute('aria-pressed', String(tf[key])); savePrefs(); load(true); } }, label);
  let timer = null; const debounceLoad = () => { clearTimeout(timer); timer = setTimeout(() => load(true), 250); };
  async function load(reset) {
    if (reset) { offset = 0; tableHost.replaceChildren(h('div', { class: 'loading' }, h('span', { class: 'spinner' }))); }
    let d;
    try { d = await api(`/api/tests?target=${encodeURIComponent(tf.target)}&q=${encodeURIComponent(tf.q)}&failed=${tf.failedOnly ? 1 : 0}&flaky=${tf.flakyOnly ? 1 : 0}&offset=${offset}&limit=300`); } catch (e) { tableHost.replaceChildren(errorBox(e)); return; }
    if (reset) tableHost.replaceChildren();
    if (!d.total) { tableHost.replaceChildren(h('div', { class: 'empty' }, h('b', {}, 'Keine Tests gefunden'), tf.failedOnly ? 'Kein Test ist aktuell rot. Schalte "nur rote" aus, um alle zu sehen.' : 'Passe Suche oder Ziel an.')); return; }
    let body = tableHost.querySelector('tbody');
    if (!body) {
      body = h('tbody', {});
      tableHost.append(h('div', { class: 'table-wrap' }, h('table', { class: 'table' }, h('thead', {}, h('tr', {}, h('th', {}), h('th', {}), h('th', {}, 'Test'), h('th', {}, 'Ziel'), h('th', {}, 'Verlauf'), h('th', {}, 'Hinweis'), h('th', {}, 'Aktionen'))), body)),
        h('div', { class: 'tiny muted', id: 'count', style: 'margin:.4rem 0' }));
    }
    let lastClass = null;
    for (const r of d.rows) {
      if (tf.group && r.class !== lastClass && !body.querySelector(`tr[data-class="${CSS.escape(r.class)}"]`)) { body.append(h('tr', { class: 'grouprow', dataset: { class: r.class } }, h('td', { colspan: 7 }, r.class))); }
      lastClass = r.class; body.append(testRow(r, bar));
    }
    offset += d.rows.length;
    $('#count', tableHost).textContent = `${offset} von ${d.total} Tests`;
    const more = $('#more', tableHost); if (more) more.remove();
    if (offset < d.total) tableHost.append(h('button', { id: 'more', class: 'btn', type: 'button', onclick: () => load(false) }, 'Mehr laden'));
  }
  const procs = processList((j) => ['test', 'check'].includes(j.kind), 'Starte oben einen Testlauf. Ergebnis und Log erscheinen hier.');
  fill(root, h('h1', {}, 'Tests'), h('p', { class: 'lead' }, `Stand der letzten ${ov.recordCount} Laeufe aus ${ov.runsDir}. Mutationslaeufe zaehlen nicht.`),
    ov.targetErrors.length ? h('div', { class: 'box box-danger' }, h('div', { class: 'box-title' }, 'Ziele mit Fehler im Datensatz'), ov.targetErrors.map((e) => h('div', {}, h('b', {}, e.target + ': '), e.error))) : null,
    controls, h('h3', {}, 'Laufende und letzte Jobs'), procs, h('h3', {}, 'Uebersicht'), tiles, h('div', { class: 'grid2' }, trend, pre),
    h('h3', {}, 'Alle Tests'), h('div', { class: 'filters' }, q, tsel, chip('nur rote', 'failedOnly'), chip('nur wackelige', 'flakyOnly', 'Tests mit wechselnden Ergebnissen'), chip('nach Klasse gruppieren', 'group')), barHost, tableHost);
  bar(); load(true);
  view = { onJobs: (all) => procs.sync(all), onFinished: () => viewTests(root) };
}

function errorBox(e) { return h('div', { class: 'box box-danger' }, h('div', { class: 'box-title' }, 'Fehler'), e.message || String(e)); }

// ---------------------------------------------------------------------------------------------
// Verlauf eines Tests, Lauf-Details, Vergleich
// ---------------------------------------------------------------------------------------------
async function showHistory(r) {
  let d;
  try { d = await api(`/api/test/history?target=${encodeURIComponent(r.target)}&id=${encodeURIComponent(r.id)}`); } catch (e) { toast(e.message, 'bad'); return; }
  const rows = d.history.slice().reverse();
  openModal(`Verlauf: ${shortId(r.id)}`, h('div', {},
    h('p', { class: 'muted' }, `${r.target}` + (d.flaky ? ` · ${d.flaky === 'confirmed' ? 'flaky (gleicher Commit, anderes Ergebnis)' : 'wackelig'}` : '') + (d.firstFailed ? ` · rot seit ${d.firstFailed.runId}` : '')),
    rows.length ? h('div', { class: 'table-wrap' }, h('table', { class: 'table' }, h('thead', {}, h('tr', {}, ['Lauf', 'Status', 'Commit', 'Dauer', 'Meldung'].map((c) => h('th', {}, c)))),
      h('tbody', {}, rows.map((x) => h('tr', { class: x.status === 'failed' ? 'failed' : '' }, h('td', {}, h('a', { href: '#', onclick: (e) => { e.preventDefault(); closeModal(); showRun(x.runId); } }, x.runId.slice(0, 19))), h('td', {}, h('span', { class: 'dot ' + x.status }), ' ', x.status), h('td', {}, x.short, x.dirty ? ' *' : ''), h('td', { class: 'num' }, x.timeMs ? x.timeMs + ' ms' : '-'), h('td', {}, x.message ? h('div', { class: 'tname' }, x.message.slice(0, 200)) : '')))))) : h('div', { class: 'empty' }, 'Keine Ergebnisse gefunden.')));
}

async function showRun(runId) {
  let d;
  try { d = await api(`/api/run/${encodeURIComponent(runId)}`); } catch (e) { toast(e.message, 'bad'); return; }
  const b = d.brief; const rec = d.record;
  const failing = rec.targets.filter((t) => t.selected).flatMap((t) => t.tests.filter((x) => x.status === 'failed').map((x) => ({ t: t.id, x })));
  openModal(`Lauf ${runId}`, h('div', {},
    h('div', { class: 'pmeta' }, h('span', { class: 'badge ' + (b.verdict === 'green' ? 'b-ok' : 'b-bad') }, b.verdict === 'green' ? 'alles gruen' : b.verdict), h('span', {}, b.startedAt), h('span', {}, `Commit ${b.commit || '?'}${b.dirty ? ' (schmutzig)' : ''} auf ${b.branch || '?'}`), h('span', {}, `Filter: ${b.filter || 'keiner'}`), h('span', {}, `Ausloeser: ${b.trigger}`), h('span', {}, fmtMs(b.durationMs))),
    h('div', { class: 'table-wrap' }, h('table', { class: 'table' }, h('thead', {}, h('tr', {}, ['Ziel', 'Tests', 'Gruen', 'Rot', 'Fehlend', 'Dauer'].map((c) => h('th', {}, c)))),
      h('tbody', {}, rec.targets.filter((t) => t.selected).map((t) => h('tr', {}, h('td', {}, t.id, t.error ? h('div', { class: 'msg' }, t.error) : null), h('td', { class: 'num' }, t.counts.total), h('td', { class: 'num' }, t.counts.passed), h('td', { class: 'num' }, t.counts.failed), h('td', { class: 'num' }, t.missingCount), h('td', { class: 'num' }, fmtMs(t.durationMs))))))),
    failing.length ? h('div', {}, h('h3', {}, 'Rote Tests'), failing.slice(0, 40).map(({ t, x }) => h('div', { style: 'margin-bottom:.4rem' }, h('span', { class: 'tname' }, `${t}  ${shortId(x.id)}`), x.message ? h('div', { class: 'msg' }, x.message) : null))) : null),
  [{ label: 'Zusammenfassung kopieren', action: async () => { copy(await api(`/api/summary.md?run=${encodeURIComponent(runId)}`), 'Markdown kopiert'); return true; } },
    { label: 'Herunterladen (.md)', action: async () => { download(`run-${runId}.md`, await api(`/api/summary.md?run=${encodeURIComponent(runId)}`)); return true; } }, { label: 'Schliessen', primary: true }]);
}

async function showCompare(a, b) {
  let d;
  try { d = await api(`/api/compare?a=${encodeURIComponent(a)}&b=${encodeURIComponent(b)}`); } catch (e) { toast(e.message, 'bad'); return; }
  const list = (title, items, cls) => h('div', {}, h('h3', {}, `${title} (${items.length})`), items.length ? items.slice(0, 80).map((i) => h('div', { style: 'margin-bottom:.3rem' }, h('span', { class: 'badge ' + cls }, i.target), ' ', h('span', { class: 'tname' }, shortId(i.id)), i.before ? h('span', { class: 'tiny muted' }, ` (vorher: ${i.before})`) : null, i.message ? h('div', { class: 'msg' }, i.message.slice(0, 300)) : null)) : h('div', { class: 'muted tiny' }, 'keine'));
  openModal('Laeufe vergleichen', h('div', {}, h('p', { class: 'muted' }, `${d.a.runId} (${d.a.commit}) → ${d.b.runId} (${d.b.commit})`), list('Neu rot', d.newlyFailing, 'b-bad'), list('Neu gruen', d.newlyFixed, 'b-ok'), list('Weiter rot', d.stillFailing, 'b-warn'),
    h('p', { class: 'tiny muted' }, `Nur im aelteren Lauf: ${d.onlyInA}, nur im neueren: ${d.onlyInB} Tests.`)));
}

// ---------------------------------------------------------------------------------------------
// Bereich: Fehlschlaege (+ KI-Dialog)
// ---------------------------------------------------------------------------------------------
async function viewFailures(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' }), ' Fehlschlaege werden gelesen ...'));
  let d;
  try { d = await api('/api/failures'); } catch (e) { fill(root, errorBox(e)); return; }
  failCount = d.rows.length; renderNav();
  const unchecked = new Set();
  const key = selKey;
  const chosen = () => d.rows.filter((r) => !unchecked.has(key(r)));
  const countEl = h('span', { class: 'badge b-info' });
  const upd = () => { countEl.textContent = `${chosen().length} von ${d.rows.length} ausgewaehlt`; };
  const groups = new Map(); for (const r of d.rows) { if (!groups.has(r.target)) groups.set(r.target, []); groups.get(r.target).push(r); }
  const acts = h('div', { class: 'filters' }, countEl,
    h('button', { class: 'btn small primary', type: 'button', disabled: !d.rows.length, onclick: () => doTests({ mode: 'tests', tests: chosen().map((r) => ({ target: r.target, id: r.id })) }, 'Ausgewaehlte Tests in Folgelaeufen') }, 'Ausgewaehlte erneut ausfuehren'),
    h('button', { class: 'btn small', type: 'button', disabled: !d.rows.length, onclick: () => openAiDialog(chosen()) }, 'Mit KI beheben ...'),
    h('button', { class: 'btn small', type: 'button', onclick: async () => { try { copy(await api('/api/summary.md'), 'Zusammenfassung kopiert'); } catch (e) { toast(e.message, 'bad'); } } }, 'Lauf als Markdown kopieren'),
    h('button', { class: 'btn small', type: 'button', onclick: () => { unchecked.clear(); $$('main input[data-fail]').forEach((c) => { c.checked = true; }); upd(); } }, 'Alle'),
    h('button', { class: 'btn small', type: 'button', onclick: () => { d.rows.forEach((r) => unchecked.add(key(r))); $$('main input[data-fail]').forEach((c) => { c.checked = false; }); upd(); } }, 'Keine'));
  const body = [];
  if (d.targetErrors.length) body.push(h('div', { class: 'box box-danger' }, h('div', { class: 'box-title' }, 'Ziele ohne Ergebnis (Build kaputt oder kein Bericht)'), d.targetErrors.map((e) => h('div', { style: 'margin-bottom:.3rem' }, h('b', {}, e.target), ` (${e.runId})`, h('div', { class: 'msg' }, e.error)))));
  if (!d.rows.length) body.push(h('div', { class: 'empty' }, h('b', {}, 'Nichts ist rot'), 'Im letzten bekannten Stand besteht jeder Test. Frueh gruen ist gut, "alles gruen" mit vollem Lauf ist besser: siehe "Vor dem Push".'));
  for (const [target, rows] of groups) {
    body.push(h('section', { class: 'card' }, h('div', { class: 'card-head' }, h('h2', {}, target), h('span', { class: 'badge b-bad' }, `${rows.length} rot`)),
      rows.map((r) => h('div', { style: 'display:flex;gap:.6rem;padding:.45rem 0;border-top:1px solid var(--border)' },
        h('input', { type: 'checkbox', checked: true, dataset: { fail: '1' }, 'aria-label': 'fuer KI-Fix/Wiederholung auswaehlen', onchange: (e) => { if (e.target.checked) unchecked.delete(key(r)); else unchecked.add(key(r)); upd(); } }),
        h('div', { style: 'min-width:0;flex:1' }, h('div', { class: 'tname' }, shortId(r.id)),
          h('div', { class: 'tiny muted' }, `${r.class} · rot seit ${r.firstFailedRun ? r.firstFailedRun.slice(0, 19) : r.runId.slice(0, 19)}`, r.flaky ? ' · wackelig' : ''),
          r.message ? h('div', { class: 'msg' }, r.message) : null, h('div', { style: 'margin-top:.3rem' }, rowActions(r)))))));
  }
  upd();
  fill(root, h('h1', {}, 'Fehlschlaege'), h('p', { class: 'lead' }, 'Aktuell rote Tests im letzten bekannten Stand jedes Ziels (neueste Aufzeichnung pro Test). Standardmaessig sind alle ausgewaehlt.'), acts, body);
  view = { onFinished: () => viewFailures(root) };
}

async function openAiDialog(rows) {
  if (!rows.length) { toast('Keine Tests ausgewaehlt', 'warn'); return; }
  let prov;
  try { prov = await api('/api/providers'); } catch (e) { toast(e.message, 'bad'); return; }
  const provSel = h('select', { 'aria-label': 'Anbieter' }, prov.providers.map((p) => h('option', { value: p.id, selected: p.id === prov.default }, `${p.label}${p.installed ? '' : ' (nicht installiert)'}`)));
  const extra = h('textarea', { rows: 3, placeholder: 'Zusaetzliche Hinweise an die KI (optional)', 'aria-label': 'Zusaetzliche Hinweise' });
  const out = h('div', {});
  const tests = rows.map((r) => ({ target: r.target, id: r.id }));
  let lastPrompt = '';
  async function preview() {
    out.replaceChildren(h('div', { class: 'loading' }, h('span', { class: 'spinner' })));
    try {
      const p = await api('/api/ai/preview', { tests, provider: provSel.value, extra: extra.value });
      lastPrompt = p.prompt;
      fill(out, 
        h('div', { class: 'pmeta' }, h('span', {}, `${p.tests} Test(s)`), h('span', {}, `${p.chars} Zeichen`), h('span', {}, 'Branch '), h('code', {}, p.branch), h('span', {}, 'Vorlage: ' + p.templateFile)),
        p.provider.installed ? null : h('div', { class: 'box box-warn' }, h('div', { class: 'box-title' }, `${p.provider.label} ist nicht installiert`), 'Installation: ', h('code', {}, p.provider.install), '. Der Prompt laesst sich trotzdem kopieren und in einem beliebigen Assistenten benutzen.'),
        p.unknownPlaceholders.length ? h('div', { class: 'box box-warn' }, 'Unbekannte Platzhalter in der Vorlage: ' + p.unknownPlaceholders.join(', ')) : null,
        h('textarea', { rows: 16, readonly: true, 'aria-label': 'Prompt', style: 'margin-top:.5rem' }, p.prompt));
      $('textarea[readonly]', out).value = p.prompt;
    } catch (e) { out.replaceChildren(errorBox(e)); }
  }
  openModal(`Mit KI beheben: ${rows.length} Test(s)`, h('div', {},
    h('p', { class: 'muted' }, 'Der Hub legt einen NEUEN Worktree an (nie das Haupt-Repo), startet dort den Assistenten ohne Oberflaeche und zeigt Log, Branch, Commit und Diff. Es wird nichts gemerged oder gepusht.'),
    h('div', { class: 'filters' }, provSel, h('button', { class: 'btn', type: 'button', onclick: preview }, 'Prompt-Vorschau'), h('button', { class: 'btn', type: 'button', onclick: async () => { if (!lastPrompt) await preview(); if (lastPrompt) copy(lastPrompt, 'Prompt kopiert'); } }, 'Prompt kopieren')),
    extra, out),
  [{ label: 'Abbrechen' }, { label: 'KI starten', primary: true, action: async () => {
    if (!lastPrompt) { toast('Erst die Prompt-Vorschau ansehen', 'warn'); return true; }
    try { await api('/api/ai/start', { tests, provider: provSel.value, extra: extra.value, confirm: true }); toast('KI-Job gestartet'); refreshJobs(); location.hash = '#/ai'; return false; }
    catch (e) { toast(e.message, 'bad'); return true; }
  } }]);
}

async function showAiFile(id, what) {
  try { const d = await api(`/api/ai/job/${id}/${what}`); openModal('Prompt', h('textarea', { rows: 22, readonly: true, 'aria-label': 'Prompt' }), [{ label: 'Kopieren', action: () => { copy(d.text); return true; } }, { label: 'Schliessen', primary: true }]); $('#modal-body textarea').value = d.text; }
  catch (e) { toast(e.message, 'bad'); }
}
async function showDiff(id) {
  let d;
  try { d = await api(`/api/ai/job/${id}/diff`); } catch (e) { toast(e.message, 'bad'); return; }
  const pre = h('pre', { class: 'diff', tabindex: '0', 'aria-label': 'Diff' });
  for (const line of d.diff.split('\n')) pre.append(h('span', { class: line.startsWith('+') && !line.startsWith('+++') ? 'add' : line.startsWith('-') && !line.startsWith('---') ? 'del' : line.startsWith('@@') ? 'hunk' : '' }, line + '\n'));
  openModal('Diff (Branch gegen Ausgangspunkt)', h('div', {}, h('pre', { class: 'diff', style: 'max-height:14rem' }, d.stat || '(keine Aenderungen)'), pre, d.truncated ? h('p', { class: 'tiny muted' }, 'Gekuerzt.') : null));
}

// ---------------------------------------------------------------------------------------------
// Bereich: Verlauf
// ---------------------------------------------------------------------------------------------
async function viewHistory(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' })));
  const hf = prefs.history; const picks = [];
  async function load() {
    let d;
    try { d = await api(`/api/runs?limit=80&mutations=${hf.mutations ? 1 : 0}`); } catch (e) { fill(root, errorBox(e)); return; }
    let runs = d.runs; if (hf.redOnly) runs = runs.filter((r) => r.verdict !== 'green');
    const cmp = h('button', { class: 'btn small', type: 'button', disabled: true, onclick: () => showCompare(picks[0], picks[1]) }, 'Zwei Laeufe vergleichen');
    const chip = (label, key) => h('button', { class: 'chip', type: 'button', 'aria-pressed': String(!!hf[key]), onclick: () => { hf[key] = !hf[key]; savePrefs(); load(); } }, label);
    fill(root, h('h1', {}, 'Verlauf'), h('p', { class: 'lead' }, `Aufzeichnungen aus ${d.runsDir}. Beurteilt wird jeder Lauf nach seinem Datensatz (nicht nach dem Exit-Code, der auch bei roten Tests 0 ist).`),
      h('div', { class: 'filters' }, chip('auch Mutationslaeufe', 'mutations'), chip('nur nicht gruene', 'redOnly'), cmp, h('button', { class: 'btn small', type: 'button', onclick: () => openPath({ what: 'runs' }) }, 'Ordner oeffnen')),
      runs.length ? h('div', { class: 'table-wrap' }, h('table', { class: 'table' }, h('thead', {}, h('tr', {}, ['', 'Lauf', 'Ergebnis', 'Ziele', 'Commit', 'Filter', 'Dauer', ''].map((c) => h('th', {}, c)))),
        h('tbody', {}, runs.map((r) => h('tr', { class: r.verdict === 'red' ? 'failed' : '' },
          h('td', {}, h('input', { type: 'checkbox', 'aria-label': 'zum Vergleich waehlen', onchange: (e) => { const i = picks.indexOf(r.runId); if (e.target.checked && i < 0) picks.push(r.runId); if (!e.target.checked && i >= 0) picks.splice(i, 1); if (picks.length > 2) { const old = picks.shift(); const c = $(`input[data-run="${old}"]`); if (c) c.checked = false; } cmp.disabled = picks.length !== 2; }, dataset: { run: r.runId } })),
          h('td', {}, h('a', { href: '#', onclick: (e) => { e.preventDefault(); showRun(r.runId); } }, r.runId.slice(0, 19)), h('div', { class: 'tiny muted' }, `${fmtAgo(r.startedAt)} · ${r.trigger}`)),
          h('td', {}, h('span', { class: 'badge ' + (r.verdict === 'green' ? 'b-ok' : r.verdict === 'red' ? 'b-bad' : '') }, r.verdict === 'green' ? 'alles gruen' : r.verdict), h('div', { class: 'tiny muted' }, `${r.totals.passed || 0}/${r.totals.total || 0}`, r.fullCoverage ? '' : ' (teilweise)')),
          h('td', { class: 'tiny' }, r.targets.map((t) => t.id.replace('client-', 'c-') + (t.failed ? `:${t.failed}` : '')).join(', ')),
          h('td', {}, r.commit || '?', r.dirty ? ' *' : ''), h('td', { class: 'tiny tname' }, r.filter || ''), h('td', { class: 'num' }, fmtMs(r.durationMs)),
          h('td', {}, h('button', { class: 'btn small', type: 'button', onclick: async () => { try { copy(await api(`/api/summary.md?run=${encodeURIComponent(r.runId)}`), 'Markdown kopiert'); } catch (e) { toast(e.message, 'bad'); } } }, 'Markdown')))))))
        : h('div', { class: 'empty' }, h('b', {}, 'Keine Laeufe'), 'In diesem Ordner liegen keine Aufzeichnungen. Starte Tests ueber "Tests".'));
  }
  await load(); view = { onFinished: load };
}

// ---------------------------------------------------------------------------------------------
// Bereich: KI-Fixes
// ---------------------------------------------------------------------------------------------
async function viewAi(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' })));
  let prov;
  try { prov = await api('/api/providers'); } catch (e) { fill(root, errorBox(e)); return; }
  const list = processList((j) => j.kind === 'ai', 'Waehle unter "Fehlschlaege" oder "Tests" rote Tests aus und starte "Mit KI beheben". Hier erscheinen Log, Branch, Commit und Diff.');
  fill(root, h('h1', {}, 'KI-Fixes'), h('p', { class: 'lead' }, 'Ein Assistent arbeitet ohne Oberflaeche in einem NEUEN Worktree. Nichts wird automatisch gemerged oder gepusht: du siehst geaenderte Dateien, Merge-Vorschau und Diff und entscheidest.'),
    h('div', { class: 'grid2' }, prov.providers.map((p) => h('div', { class: 'card' }, h('div', { class: 'card-head' }, h('h3', {}, p.label), h('span', { class: 'badge ' + (p.installed ? 'b-ok' : 'b-bad') }, p.installed ? 'installiert' : 'nicht installiert'), p.id === prov.default ? h('span', { class: 'badge b-info' }, 'Standard') : null),
      p.installed ? h('div', { class: 'tiny muted' }, p.path) : h('div', {}, h('div', { class: 'tiny muted' }, 'Installation:'), h('code', {}, p.install)),
      h('div', { class: 'tiny muted', style: 'margin-top:.4rem' }, 'Befehl: ', h('code', {}, p.template || '?'), ' (in den Einstellungen aenderbar)')))),
    h('div', { class: 'btn-row' }, h('button', { class: 'btn primary', type: 'button', onclick: async () => { try { const f = await api('/api/failures'); openAiDialog(f.rows); } catch (e) { toast(e.message, 'bad'); } } }, 'Alle aktuell roten Tests beheben ...')),
    h('h3', {}, 'Jobs'), list);
  view = { onJobs: (all) => list.sync(all) };
}

// ---------------------------------------------------------------------------------------------
// Bereich: Worktrees
// ---------------------------------------------------------------------------------------------
async function viewWorktrees(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' }), ' Worktrees werden geprueft (git, das dauert einen Moment) ...'));
  let d;
  try { d = await api('/api/worktrees'); } catch (e) { fill(root, errorBox(e)); return; }
  const gate = S && S.gateDir;
  const del = async (w) => {
    const ok = await confirmDialog('Worktree loeschen?', h('div', {}, h('p', {}, h('code', {}, w.path)), h('p', {}, `${fmtBytes(w.sizeBytes)} werden freigegeben. Der Branch ${w.branch || '(keiner)'} wird nur geloescht, wenn er gemerged ist.`)), 'Loeschen', true);
    if (!ok) return;
    try { const r = await api('/api/worktrees/delete', { path: w.path, confirm: true }); toast(r.message || `Geloescht. ${r.branch || ''}`); viewWorktrees(root); }
    catch (e) {
      if (e.data && e.data.needsSecondConfirm) {
        if (await confirmDialog('Ungemergte Arbeit!', `${e.message}. Beim Loeschen gehen diese Commits/Aenderungen verloren (sie stehen nur in diesem Worktree). Wirklich loeschen?`, 'Trotzdem loeschen', true)) {
          try { const r = await api('/api/worktrees/delete', { path: w.path, confirm: true, confirmUnmerged: true }); toast(`Geloescht. ${r.branch || ''}`); viewWorktrees(root); } catch (e2) { toast(e2.message, 'bad'); }
        }
      } else toast(e.message, 'bad');
    }
  };
  const total = d.worktrees.reduce((s, w) => s + (w.sizeBytes || 0), 0);
  fill(root, h('div', { class: 'card-head' }, h('h1', {}, 'Worktrees'), h('span', { class: 'sp' }), h('button', { class: 'btn small', type: 'button', onclick: () => viewWorktrees(root) }, 'Neu pruefen')),
    h('p', { class: 'lead' }, `Agenten-Worktrees unter ${d.root}. Verglichen wird mit ${d.base}. Geloescht wird nur nach Bestaetigung, Worktrees mit ungemergten Commits brauchen eine zweite.`),
    h('div', { class: 'card' }, h('div', { class: 'card-head' }, h('h3', {}, 'Gate-Worktree'), h('span', { class: 'badge ' + (S && S.gateExists ? 'b-ok' : '') }, S && S.gateExists ? 'vorhanden' : 'noch nicht angelegt')), h('div', { class: 'tiny muted' }, gate || ''),
      h('div', { class: 'btn-row', style: 'margin-top:.5rem' }, h('button', { class: 'btn small', type: 'button', onclick: () => post('/api/gate/prepare', {}, 'Gate-Worktree wird auf HEAD gesetzt') }, 'Auf HEAD setzen / anlegen'))),
    d.worktrees.length ? h('div', {}, h('p', { class: 'tiny muted' }, `${d.worktrees.length} Worktrees, zusammen ${fmtBytes(total)}${d.worktrees.some((w) => w.sizeBusy) ? ' (Groessen werden noch berechnet, "Neu pruefen" aktualisiert)' : ''}`),
      h('div', { class: 'table-wrap' }, h('table', { class: 'table' }, h('thead', {}, h('tr', {}, ['Worktree', 'Branch', 'Groesse', 'Status', 'Letzter Commit', ''].map((c) => h('th', {}, c)))),
        h('tbody', {}, d.worktrees.map((w) => h('tr', {}, h('td', {}, h('b', {}, w.name), w.isHub ? h('span', { class: 'badge b-info' }, ' dieser Hub') : null), h('td', { class: 'tname' }, w.branch || (w.detached ? '(detached)' : '')),
          h('td', { class: 'num' }, fmtBytes(w.sizeBytes) + (w.sizeComplete === false && w.sizeBytes !== null ? '+' : '')),
          h('td', {}, h('span', { class: 'badge ' + (w.merged ? 'b-ok' : 'b-warn') }, w.merged ? 'gemerged' : `${w.unmerged} ungemergt`), w.dirty ? h('span', { class: 'badge b-bad', style: 'margin-left:.3rem' }, 'ungesichert') : null, w.locked ? h('span', { class: 'badge' }, 'gesperrt') : null),
          h('td', { class: 'tiny' }, w.last), h('td', {}, h('span', { class: 'btn-row' }, h('button', { class: 'btn small', type: 'button', onclick: () => openPath({ what: 'worktree', path: w.path }) }, 'Oeffnen'), h('button', { class: 'btn small danger', type: 'button', disabled: w.isHub || w.locked, onclick: () => del(w) }, 'Loeschen')))))))))
      : h('div', { class: 'empty' }, h('b', {}, 'Keine Agenten-Worktrees'), 'Unter .claude/worktrees liegt nichts, was aufgeraeumt werden muesste.'));
  if (d.worktrees.some((w) => w.sizeBusy)) setTimeout(() => { if (location.hash === '#/worktrees') viewWorktrees(root); }, 12000);
  view = {};
}

// ---------------------------------------------------------------------------------------------
// Bereich: Einstellungen
// ---------------------------------------------------------------------------------------------
async function viewSettings(root) {
  fill(root, h('div', { class: 'loading' }, h('span', { class: 'spinner' })));
  let d;
  try { d = await api('/api/settings'); } catch (e) { fill(root, errorBox(e)); return; }
  const s = d.settings; const f = {};
  const num = (key, label, hint, min, max) => { f[key] = h('input', { type: 'number', min, max, value: s[key], 'aria-label': label }); return h('div', { class: 'field' }, h('label', {}, label), f[key], h('span', { class: 'hint' }, hint)); };
  const sel = (key, label, hint, opts) => { f[key] = h('select', { 'aria-label': label }, opts.map(([v, t]) => h('option', { value: v, selected: s[key] === v }, t))); return h('div', { class: 'field' }, h('label', {}, label), f[key], h('span', { class: 'hint' }, hint)); };
  const chk = (key, label, hint, disabled) => { f[key] = h('input', { type: 'checkbox', checked: s[key], disabled }); return h('div', { class: 'field' }, h('label', { class: 'check' }, f[key], label), h('span', { class: 'hint' }, hint)); };
  const provFields = {};
  const provCards = d.providers.map((p) => {
    const conf = s.providers[p.id];
    provFields[p.id] = { template: h('input', { type: 'text', value: conf.template, style: 'width:100%', 'aria-label': `Befehl ${p.label}` }), stdin: h('input', { type: 'checkbox', checked: conf.stdin }) };
    return h('div', { class: 'card' }, h('div', { class: 'card-head' }, h('h3', {}, p.label), h('span', { class: 'badge ' + (p.installed ? 'b-ok' : 'b-bad') }, p.installed ? 'installiert' : 'nicht installiert')),
      h('div', { class: 'field' }, h('label', {}, 'Befehlsvorlage'), provFields[p.id].template, h('span', { class: 'hint' }, 'Platzhalter: {prompt_file} {prompt} {worktree} {branch}. Jedes Wort wird ein eigenes Argument, es gibt keine Shell.')),
      h('label', { class: 'check' }, provFields[p.id].stdin, 'Prompt ueber stdin senden'), !p.installed ? h('div', { class: 'tiny muted', style: 'margin-top:.4rem' }, 'Installation: ', h('code', {}, p.install)) : null,
      h('div', { class: 'tiny muted', style: 'margin-top:.4rem' }, 'Die Flags folgen den oeffentlichen Docs, wurden aber auf dieser Maschine nicht geprueft: erst `--help` der CLI ansehen.'));
  });
  fill(root, h('h1', {}, 'Einstellungen'), h('p', { class: 'lead' }, `Gespeichert in ${d.file} (nicht im Git).`),
    h('div', { class: 'card' }, h('h2', {}, 'Starten und Tests'), h('div', { class: 'form-grid' },
      num('minFreeGb', 'Mindestens freier Platz (GB)', 'Darunter startet der Hub nichts.', 1, 500),
      sel('testWorkspace', 'Tests und Checks laufen in', 'Standard Gate-Worktree (AGENTS.md Regel 3).', [['gate', 'Gate-Worktree'], ['repo', 'Repo']]),
      sel('launchWorkspace', 'Clients und Server laufen in', 'Standard: dieses Repo wie bisher.', [['repo', 'Repo'], ['gate', 'Gate-Worktree']]),
      chk('quickPlay', 'Testwelt direkt betreten', 'Haengt --quickPlaySingleplayer SB-Testzentrale an, wenn die Welt existiert (nicht auf dieser Maschine geprueft).'),
      chk('dryRun', 'Trockenlauf', d.envDryRun ? 'Per Umgebungsvariable SB_HUB_DRY_RUN erzwungen.' : 'Nichts wird gestartet, Befehle werden nur ausgegeben.', d.envDryRun),
      num('historyDepth', 'Laeufe im Trend', 'Volle Laeufe fuer Sparklines.', 5, 200), num('stateDepth', 'Aufzeichnungen fuer den Teststand', 'So weit zurueck wird pro Test der letzte Stand gesucht.', 20, 600),
      chk('includeMutations', 'Mutationslaeufe einbeziehen', 'Sie sind absichtlich rot und stoeren Fehlerlisten und Trends.'))),
    h('h2', {}, 'KI-Anbieter'), sel('defaultProvider', 'Standardanbieter', '', [['claude', 'Claude Code'], ['codex', 'OpenAI Codex']]), h('div', { class: 'grid2' }, provCards),
    h('div', { class: 'card' }, h('h2', {}, 'Prompt-Vorlage'), h('p', { class: 'muted' }, 'Zum Aendern die Datei ', h('code', {}, d.templateFile), ' bearbeiten (Platzhalter in doppelten geschweiften Klammern). Die Vorschau im KI-Dialog zeigt das Ergebnis.'), h('textarea', { rows: 12, readonly: true, 'aria-label': 'Prompt-Vorlage' })),
    h('div', { class: 'btn-row' }, h('button', { class: 'btn primary', type: 'button', onclick: async () => {
      const patch = {
        minFreeGb: Number(f.minFreeGb.value), testWorkspace: f.testWorkspace.value, launchWorkspace: f.launchWorkspace.value, quickPlay: f.quickPlay.checked,
        historyDepth: Number(f.historyDepth.value), stateDepth: Number(f.stateDepth.value), includeMutations: f.includeMutations.checked, defaultProvider: f.defaultProvider.value,
        providers: Object.fromEntries(Object.entries(provFields).map(([id, x]) => [id, { template: x.template.value, stdin: x.stdin.checked }])),
      };
      if (!d.envDryRun) patch.dryRun = f.dryRun.checked;
      try { await api('/api/settings', { settings: patch }); toast('Gespeichert'); await refreshState(); viewSettings(root); } catch (e) { toast(e.message, 'bad'); }
    } }, 'Speichern')));
  $('textarea[readonly]', root).value = d.template;
  view = {};
}

// ---------------------------------------------------------------------------------------------
// Router, Tastatur, Start
// ---------------------------------------------------------------------------------------------
const VIEWS = { mods: viewMods, launch: viewLaunch, tests: viewTests, failures: viewFailures, history: viewHistory, ai: viewAi, worktrees: viewWorktrees, settings: viewSettings };
function routeName() { const n = (location.hash || '').replace(/^#\/?/, '').split('/')[0]; return VIEWS[n] ? n : (VIEWS[prefs.route] ? prefs.route : 'launch'); }
async function route() {
  if (view && view.cleanup) view.cleanup();
  view = null; closeMenu();
  const name = routeName(); prefs.route = name; savePrefs();
  document.body.classList.remove('nav-open'); $('#menu-btn').setAttribute('aria-expanded', 'false');
  renderNav();
  const root = $('#main');
  try { await VIEWS[name](root); } catch (e) { fill(root, errorBox(e)); }
}
addEventListener('hashchange', route);

function showHelp() {
  const rows = [['r', 'Fehlgeschlagene Tests erneut ausfuehren'], ['a', 'Alle Tests (26.3) ausfuehren'], ['/', 'Suchfeld des Bereichs fokussieren'], ['1 bis 7', 'Bereich wechseln'], ['?', 'Diese Hilfe'], ['Esc', 'Menue oder Dialog schliessen'], ['Umschalt+F10', 'Kontextmenue der fokussierten Zeile']];
  openModal('Tastenkuerzel', h('div', { class: 'kbd-list' }, rows.flatMap(([k, t]) => [h('kbd', {}, k), h('span', {}, t)])));
}
addEventListener('keydown', (e) => {
  if (e.key === 'Escape') { if (ctxEl) closeMenu(); else if (!$('#modal').hidden) closeModal(); return; }
  if (e.ctrlKey || e.metaKey || e.altKey) return;
  const tag = (e.target.tagName || '').toLowerCase();
  if (['input', 'textarea', 'select'].includes(tag) || e.target.isContentEditable || !$('#modal').hidden) return;
  if (e.key === '/') { const q = $('#search-current'); if (q) { e.preventDefault(); q.focus(); } }
  else if (e.key === '?') { e.preventDefault(); showHelp(); }
  else if (e.key === 'r') runFailed();
  else if (e.key === 'a') runAll();
  else if (/^[1-7]$/.test(e.key)) location.hash = '#/' + SECTIONS[Number(e.key) - 1][0];
});

$('#menu-btn').addEventListener('click', () => { const open = document.body.classList.toggle('nav-open'); $('#menu-btn').setAttribute('aria-expanded', String(open)); });
$('#backdrop').addEventListener('click', () => { document.body.classList.remove('nav-open'); });
$('#help-btn').addEventListener('click', showHelp);
$('#theme-btn').addEventListener('click', () => {
  const cur = document.documentElement.getAttribute('data-theme') || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  const next = cur === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  try { localStorage.setItem('simplebuilding-wiki-theme', next); } catch (e) { /* ohne Storage */ }
});

(async function start() {
  await refreshState();
  try { T = await api('/api/targets'); } catch (e) { $('#main').replaceChildren(errorBox(e)); return; }
  await refreshJobs();
  loadFailCount();
  await route();
  setInterval(() => { if (!document.hidden) { refreshState(); refreshJobs(); } }, 3000);
})();

async function viewMods(root) {
  const state = await api('/api/mods');
  const selection = structuredClone(state.selection);
  const buttons = h('div', {class:'actions'});
  const table = h('table', {class:'table'}, h('thead', {}, h('tr', {}, ['Aktiv', 'Mod', 'Version', 'JAR', 'Zweck'].map(t => h('th', {}, t)))),
    h('tbody', {}, state.rows.map(row => h('tr', {},
      h('td', {}, h('input', {type:'checkbox', class:'mod-switch', role:'switch', checked:row.enabled, 'aria-label':row.name,
        onchange:e => { const ids=selection[row.kind]; if(e.target.checked) ids.push(row.id); else ids.splice(ids.indexOf(row.id),1); }})),
      h('td', {}, row.name, h('small', {class:'muted'}, ' ? '+(row.kind==='modules'?'Repo':'Dev'))),
      h('td', {}, row.version), h('td', {}, row.jarPresent?'vorhanden':'noch nicht gebaut/geladen'),
      h('td', {}, row.purpose || (row.example?'Beispiel ohne Gameplay':'Repo-Mod'))))));
  const run = async (path, body) => { try { await api(path,body); toast('Gespeichert / gestartet'); } catch(e) { toast(e.message,'bad'); } };
  const preset = h('select', {'aria-label':'Mod-Preset'}, Object.keys(state.presets).map(id=>h('option',{value:id},id)));
  const name = h('input', {placeholder:'Eigenes Preset (a-z, 0-9, _)', 'aria-label':'Preset-Name'});
  buttons.append(h('button',{class:'btn primary',onclick:()=>run('/api/mods',{selection})},'Auswahl speichern'), preset,
    h('button',{class:'btn',onclick:async()=>{ await run('/api/mods',{selection:state.presets[preset.value]}); await viewMods(root); }},'Preset laden'),
    name, h('button',{class:'btn',onclick:async()=>{ await run('/api/mods',{selection,preset:name.value}); await viewMods(root); }},'Preset speichern'));
  const launches = h('div',{class:'actions'}, [['client','Integration: Client'],['server','Server'],['fresh','Frische Welt'],['tests','Integrationstests']].map(([action,label])=>
    h('button',{class:'btn',onclick:async()=>{ try { await api('/api/mods',{selection}); const result=await api('/api/integration/launch',{action,workspace:launchWs()}); toast(result.warnings.join(' ') || 'Gestartet'); } catch(e){toast(e.message,'bad');} }},label)));
  fill(root,h('h1',{},'Mods'),h('p',{class:'muted'},'Minecraft 26.3: Normale Hub-Starts laden die ausgewählten Projektmods; SimpleBuilding ist immer dabei. Entwickler-Mods werden zusätzlich nach Auswahl geladen. Die Integration verwendet separate Saves. Cloth Config ist für SimpleBuilding auf Fabric und NeoForge erforderlich.'),
    h('section',{class:'card'},buttons,h('div',{class:'table-wrap'},table),launches),processList(j=>j.meta && j.meta.target==='integration-263','Noch keine Integrationsl?ufe.'));
}
