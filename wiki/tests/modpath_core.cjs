// N74 core test of the wiki addresses (run by test_modpath.py, or directly: node wiki/tests/modpath_core.cjs).
// Takes the real address functions out of wiki/index.html (urlFor, pathParts, pathToRoute, ownerOf, the start-up mod
// choice START ...) and runs them in a sandbox with the real catalogue and data: path form <-> route both ways for every
// wiki mod x section (ids from the mod's own data), and the old ?mod= / #/ links of the concept table.
// Prints one JSON line {checks, failures}.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const WIKI = path.resolve(__dirname, '..');
const html = fs.readFileSync(path.join(WIKI, 'index.html'), 'utf8');

function fnSource(name) {
  const at = html.indexOf('\nfunction ' + name + '(');
  if (at < 0) throw new Error('missing function ' + name);
  let i = html.indexOf('{', at), depth = 0;
  for (; i < html.length; i++) {
    const c = html[i];
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return html.slice(at + 1, i + 1);
  }
  throw new Error('unbalanced ' + name);
}
function varSource(name, endMarker) {
  const at = html.indexOf('\nvar ' + name + ' = ');
  if (at < 0) throw new Error('missing var ' + name);
  return html.slice(at + 1, html.indexOf(endMarker, at) + endMarker.length);
}

const data = { window: {} };
vm.createContext(data);
vm.runInContext(fs.readFileSync(path.join(WIKI, 'data/modules.js'), 'utf8'), data);
for (const m of data.window.WIKI_MODULES) {
  const f = path.join(WIKI, 'data', m.id + '.js');
  if (fs.existsSync(f)) vm.runInContext(fs.readFileSync(f, 'utf8'), data);
}
const MODULE_DATA = Object.assign({}, data.window.WIKI_MODULE_DATA);
if (!MODULE_DATA.simplebuilding && data.window.WIKI_DATA) MODULE_DATA.simplebuilding = data.window.WIKI_DATA;

const FUNCS = ['arr', 'isObj', 'nsOf', 'hasData', 'wikiMod', 'modMetaRaw', 'pathParts', 'relPath', 'ownerOf', 'routeParams', 'readableQuery', 'urlFor',
  'pathToRoute', 'paramsToRoute', 'hashToRoute', 'urlMod'];
const src = [
  varSource('ROUTE_PARAM', ';'), varSource('PARAM_ROUTE', ';'), varSource('NS_SECTIONS', ';'),
  FUNCS.map(fnSource).join('\n'),
  'function catDef(k) { return null; }',
  'var IX = { items: new Map(), blocks: new Map(), config: new Map(), trades: new Map(), recipes: new Map() };',
  'function startOf() {\n' + varSource('START', '})();') + '\nreturn START; }'
].join('\n');

function sandbox(loc, modpath) {
  const u = new URL(loc, 'http://wiki.test/');
  const ctx = {
    URL, URLSearchParams, MODULES: data.window.WIKI_MODULES, MODULE_DATA, WIDX: {}, PACK_HIDDEN: { wiringexample: 1 },
    PACK: { key: 'all' }, LANG: 'en', MODPATH: modpath, BASE: modpath ? '/' : '',
    location: { pathname: u.pathname, search: u.search, hash: u.hash, href: u.href, protocol: 'http:' }
  };
  ctx.modParams = new URLSearchParams(u.search);
  vm.createContext(ctx);
  vm.runInContext(src, ctx);
  return ctx;
}

let checks = 0;
const failures = [];
function check(cond, msg) { checks++; if (!cond) failures.push(msg); }

// 1) round trip for every wiki mod x section, with real ids
const base = sandbox('/', true);
const mods = data.window.WIKI_MODULES.map(m => m.id).filter(id => base.wikiMod(id));
check(mods.length >= 60, 'wiki mods ' + mods.length);
const KEYS = { items: 'items', blocks: 'blocks', recipes: 'recipes', loot: 'lootTables', trades: 'trades', enchantments: 'enchantments', tags: 'tags', features: 'features', config: 'config', make: 'items', use: 'items', tree: 'items' };
const PLAIN = ['overview', 'start', 'submods', 'guidebook', 'questionary', 'allrecipes', 'loot', 'trades', 'enchantments', 'tags', 'config', 'advancements', 'commands', 'structure', 'browse', 'textures'];
let ids = 0;
for (const mod of mods) {
  const routes = PLAIN.map(s => ({ section: s, id: '' }));
  for (const [section, key] of Object.entries(KEYS)) {
    const list = (MODULE_DATA[mod][key] || []).slice(0, 3);
    for (const e of list) routes.push({ section, id: e.id || e.name });
  }
  routes.push({ section: 'search', id: 'ash wood' }, { section: 'items', id: 'minecraft:stone' }, { section: 'tree', id: '#minecraft:planks' }, { section: 'cat', id: 'tools' });
  for (const r of routes) {
    const url = base.urlFor(mod, Object.assign({ tab: '', focus: '' }, r));
    const u = new URL(url, 'http://wiki.test/');
    const pp = base.pathParts(u.pathname.slice(1));
    check(pp && pp.mod === mod, `${mod} ${r.section} ${r.id}: mod segment of ${url}`);
    if (!pp) continue;
    const back = base.pathToRoute(pp, u.searchParams);
    const want = r.section === 'allrecipes' ? 'recipes' : r.section;
    check(back.section === want && (back.id || '') === (r.id || ''), `${mod} ${r.section} ${r.id}: ${url} -> ${JSON.stringify(back)}`);
    if (r.id && KEYS[r.section] && r.id.indexOf(mod + ':') === 0) check(url.indexOf(mod + ':') < 0, `${url} keeps its own namespace`);
    ids++;
  }
  // query form (no server rule) round trip of the same routes
  const q = base.urlFor.call(null, mod, { section: 'items', id: (MODULE_DATA[mod].items[0] || {}).id || '' });
  check(q.indexOf('/' + mod + '/') === 0, 'path form for ' + mod);
}
check(ids > 1500, 'round trips ' + ids);
const query = sandbox('/index.html', false);
for (const mod of ['simplebuilding', 'simpleend', 'simplelib']) {
  const it = MODULE_DATA[mod].items[0];
  const url = query.urlFor(mod, { section: 'items', id: it ? it.id : '' });
  check(url === '/index.html?mod=' + mod + (it ? '&item=' + it.id : ''), 'query form ' + url);
  const r = query.paramsToRoute(new URL(url, 'http://x/').searchParams);
  check(r.section === (it ? 'items' : 'overview') && r.id === (it ? it.id : ''), 'query round trip ' + url);
}
check(query.urlFor(null, { section: 'search', id: 'chisel' }) === '/index.html?q=chisel', 'root search, query form');
check(base.urlFor(null, { section: 'search', id: 'chisel' }) === '/?q=chisel', 'root search, path form');
check(base.urlFor(null, { section: 'items', id: 'simpleend:ashwood_boat' }) === '/simpleend/items/ashwood_boat', 'root item -> owner area');
check(base.urlFor(null, { section: 'items', id: '' }) === '/?tab=items', 'root list');
check(base.urlFor('simplebuilding', { section: 'allmods' }) === '/', 'all mods -> root');
check(base.urlFor('nosuchmod', { section: 'items' }) === '/?tab=items', 'unknown mod -> root');

// 2) old links (concept table) -> start mod + canonical address
const LEGACY = [
  ['/?mod=simplebuilding', 'simplebuilding', '/simplebuilding/'],
  ['/?mod=simpleend&item=simpleend:ashwood_boat&lang=de', 'simpleend', '/simpleend/items/ashwood_boat'],
  ['/?mod=simplebuilding&tab=recipes', 'simplebuilding', '/simplebuilding/recipes'],
  ['/?mod=simplebuilding#/allmods', null, '/'],
  ['/#/items/simplebuilding:enderite_chisel', 'simplebuilding', '/simplebuilding/items/enderite_chisel'],
  ['/?item=simplechisels:copper_chisel', 'simplechisels', '/simplechisels/items/copper_chisel'],
  ['/?pack=super:simplebuilding', null, '/'],
  ['/?tab=allmods', null, '/'],
  ['/#/features/welcome', 'simplebuilding', '/simplebuilding/features/welcome'],
  ['/simpleend/items/ashwood_boat', 'simpleend', '/simpleend/items/ashwood_boat'],
  ['/simplenosuch/x', null, '/'],
  ['/', null, '/']
];
for (const [loc, mod, want] of LEGACY) {
  const ctx = sandbox(loc, true);
  const st = ctx.startOf();
  check(st.mod === mod, `${loc}: start mod ${st.mod} (want ${mod})`);
  const u = new URL(loc, 'http://wiki.test/');
  const pp = ctx.pathParts(u.pathname.slice(1));
  let r = u.hash.indexOf('#/') === 0 ? ctx.hashToRoute(u.hash) : pp ? ctx.pathToRoute(pp, u.searchParams) : ctx.paramsToRoute(u.searchParams);
  if (r.section === 'allmods') r = { section: 'overview', id: '' };
  const got = ctx.urlFor(st.mod, r);
  check(got === want, `${loc}: -> ${got} (want ${want})`);
}
console.log(JSON.stringify({ checks, failures: failures.slice(0, 30), nFailures: failures.length }));
process.exit(failures.length ? 1 : 0);
