// N113 core test (run by test_no_placeholders.py, or directly: node wiki/tests/n113_loader_core.cjs).
// assets/module-loader.js in a sandbox per address: the public view leaves out the "release": false mods (not loaded,
// not listed), the dev wiki and local runs keep them; WIKI_ASSET_OK answers false for a git-ignored picture
// (data/local-only.js) until the host's data/derived-present.json names it. Then the real texture-page functions of
// wiki/index.html (txHostView, txSrc, txFinal, okTex): no request for a picture the host does not have.
// Prints one JSON line {checks, failures}.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const WIKI = path.resolve(__dirname, '..');
const loader = fs.readFileSync(path.join(WIKI, 'assets/module-loader.js'), 'utf8');
const html = fs.readFileSync(path.join(WIKI, 'index.html'), 'utf8');
let checks = 0;
const failures = [];
function check(cond, msg) { checks++; if (!cond) failures.push(msg); }

const catalogue = new Function('window', fs.readFileSync(path.join(WIKI, 'data/modules.js'), 'utf8') + '; return window.WIKI_MODULES;')({});
const localOnly = new Function('window', fs.readFileSync(path.join(WIKI, 'data/local-only.js'), 'utf8') + '; return window.WIKI_LOCAL_ONLY;')({});
const unreleased = catalogue.filter(m => m.release === false).map(m => m.id);
check(unreleased.length >= 3 && unreleased.includes('simplechess'), 'catalogue marks unreleased mods: ' + unreleased);
const someLocal = Object.keys(localOnly).map(k => k + localOnly[k][0] + '.png');
const render = Object.keys(localOnly).filter(k => k.includes('/render/')).map(k => k + localOnly[k][0] + '.png')[0];
check(!!render, 'local-only.js lists the git-ignored 3D renders');

const slabs = JSON.parse(fs.readFileSync(path.join(WIKI, 'data/simpleslabs.json'), 'utf8'));
async function run(href, presentFiles) {
  const u = new URL(href);
  const scripts = [], fetched = [];
  const win = {
    WIKI_MODULES: JSON.parse(JSON.stringify(catalogue)), WIKI_LOCAL_ONLY: localOnly, WIKI_DATA: {},
    location: { protocol: u.protocol, hostname: u.hostname, pathname: u.pathname, search: u.search },
    fetch: (url) => { fetched.push(url); return Promise.resolve(presentFiles ? { ok: true, json: () => Promise.resolve({ files: presentFiles }) } : { ok: false }); }
  };
  // O5b: data/simpleslabs.js runs like in the page (fills WIKI_MODULE_DATA) - with the chess half slabs and their recipes
  const doc = { createElement: () => { const s = {}; scripts.push(s); return s; }, head: { appendChild: s => setImmediate(() => {
    if (/^data\/simpleslabs\.js/.test(s.src)) win.WIKI_MODULE_DATA.simpleslabs = JSON.parse(JSON.stringify(slabs));
    if (s.onload) s.onload(); }) } };
  const ctx = { window: win, document: doc, Promise, setTimeout, Object, Array };
  vm.createContext(ctx);
  vm.runInContext(loader, ctx);
  await win.WIKI_READY;
  return { win, scripts: scripts.map(s => s.src.replace(/\?.*/, '')), fetched };
}

(async () => {
  // public wiki (production host): unreleased mods neither listed nor loaded, missing pictures not asked for
  const prod = await run('https://wiki.example/simpleores/textures', null);
  for (const id of unreleased) {
    check(!prod.win.WIKI_MODULES.some(m => m.id === id), 'prod lists ' + id);
    check(!prod.scripts.includes('data/' + id + '.js'), 'prod loads data/' + id + '.js');
    check(prod.win.WIKI_HIDDEN_MODULES.includes(id), 'prod hidden list lacks ' + id);
  }
  check(prod.scripts.length === catalogue.length - 1 - unreleased.length, 'prod loads every released mod: ' + prod.scripts.length);
  check(prod.fetched.includes('data/derived-present.json'), 'prod asks for the present list');
  check(someLocal.every(p => !prod.win.WIKI_ASSET_OK(p)), 'prod without a fill step: no local-only picture is "there"');
  check(prod.win.WIKI_ASSET_OK('assets/textures/simplechisels/item/iron_chisel.png'), 'committed pictures stay');
  // production host after the fill step: what it wrote is there, the rest (3D renders) is not
  const filled = await run('https://wiki.example/', someLocal.filter(p => !p.includes('/render/')));
  check(someLocal.filter(p => !p.includes('/render/')).every(p => filled.win.WIKI_ASSET_OK(p)), 'filled pictures are there');
  check(!filled.win.WIKI_ASSET_OK(render), 'a render the host does not have is not there');
  // dev wiki: everything listed and loaded (marked in the page), the same present-list rule
  const dev = await run('https://wiki.example/dev/simplechess/', []);
  for (const id of unreleased) check(dev.scripts.includes('data/' + id + '.js') || id === 'simplebuilding', 'dev loads ' + id);
  check(dev.win.WIKI_HIDDEN_MODULES.length === 0, 'dev hides nothing');
  check(!dev.win.WIKI_ASSET_OK(render), 'dev: render missing unless written');
  // O5b: Simple Slabs' chess half slabs load only with Simple Chess (recipe load conditions, SlabRegistry): the public view
  // leaves out those recipes and blocks with their block loot, the lists and counts follow; the dev view keeps all
  const chessRecipes = slabs.recipes.filter(r => (r.needsMods || []).includes('simplechess') || JSON.stringify(r).includes('simplechess:'));
  const chessBlocks = slabs.blocks.filter(b => (b.needsMods || []).includes('simplechess'));
  check(chessRecipes.length >= 100 && chessBlocks.length === 24, 'generator marks the chess half slabs: ' + chessRecipes.length + ' recipes, ' + chessBlocks.length + ' blocks');
  check(chessRecipes.every(r => (r.needsMods || []).includes('simplechess')), 'every recipe with a chess id carries needsMods simplechess');
  const ps = prod.win.WIKI_MODULE_DATA.simpleslabs;
  check(!JSON.stringify(ps.recipes).includes('simplechess'), 'prod: no slabs recipe with a chess id or condition left');
  check(!ps.blocks.some(b => /quartz_checker|_checker_/.test(b.id)), 'prod: chess half slabs gone');
  check(ps.blocks.length === slabs.blocks.length - 24 && ps.counts.blocks === ps.blocks.length && ps.counts.recipes === ps.recipes.length, 'prod: counts follow');
  const ids = new Set(ps.recipes.map(r => r.id));
  check(ps.blocks.every(b => b.craftedBy.concat(b.usedIn).every(id => ids.has(id))), 'prod: craftedBy/usedIn name only recipes that are there');
  check(!ps.lootTables.some(t => /checker/.test(t.id)) && ps.lootTables.length === slabs.lootTables.length - 24, 'prod: their block loot gone');
  check(ps.blocks.some(b => b.id === 'simpleslabs:oak_half_slab') && ps.recipes.length > slabs.recipes.length - chessRecipes.length - 1, 'prod: the rest stays');
  const ds = dev.win.WIKI_MODULE_DATA.simpleslabs;
  check(ds.blocks.length === slabs.blocks.length && ds.recipes.length === slabs.recipes.length, 'dev keeps the chess half slabs');
  // a slot that another (released) item can fill keeps the recipe; a slot only a hidden mod fills drops it
  const t = { x: { recipes: [{ id: 'x:a', result: { id: 'x:a' }, ingredients: ['simplechess:p', 'minecraft:stone'], slots: { ingredient: ['simplechess:p', 'minecraft:stone'] } },
    { id: 'x:b', result: { id: 'x:b' }, ingredients: ['simplechess:p'], key: { A: ['simplechess:p'] } }, { id: 'x:c', result: { id: 'simplechess:q' }, ingredients: [] }],
    items: [{ id: 'x:a', craftedBy: ['x:a'], usedIn: ['x:b'] }], counts: {} } };
  prod.win.WIKI_PRUNE_HIDDEN(t, ['simplechess']);
  check(t.x.recipes.map(r => r.id).join() === 'x:a' && t.x.items[0].usedIn.length === 0 && t.x.counts.recipes === 1, 'slot rule: ' + JSON.stringify(t.x.recipes.map(r => r.id)));
  // ?release=1 = public view on the dev wiki
  const devRel = await run('https://wiki.example/dev/?release=1', []);
  check(unreleased.every(id => !devRel.win.WIKI_MODULES.some(m => m.id === id)), '?release=1 hides unreleased on dev');
  // local (generate run, harness): everything, no present list asked for
  for (const href of ['file:///x/wiki/index.html', 'http://localhost:8000/', 'http://127.0.0.1:9/']) {
    const loc = await run(href, null);
    check(loc.win.WIKI_MODULES.length === catalogue.length, 'local keeps all mods ' + href);
    check(someLocal.every(p => loc.win.WIKI_ASSET_OK(p)), 'local: all pictures there ' + href);
    check(!loc.fetched.length, 'local asks for nothing ' + href);
  }

  // texture page functions of index.html with the production state
  function fnSource(name) {
    const at = html.indexOf('\nfunction ' + name + '(');
    if (at < 0) throw new Error('missing function ' + name);
    let i = html.indexOf('{', at), depth = 0;
    for (; i < html.length; i++) { const c = html[i]; if (c === '{') depth++; else if (c === '}' && --depth === 0) return html.slice(at + 1, i + 1); }
    throw new Error('unbalanced ' + name);
  }
  const ctx = { window: prod.win };
  vm.createContext(ctx);
  vm.runInContext(['esc', 'assetOk', 'okTex', 'txHostView', 'txSrc', 'txFrame', 'txCrop', 'txFinal'].map(fnSource).join('\n'), ctx);
  const ownFile = 'assets/textures/simplechisels/item/iron_chisel.png';
  const derivedFile = someLocal.find(p => !p.includes('/render/'));
  const t1 = ctx.txHostView({ id: 'a', file: ownFile, icon: render, w: 16, h: 16 });
  const h1 = ctx.txFinal(t1, 48);
  check(!h1.includes(render) && h1.includes(ownFile) && t1.iconLocal === render, 'missing render: card shows the texture, no request for the render');
  const h2 = ctx.txFinal(ctx.txHostView({ id: 'b', file: derivedFile, w: 16, h: 16 }), 48);
  check(!h2.includes(derivedFile) && h2.includes('src="data:,"'), 'missing derived texture: no request (data:, -> "not published" box)');
  check(ctx.okTex(render, derivedFile, ownFile) === ownFile && ctx.okTex(render) === null, 'okTex takes the first picture the host has');
  const t3 = ctx.txHostView({ id: 'c', file: ownFile, icon: ownFile, w: 16, h: 16 });
  check(t3.icon === ownFile, 'pictures the host has stay');
  console.log(JSON.stringify({ checks, failures }));
  process.exit(failures.length ? 1 : 0);
})().catch(e => { console.log(JSON.stringify({ checks, failures: failures.concat(['crash: ' + e.stack]) })); process.exit(1); });
