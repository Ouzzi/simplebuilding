'use strict';
// Zentrale der Zentralen - Oberflaeche. Gleiche Bausteine wie die Launch- und Testzentrale (app.css).
const STATE = {
  own: ['läuft', 'own', 'von hier gestartet'],
  external: ['läuft', 'external', 'extern gestartet'],
  starting: ['startet', 'starting', 'startet ...'],
  stopped: ['gestoppt', 'stopped', 'gestoppt'],
};
let centrals = [];
let busy = false;
const $ = id => document.getElementById(id);
const running = c => c.state === 'own' || c.state === 'external';

async function api(path, body) {
  const opts = body === undefined ? {} : {
    method: 'POST', headers: { 'X-Zentrale-Client': '1', 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
  const r = await fetch(path, opts);
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(data.error || ('HTTP ' + r.status));
  return data;
}

function el(tag, attrs, ...children) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (k === 'class') e.className = v; else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else if (v === true) e.setAttribute(k, ''); else if (v !== false && v != null) e.setAttribute(k, v);
  }
  for (const c of children.flat()) if (c != null) e.append(c);
  return e;
}

function toast(text, bad) {
  const t = el('div', { class: 'toast' + (bad ? ' bad' : '') }, text);
  $('toasts').append(t);
  setTimeout(() => t.remove(), bad ? 7000 : 3500);
}

function confirmDialog(title, text, okLabel) {
  return new Promise(resolve => {
    const back = $('modal'), body = $('modal-body');
    const close = v => { back.hidden = true; document.removeEventListener('keydown', key); resolve(v); };
    const key = e => { if (e.key === 'Escape') close(false); };
    body.replaceChildren(
      el('h2', { id: 'modal-title' }, title),
      el('p', {}, text),
      el('div', { class: 'btn-row foot', style: 'display:flex;gap:.5rem;justify-content:flex-end' },
        el('button', { class: 'btn', onclick: () => close(false) }, 'Abbrechen'),
        el('button', { class: 'btn danger', id: 'modal-ok', onclick: () => close(true) }, okLabel)));
    back.hidden = false;
    document.addEventListener('keydown', key);
    $('modal-ok').focus();
  });
}

function renderStrip() {
  const up = centrals.filter(running).length;
  const pills = [el('span', { class: 'pill ' + (up === centrals.length ? 'ok' : up ? 'warn' : '') },
    el('b', {}, `${up}/${centrals.length}`), ' laufen')];
  for (const c of centrals) {
    pills.push(el('span', { class: 'pill' + (running(c) ? ' ok' : '') }, el('span', { class: 'dot ' + c.state }), c.name));
  }
  pills.push(el('span', { class: 'pill' }, 'Produktivmodus'));
  $('strip').replaceChildren(...pills);
}

function card(c) {
  const [label, cls, long] = STATE[c.state];
  const act = (text, cls2, fn, disabled, title) => el('button', { class: 'btn small ' + cls2, onclick: fn, disabled, title }, text);
  return el('section', { class: 'card z-card', 'aria-label': c.name },
    el('div', { class: 'z-head' }, el('span', { class: 'dot ' + c.state }), el('span', { class: 'title' }, c.name),
      el('span', { class: 'badge ' + cls, title: long }, label)),
    el('div', { class: 'z-meta' }, el('span', {}, 'Port ', el('code', {}, String(c.port))),
      el('span', {}, long), c.pid ? el('span', {}, 'PID ', el('code', {}, String(c.pid))) : null),
    el('div', { class: 'z-actions' },
      act('Starten', 'primary', () => run('/api/start', [c.id]), running(c) || c.state === 'starting'),
      act('Neu starten', '', () => restart([c.id]), !running(c)),
      act('Stoppen', 'danger', () => stop([c.id]), !running(c)),
      act('Öffnen ↗', '', () => window.open(c.url, '_blank', 'noopener'), !running(c), c.url)));
}

function render() {
  renderStrip();
  $('list').replaceChildren(...centrals.map(card));
  $('stop-all').disabled = !centrals.some(running);
  $('open-all').disabled = !centrals.some(running);
  $('restart-all').disabled = !centrals.some(running);
  $('start-all').disabled = centrals.every(c => running(c) || c.state === 'starting');
}

async function refresh() {
  if (busy) return;
  try { centrals = (await api('/api/centrals')).centrals; render(); }
  catch (e) { $('strip').replaceChildren(el('span', { class: 'pill bad' }, 'Zentrale nicht erreichbar: ' + e.message)); }
}

async function run(path, ids, external) {
  busy = true; document.body.classList.add('busy');
  try {
    const d = await api(path, { ids, external: !!external });
    centrals = d.centrals; render();
    const parts = [];
    if ((d.stopped || []).length) parts.push(d.stopped.length + ' gestoppt');
    if ((d.started || []).length) parts.push(d.started.length + ' gestartet');
    toast(parts.join(', ') || 'Nichts zu tun');
  } catch (e) { toast(e.message, true); }
  finally { busy = false; document.body.classList.remove('busy'); refresh(); }
}

function externals(ids) {
  return centrals.filter(c => (ids === 'all' || ids.includes(c.id)) && c.state === 'external');
}

async function stop(ids) {
  const ext = externals(ids);
  if (ext.length && !await confirmDialog('Extern gestartete Zentralen stoppen?',
    `${ext.map(c => c.name).join(', ')} wurde nicht hier gestartet. Ungespeicherte Eingaben dort gehen verloren; ein vom Hub gestarteter Minecraft-Client läuft weiter.`,
    'Stoppen')) return;
  run('/api/stop', ids, true);
}

async function restart(ids) {
  const ext = externals(ids);
  if (ext.length && !await confirmDialog('Extern gestartete Zentralen neu starten?',
    `${ext.map(c => c.name).join(', ')} wird beendet und hier neu gestartet, im Produktivmodus.`, 'Neu starten')) return;
  run('/api/restart', ids, true);
}

$('start-all').onclick = () => run('/api/start', 'all');
$('stop-all').onclick = () => stop('all');
$('restart-all').onclick = () => restart('all');
$('open-all').onclick = () => centrals.filter(running).forEach(c => window.open(c.url, '_blank', 'noopener'));
$('theme-btn').onclick = () => {
  const cur = document.documentElement.getAttribute('data-theme')
    || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  const next = cur === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  try { localStorage.setItem('simplebuilding-wiki-theme', next); } catch (e) { /* ohne Storage */ }
};
refresh();
setInterval(refresh, 2000);
