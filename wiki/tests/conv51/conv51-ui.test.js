// node --test wiki/tests/conv51/   (N51: smoke test of conv-ui.js with a DOM stand-in; no real browser in the container)
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('fs'), path = require('path'), vm = require('vm');
const A = path.join(__dirname, '../../assets/conv'), EX = path.join(__dirname, 'examples'), W = path.join(__dirname, '../..');

/* element stand-in: querySelector hands out one stub per selector (the UI only reads/writes properties and handlers) */
function el(tag) {
  const kids = new Map();
  const e = {
    tag, innerHTML: '', textContent: '', value: '', hidden: false, checked: false, classList: { add() {}, remove() {} }, style: {},
    getAttribute: (k) => e.attrs[k], attrs: {}, files: [], nextElementSibling: null,
    querySelector(sel) { if (!kids.has(sel)) kids.set(sel, el(sel)); return kids.get(sel); },
    querySelectorAll() { return []; }, select() {}, click() {}, remove() {}, appendChild() {},
    getContext: () => new Proxy({}, { get: (t, k) => (k in t ? t[k] : () => {}), set: (t, k, v) => { t[k] = v; return true; } })
  };
  return e;
}
function page(mode, url, extra) {
  const u = new URL(url), store = {}, root = el('div');
  root.attrs['data-conv'] = mode;
  const ctx = {
    console, setTimeout, clearTimeout, Promise, URL, URLSearchParams, TextDecoder, TextEncoder, Blob, Response, DecompressionStream, CompressionStream, atob, btoa,
    location: { search: u.search, pathname: u.pathname, protocol: u.protocol }, navigator: { language: 'de-DE' },
    sessionStorage: { getItem: (k) => store[k] ?? null, setItem: (k, v) => { store[k] = String(v); } },
    localStorage: { getItem: () => null, setItem() {} },
    document: { readyState: 'complete', querySelectorAll: (s) => (s === '[data-conv]' ? [root] : []), createElement: el, body: el('body') }
  };
  Object.assign(ctx, extra || {});
  ctx.window = ctx; ctx.globalThis = ctx;
  vm.createContext(ctx);
  for (const f of ['conv-data.js', 'conv-core.js', 'conv-formats.js', 'conv-lang.js', 'conv-mesh.js', 'conv-ui.js']) vm.runInContext(fs.readFileSync(path.join(A, f), 'utf8'), ctx, { filename: f });
  return { root, ctx, store };
}
const tick = () => new Promise((r) => setTimeout(r, 50));

test('dev gate: public URL shows only "not public", /dev/, ?dev=1 (kept in the session), sbDevHook and file:// open the tool', () => {
  assert.match(page('blueprint', 'https://wiki.example/bauwerke.html').root.innerHTML, /noch nicht öffentlich/);
  assert.match(page('blueprint', 'https://wiki.example/dev/bauwerke.html').root.innerHTML, /cv-drop/);
  const q = page('model', 'https://wiki.example/modelle.html?dev=1');
  assert.match(q.root.innerHTML, /cv-drop/); assert.equal(q.store['simple-wiki-dev'], '1');
  assert.match(page('model', 'https://wiki.example/modelle.html', { sbDevHook: {} }).root.innerHTML, /cv-drop/);
  assert.match(page('model', 'file:///x/modelle.html').root.innerHTML, /cv-drop/);
  assert.equal(page('model', 'https://wiki.example/x').ctx.Conv51UI.PUBLIC, false);
});

test('pages keep noindex, load only local scripts; index.html links them only in dev mode', () => {
  for (const f of ['bauwerke.html', 'modelle.html']) {
    const h = fs.readFileSync(path.join(W, f), 'utf8');
    assert.match(h, /<meta name="robots" content="noindex,nofollow">/);
    assert.ok(!/(src|href)="(https?:)?\/\//.test(h), f + ' has no foreign host');
  }
  const idx = fs.readFileSync(path.join(W, 'index.html'), 'utf8');
  assert.match(idx, /var CONV51_PUBLIC = false;/);
  assert.match(idx, /if \(!c \|\| !\(CONV51_PUBLIC \|\| isDevSite\(\)\)\) return '';/);
  for (const f of fs.readdirSync(A)) assert.ok(!/https?:\/\/(?!www\.w3\.org)/.test(fs.readFileSync(path.join(A, f), 'utf8')), f + ' contacts no host');
});

test('every UI text exists in English and German', () => {
  const L = page('model', 'file:///m.html').ctx.Conv51UI.strings;
  assert.deepEqual(Object.keys(L.de).sort(), Object.keys(L.en).sort());
});

test('blueprint page: example file -> notes and code; way back checks a code', async () => {
  const s = { blueprint: { name: 'hut.litematic', b64: fs.readFileSync(path.join(EX, 'hut.litematic')).toString('base64') } };
  const { root } = page('blueprint', 'https://wiki.example/dev/bauwerke.html', { CONV51_SAMPLES: s });
  root.querySelector('.cv-unk').value = 'keep';
  root.querySelector('.cv-sample').onclick();
  await tick();
  const out = root.querySelector('.cv-out').innerHTML;
  assert.match(out, /litematic/); assert.match(out, /Blaupausen-Code/); assert.match(out, /# 9x8x9, \d+ blocks/); assert.match(out, /Nötiger Baustab/);
  assert.equal(root.querySelector('.cv-regl').hidden, false);
  root.querySelector('.cv-back-code').value = 'stone 0..2,0,0\n$r = 4\nglass sphere(8,8,8,$r)';
  root.querySelector('.cv-back-check').onclick();
  assert.match(root.querySelector('.cv-back-out').innerHTML, /Code ist in Ordnung/);
  root.querySelector('.cv-back-code').value = 'stone 0,0';
  root.querySelector('.cv-back-check').onclick();
  assert.match(root.querySelector('.cv-back-out').innerHTML, /Zeile 1: ungültige Position/);
});

test('model page: .glb example -> approximation note and model code; errors are readable', async () => {
  const s = { model: { name: 'house.glb', b64: fs.readFileSync(path.join(EX, 'house.glb')).toString('base64') } };
  const { root, ctx } = page('model', 'https://wiki.example/dev/modelle.html', { CONV51_SAMPLES: s });
  root.querySelector('.cv-sample').onclick();
  await tick();
  const out = root.querySelector('.cv-out').innerHTML;
  assert.match(out, /Näherung/); assert.match(out, /&quot;elements&quot;/); assert.match(out, /Ressourcenpaket/);
  assert.match(ctx.Conv51UI.errText(new Error('axiom_bp')), /Axiom/);
  assert.match(ctx.Conv51UI.errText(new Error('gltf_missing_bin:a.bin')), /a\.bin/);
});
