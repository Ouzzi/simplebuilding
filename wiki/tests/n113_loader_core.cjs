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

async function run(href, presentFiles) {
  const u = new URL(href);
  const scripts = [], fetched = [];
  const win = {
    WIKI_MODULES: JSON.parse(JSON.stringify(catalogue)), WIKI_LOCAL_ONLY: localOnly, WIKI_DATA: {},
    location: { protocol: u.protocol, hostname: u.hostname, pathname: u.pathname, search: u.search },
    fetch: (url) => { fetched.push(url); return Promise.resolve(presentFiles ? { ok: true, json: () => Promise.resolve({ files: presentFiles }) } : { ok: false }); }
  };
  const doc = { createElement: () => { const s = {}; scripts.push(s); return s; }, head: { appendChild: s => setImmediate(() => s.onload && s.onload()) } };
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
