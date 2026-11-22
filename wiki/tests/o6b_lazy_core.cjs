// O6b core test (run by test_wiki_index.py, or directly: node wiki/tests/o6b_lazy_core.cjs).
// assets/module-loader.js in a sandbox per address: which data/<mod>.js it loads (root start page: none, a mod area: the
// mod with its sub-mods, every other address: all; the one-shot sessionStorage flag; no storage = never light), loads on
// demand once, prunes lazily loaded mods in the public view like before (O5b), prepares data/index.js for the public view.
// Then data/index.js (wiki/wiki_index.py) against the real data: every item/block id there, and its counts equal what
// index.html computes from the loaded data after the real pruneHidden - in the dev and in the public view.
// Prints one JSON line {checks, failures}.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const WIKI = path.resolve(__dirname, '..');
const loader = fs.readFileSync(path.join(WIKI, 'assets/module-loader.js'), 'utf8');
let checks = 0;
const failures = [];
function check(cond, msg) { checks++; if (!cond) failures.push(msg); }
const load = (file, name) => new Function('window', fs.readFileSync(path.join(WIKI, file), 'utf8') + '; return window.' + name + ';')({});
const catalogue = load('data/modules.js', 'WIKI_MODULES');
const index = load('data/index.js', 'WIKI_INDEX');
const unreleased = catalogue.filter(m => m.release === false).map(m => m.id);
const TEXT = {};
const json = id => JSON.parse(TEXT[id] || (TEXT[id] = fs.readFileSync(path.join(WIKI, 'data', id + '.json'), 'utf8')));
const family = id => [id].concat(...catalogue.filter(c => c.parent === id && c.id !== id).map(c => family(c.id)));
const isObj = x => !!x && typeof x === 'object' && !Array.isArray(x);

function storage(init) {
  const m = Object.assign({}, init);
  return { getItem: k => (k in m ? m[k] : null), setItem: (k, v) => { m[k] = String(v); }, removeItem: k => { delete m[k]; }, m };
}
async function run(href, opts) {
  opts = opts || {};
  const u = new URL(href), scripts = [];
  const win = {
    WIKI_MODULES: JSON.parse(JSON.stringify(catalogue)), WIKI_LOCAL_ONLY: {},
    location: { protocol: u.protocol, hostname: u.hostname, pathname: u.pathname, search: u.search, hash: u.hash },
    fetch: () => Promise.resolve({ ok: false })
  };
  if (opts.storage) win.sessionStorage = opts.storage;
  const real = new Set(opts.real || []);
  const doc = {
    querySelector: sel => (sel === 'base[data-modpath]' && opts.base ? { getAttribute: () => opts.base } : null),
    createElement: () => { const s = {}; scripts.push(s); return s; },
    head: { appendChild: s => setImmediate(() => {
      const m = /^data\/([a-z0-9_-]+)\.js/.exec(s.src);
      if (m && m[1] === 'index') win.WIKI_INDEX = JSON.parse(JSON.stringify(index));
      else if (m && m[1] === 'simplebuilding') win.WIKI_DATA = real.has('simplebuilding') ? json('simplebuilding') : {};
      else if (m) { win.WIKI_MODULE_DATA = win.WIKI_MODULE_DATA || {}; win.WIKI_MODULE_DATA[m[1]] = real.has(m[1]) ? json(m[1]) : {}; }
      if (s.onload) s.onload();
    }) }
  };
  const ctx = { window: win, document: doc, Promise, setTimeout, Object, Array, URLSearchParams };
  vm.createContext(ctx);
  vm.runInContext(loader, ctx);
  await win.WIKI_READY;
  const mods = () => scripts.map(s => s.src.replace(/\?.*/, '')).filter(s => s !== 'data/index.js').map(s => s.slice(5, -3)).sort();
  return { win, mods, scripts };
}
const same = (a, b) => JSON.stringify(a.slice().sort()) === JSON.stringify(b.slice().sort());
const all = catalogue.map(m => m.id);

(async () => {
  const DEV = 'https://wiki.example/dev/';
  // root start page: index only
  let r = await run(DEV, { base: '/dev/', storage: storage() });
  check(r.win.WIKI_LIGHT === true && r.mods().length === 0, 'root start page loads no mod data: ' + r.mods());
  check(r.scripts.some(s => s.src === 'data/index.js') && !!r.win.WIKI_INDEX, 'root start page loads the index');
  for (const q of ['?lang=de', '?lang=en&pack=all', '#/', 'index.html']) {
    r = await run(DEV + q, { base: '/dev/', storage: storage() });
    check(r.win.WIKI_LIGHT === true && r.mods().length === 0, 'light for ' + q);
  }
  // mod areas: the mod with its sub-mods only
  for (const [href, mod] of [[DEV + 'simpleend/items/ashwood_boat', 'simpleend'], [DEV + 'simpleend/?lang=de', 'simpleend'], [DEV + 'simplebuilding/', 'simplebuilding'],
    [DEV + '?mod=simplestorage&item=simplestorage:x', 'simplestorage'], [DEV + 'simplequalityoflife/guidebook', 'simplequalityoflife']]) {
    r = await run(href, { base: '/dev/', storage: storage() });
    check(r.win.WIKI_LIGHT === false && same(r.mods(), family(mod)), 'mod area ' + href + ' loads ' + family(mod).length + ' mods, got ' + r.mods().length);
  }
  check(family('simplebuilding').length > 20, 'simplebuilding family is the super-mod with its sub-mods');
  // other addresses: everything, as before
  for (const q of ['?q=purpur', '#/items', '?tab=items', '?mod=simpleend&tab=allmods', '?item=simpleend:ashwood_boat', '#/allmods', '?dev=1']) {
    r = await run(DEV + q, { base: '/dev/', storage: storage() });
    check(r.win.WIKI_LIGHT === false && same(r.mods(), all.filter(id => id !== 'simplebuilding').concat(['simplebuilding'])), 'all mods for ' + q + ': ' + r.mods().length);
  }
  // the page's reload flag: all mods once, flag gone; without storage the start page is never light
  const st = storage({ 'wiki-load-all': '1' });
  r = await run(DEV, { base: '/dev/', storage: st });
  check(!r.win.WIKI_LIGHT && r.mods().length === all.length && !('wiki-load-all' in st.m), 'flag: all mods once, flag cleared');
  r = await run(DEV, { base: '/dev/', storage: st });
  check(r.win.WIKI_LIGHT === true, 'next load after the flag is light again');
  r = await run(DEV, { base: '/dev/' });
  check(!r.win.WIKI_LIGHT && r.mods().length === all.length, 'no sessionStorage: root loads everything');
  // without <base data-modpath> (file://, old servers): query form only
  r = await run('http://localhost:8000/?mod=simpleend', { storage: storage() });
  check(same(r.mods(), family('simpleend')), 'query form ?mod= without base');
  r = await run('http://localhost:8000/', { storage: storage() });
  check(r.win.WIKI_LIGHT === true, 'local root start page is light');
  // on demand: each script once
  r = await run(DEV + 'simpleend/', { base: '/dev/', storage: storage() });
  const before = r.scripts.length;
  await r.win.WIKI_LOAD_MODS(['simplestorage', 'simpleend']); await r.win.WIKI_LOAD_MODS(['simplestorage']);
  check(r.scripts.length === before + 1 && !!r.win.WIKI_MODULE_DATA.simplestorage, 'WIKI_LOAD_MODS loads a mod once');
  check(r.win.WIKI_HAS_ALL() === false, 'not all loaded yet');
  await r.win.WIKI_LOAD_ALL();
  check(r.win.WIKI_HAS_ALL() === true && r.scripts.length === before + all.length - family('simpleend').length, 'WIKI_LOAD_ALL loads the rest once: ' + r.scripts.length);

  // public view (production host, base "/"): hidden mods never loaded, a lazily loaded mod is pruned like before
  const PROD = 'https://wiki.example/';
  r = await run(PROD + 'simpleslabs/', { base: '/', storage: storage(), real: ['simpleslabs'] });
  check(!JSON.stringify(r.win.WIKI_MODULE_DATA.simpleslabs.recipes).includes('simplechess'), 'prod mod area: slabs pruned');
  r = await run(PROD, { base: '/', storage: storage(), real: ['simpleslabs'] });
  check(r.win.WIKI_LIGHT && !r.win.WIKI_MODULE_DATA.simpleslabs, 'prod start page light');
  await r.win.WIKI_LOAD_MODS(['simpleslabs', 'simplechess']);
  const lazySlabs = r.win.WIKI_MODULE_DATA.simpleslabs;
  check(!JSON.stringify(lazySlabs.recipes).includes('simplechess') && !lazySlabs.blocks.some(b => (b.needsMods || []).includes('simplechess')), 'prod: lazily loaded slabs pruned');
  check(!r.win.WIKI_MODULE_DATA.simplechess, 'prod: a hidden mod is never loaded');
  await r.win.WIKI_LOAD_ALL();
  check(unreleased.every(id => !r.win.WIKI_MODULE_DATA[id]), 'prod WIKI_LOAD_ALL leaves the hidden mods out');
  const pix = r.win.WIKI_INDEX.mods;
  check(unreleased.every(id => !pix[id]), 'prod index without hidden mods');
  check(!pix.simpleslabs.blocks.some(e => (e[5] || []).includes('simplechess')) && index.mods.simpleslabs.blocks.some(e => (e[5] || []).includes('simplechess')), 'prod index without the chess half slabs');
  check(JSON.stringify(pix.simpleslabs.counts) === JSON.stringify(index.mods.simpleslabs.countsPublic), 'prod index uses the public counts');
  r = await run(DEV + '?release=1', { base: '/dev/', storage: storage() });
  check(r.win.WIKI_LIGHT && unreleased.every(id => !r.win.WIKI_INDEX.mods[id]), '?release=1 on dev: public index');
  r = await run(DEV, { base: '/dev/', storage: storage() });
  check(unreleased.every(id => !!r.win.WIKI_INDEX.mods[id]) && JSON.stringify(r.win.WIKI_INDEX.mods.simpleslabs.counts) === JSON.stringify(index.mods.simpleslabs.counts), 'dev index keeps everything');

  // the index against the real data, both views (counts as index.html famCounts / NAV_GROUPS compute them)
  const prune = r.win.WIKI_PRUNE_HIDDEN;
  function counts(d) {
    const items = (d.items || []).filter(e => isObj(e) && e.id), ids = new Set(items.map(e => e.id));
    const iw = isObj(d.inWorld) ? d.inWorld : {}, ln = k => (Array.isArray(d[k]) ? d[k].length : 0);
    return { entries: items.length + (d.blocks || []).filter(e => isObj(e) && e.id && !ids.has(e.id)).length, recipes: ln('recipes'), features: ln('features'), config: ln('config'),
      inworld: (iw.entries || []).filter(e => isObj(e) && e.kind).length, enchantments: ln('enchantments'), trades: ln('trades'), lootTables: ln('lootTables'),
      advancements: ln('advancements'), tags: ln('tags') };
  }
  const merged = { dev: { items: [], blocks: [] }, pub: { items: [], blocks: [] } };
  let sums = { dev: 0, pub: 0 };
  for (const m of catalogue) {
    const ix = index.mods[m.id];
    check(!!ix, 'index has ' + m.id);
    if (!ix) continue;
    const d = json(m.id);
    const have = new Set(ix.items.map(e => e[0]).concat(ix.blocks.map(e => e[0])));
    const missing = (d.items || []).concat(d.blocks || []).filter(e => isObj(e) && e.id && !have.has(e.id)).map(e => e.id);
    check(!missing.length, m.id + ': index lacks ' + missing.slice(0, 3));
    check(JSON.stringify(counts(d)) === JSON.stringify(ix.counts), m.id + ': dev counts ' + JSON.stringify(counts(d)) + ' vs ' + JSON.stringify(ix.counts));
    sums.dev += ix.counts.entries; merged.dev.items.push(...(d.items || [])); merged.dev.blocks.push(...(d.blocks || []));
    if (unreleased.includes(m.id)) continue;
    const one = {}; one[m.id] = d; prune(one, unreleased);
    const pub = ix.countsPublic || ix.counts;
    check(JSON.stringify(counts(d)) === JSON.stringify(pub), m.id + ': public counts ' + JSON.stringify(counts(d)) + ' vs ' + JSON.stringify(pub));
    sums.pub += pub.entries; merged.pub.items.push(...(d.items || [])); merged.pub.blocks.push(...(d.blocks || []));
  }
  check(!!index.mods.simpleslabs.countsPublic, 'simpleslabs has public counts (chess half slabs)');
  // root sidebar "Blocks & items" over the merged scope = the sum of the per-mod numbers the light start page shows
  for (const v of ['dev', 'pub']) {
    const ids = new Set(merged[v].items.filter(e => isObj(e) && e.id).map(e => e.id));
    const n = merged[v].items.length + merged[v].blocks.filter(b => isObj(b) && !ids.has(b.id)).length;
    check(n === sums[v], v + ': merged blocks & items ' + n + ' = index sum ' + sums[v]);
  }
  const size = fs.statSync(path.join(WIKI, 'data/index.js')).size;
  check(size < 1.5e6, 'index.js stays small: ' + size);
  console.log(JSON.stringify({ checks, failures }));
  process.exit(failures.length ? 1 : 0);
})().catch(e => { console.log(JSON.stringify({ checks, failures: failures.concat(['crash: ' + e.stack]) })); process.exit(1); });
