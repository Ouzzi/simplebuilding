// N74 browser test (docs/ai/PLAN-N74-WIKINAV-2026-11-16.md): wiki navigation without the mod drop-down.
// Playwright (or playwright-core) in NODE_PATH, no Minecraft runtime. Runs on sb-test:
//   NODE_PATH=/var/tmp/sb/pw/node_modules WIKI_CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   NAV74_SHOTS=/var/tmp/sb/pw/n74 node wiki/tests/nav74_ui.cjs
// Deployed dev wiki: WIKI_BASE=https://wiki.simplemods.de/dev/ WIKI_COOKIE=sb_session=<value> (path-form run only, the
// query-form run needs the local server without the rule).
// Two static servers: "path" behaves like nginx with the N74 rule (/<mod>/... -> index.html, <base href="/" data-modpath>),
// "query" like a server without it (the wiki has to stay on ?mod=). Checks: start page tiles, mod area with its own
// sidebar (back link, head, counters = family sum, sub-mods with filter), breadcrumb, flat addresses, search inside the
// mod + "Search in all mods", old ?mod= links, browser back, mod structure as the only page with the modpack bar,
// 1280 and 390 px, light and dark: no horizontal scrolling, text contrast >= 4.5:1, no script errors.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const root = path.resolve(__dirname, '..');
const shots = process.env.NAV74_SHOTS || os.tmpdir();
fs.mkdirSync(shots, { recursive: true });
const TYPES = { '.js': 'text/javascript', '.html': 'text/html', '.json': 'application/json', '.png': 'image/png' };
function makeServer(mode) {
  return http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    let p = decodeURIComponent(url.pathname);
    const flat = mode === 'path' && /^\/simple[a-z0-9_]+(\/.*)?$/.test(url.pathname);
    if (p === '/' || flat) p = '/index.html';
    const file = path.resolve(root, '.' + p);
    if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
    fs.readFile(file, (err, data) => {
      if (err) { res.writeHead(404).end(); return; }
      const type = TYPES[path.extname(file)] || 'application/octet-stream';
      res.setHeader('Content-Type', type);
      if (mode === 'path' && type === 'text/html') data = Buffer.from(String(data).replace('<head>', '<head><base href="/" data-modpath>'));
      res.end(data);
    });
  });
}
const CONTRAST = el => {
  const rgb = s => (s.match(/[\d.]+/g) || []).map(Number);
  const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
  let fg = rgb(getComputedStyle(el).color), n = el, bg = null;
  while (n && n.nodeType === 1) { const c = rgb(getComputedStyle(n).backgroundColor); if (c.length === 3 || (c.length === 4 && c[3] > 0.95)) { bg = c; break; } n = n.parentElement; }
  bg = bg || rgb(getComputedStyle(document.body).backgroundColor);
  const a = lum(fg), b = lum(bg);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
};
let checks = 0;
const ok = (cond, msg) => { assert.ok(cond, msg); checks++; };
const eq = (a, b, msg) => { assert.deepEqual(a, b, msg); checks++; };

async function contrast(page, tag, sels) {
  for (const sel of sels) {
    const els = page.locator(sel).filter({ visible: true });
    if (!(await els.count())) continue;
    const c = await els.first().evaluate(CONTRAST);
    ok(c >= 4.5, `${tag} ${sel} contrast ${c.toFixed(2)}`);
  }
}
async function noOverflow(page, tag) {
  const o = await page.evaluate(() => ({ doc: document.documentElement.scrollWidth, w: window.innerWidth }));
  ok(o.doc <= o.w, tag + ' horizontal scroll ' + JSON.stringify(o));
}
const PFX = process.env.WIKI_BASE ? new URL(process.env.WIKI_BASE).pathname.replace(/\/$/, '') : '';
const pn = u => { const p = new URL(u).pathname; return PFX && p.startsWith(PFX) ? p.slice(PFX.length) || '/' : p; };
const st = h => (PFX && typeof h === 'string' && h.startsWith(PFX + '/') ? h.slice(PFX.length) : h);
const rel = page => { const u = new URL(page.url()); return pn(u) + u.search; };
async function ready(page) { await page.locator('#main h1').first().waitFor(); }
async function open(page, base, p) { await page.goto(base + p); await ready(page); }
/* sum of a data key over a mod family, from the loaded data */
const famItems = (page, id) => page.evaluate(id => {
  const M = window.WIKI_MODULES, D = window.WIKI_MODULE_DATA;
  const fam = x => [x].concat(...M.filter(m => m.parent === x && m.id !== 'wiringexample').map(m => fam(m.id)));
  let n = 0;
  fam(id).forEach(m => { const d = D[m]; if (!d) return; const items = new Set((d.items || []).map(e => e.id)); n += items.size + (d.blocks || []).filter(b => !items.has(b.id)).length; });
  return n;
}, id);

async function pathRun(browser, base, width, scheme) {
  const lang = scheme === 'dark' ? 'de' : 'en', de = lang === 'de';
  const ctx = await browser.newContext({ viewport: { width, height: 900 }, colorScheme: scheme });
  // WIKI_OVERLAY=<file>: try a local dev-overlay.js on the deployed dev wiki
  if (process.env.WIKI_OVERLAY) await ctx.route('**/_overlay.js', r => r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(process.env.WIKI_OVERLAY, 'utf8') }));
  if (process.env.WIKI_COOKIE) { const [name, ...rest] = process.env.WIKI_COOKIE.split('='); await ctx.addCookies([{ name, value: rest.join('='), url: new URL('/', base).href }]); }
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  const tag = `path ${width} ${scheme}`;
  const phone = width < 700;
  const L = de ? '?lang=de' : '';
  const sideOpen = async () => { if (phone && !(await page.evaluate(() => document.body.classList.contains('nav-open')))) await page.click('#menu-btn'); };

  // ---- start page: 4 big tiles, 3 small, no drop-down, no whole-pack switch, no modpack bar ----
  await open(page, base, L);
  eq(rel(page), '/' + L, tag + ' home address');
  ok(/Simple Mods Wiki/.test(await page.locator('#main h1').first().textContent()), tag + ' home title');
  eq(await page.locator('.modtiles:not(.small) .modtile').count(), 4, tag + ' 4 super-mod tiles');
  eq(await page.locator('.modtiles.small .modtile').count(), 3, tag + ' 3 standalone tiles');
  eq(await page.locator('#mod-select, #all-mods, .allmods, .packbar').count(), 0, tag + ' no drop-down / whole pack / modpack bar');
  eq(await page.locator('#sidebar .sb-mods a').count(), 7, tag + ' 7 mods in the root sidebar');
  ok(await page.locator('#sidebar details[data-sb="all-reference"]').count() === 1, tag + ' root sidebar: all mods reference group');
  eq(await page.locator('#modstrip').isVisible(), false, tag + ' no mod strip on the start page');
  await noOverflow(page, tag + ' home');
  await contrast(page, tag + ' home', ['.modtile .mt-name', '.modtile .mt-kind', '.modtile .mt-desc', '.modtile .mt-n', '.statrow .tile .lbl', '.home-hero .lead']);
  await page.screenshot({ path: path.join(shots, `n74-home-${width}-${scheme}.png`) });

  // ---- tile -> mod area ----
  await page.locator('.modtile[data-mod="simplebuilding"]').click();
  await page.waitForURL(u => pn(u) === '/simplebuilding/'); await ready(page);
  ok(/Wiki$/.test(await page.locator('#main h1').first().textContent()), tag + ' mod overview');
  eq((await page.locator('#main .crumbs').first().innerText()).replace(/\s+/g, ' ').trim(), (de ? 'Alle Mods' : 'All mods') + ' › SimpleBuilding', tag + ' breadcrumb mod');
  await sideOpen();
  ok(await page.locator('#sidebar a.sb-back').isVisible(), tag + ' back link in the sidebar');
  eq((await page.locator('#sidebar .sb-mod-name').textContent()).trim(), 'SimpleBuilding', tag + ' sidebar mod head');
  const sbItems = await page.locator('#sidebar a[href="' + PFX + '/simplebuilding/items' + L + '"] .n').textContent();
  eq(Number(sbItems.replace(/\D/g, '')), await famItems(page, 'simplebuilding'), tag + ' counter = family sum (SimpleBuilding)');
  // sub-mods with a filter field (42 > 8)
  const subs = page.locator('#sidebar details[data-sb="subs-own"] a.sub');
  const nSubs = await subs.count();
  ok(nSubs > 8 && await page.locator('#sidebar [data-sb-filter]').count() === 1, tag + ' sub-mod filter shown for ' + nSubs);
  await page.fill('#sidebar [data-sb-filter]', 'end');
  const vis = await subs.evaluateAll(as => as.filter(a => !a.hidden).map(a => a.textContent));
  ok(vis.length >= 2 && vis.length < nSubs && vis.every(t => /end/i.test(t)), tag + ' sub-mod filter narrows ' + JSON.stringify(vis));
  await contrast(page, tag + ' sidebar', ['#sidebar a span', '#sidebar .sb-mod-name', '#sidebar .sb-note', '#sidebar summary span']);
  await page.screenshot({ path: path.join(shots, `n74-mod-${width}-${scheme}.png`) });
  await page.locator('#sidebar a.sub', { hasText: /^Simple End$/ }).click();
  await page.waitForURL(u => pn(u) === '/simpleend/'); await ready(page);
  eq((await page.locator('#main .crumbs').first().innerText()).replace(/\s+/g, ' ').trim(), (de ? 'Alle Mods' : 'All mods') + ' › SimpleBuilding › Simple End', tag + ' breadcrumb sub-mod');
  await sideOpen();
  ok(/SimpleBuilding/.test(await page.locator('#sidebar .sb-mod-up').textContent()), tag + ' sub-mod of');
  eq(Number((await page.locator('#sidebar a[href="' + PFX + '/simpleend/items' + L + '"] .n').textContent()).replace(/\D/g, '')), await famItems(page, 'simpleend'), tag + ' counter = Simple End + Dragon Fight');
  ok(/Simple End \+ 1/.test(await page.locator('#sidebar .sb-note').textContent()), tag + ' counts note');
  if (phone) {
    ok(await page.locator('#modstrip').isVisible(), tag + ' mod strip on the phone');
    ok(/Simple End/.test(await page.locator('#modstrip .ms-name').textContent()), tag + ' mod strip name');
    const h = await page.locator('#modstrip .backbtn').evaluate(e => e.getBoundingClientRect().height);
    ok(h >= 32, tag + ' strip back button height ' + h);
    await contrast(page, tag + ' strip', ['#modstrip .ms-name', '#modstrip .ms-up', '#modstrip .backbtn']);
  } else eq(await page.locator('#modstrip').isVisible(), false, tag + ' no mod strip on the desktop');
  await page.locator('#sidebar a.sub', { hasText: /Dragon/ }).first().click();
  await page.waitForURL(u => pn(u) === '/simpleenddragonfight/'); await ready(page);
  await sideOpen();
  eq(await page.locator('#sidebar details[data-sb="subs-more"]').count(), 1, tag + ' leaf sub-mod: more in parent');
  eq(await page.locator('#sidebar details[data-sb="subs-more"]').evaluate(d => d.open), false, tag + ' more in parent folded');
  await page.screenshot({ path: path.join(shots, `n74-submod-${width}-${scheme}.png`) });
  if (phone) await page.click('#menu-btn');
  // breadcrumb back to Simple End, browser back/forward
  await page.locator('#main .crumbs a', { hasText: /^Simple End$/ }).click();
  await page.waitForURL(u => pn(u) === '/simpleend/'); await ready(page);
  await page.goBack(); await page.waitForURL(u => pn(u) === '/simpleenddragonfight/'); await ready(page);
  await page.goBack(); await page.waitForURL(u => pn(u) === '/simpleend/'); await ready(page);
  await page.goBack(); await page.waitForURL(u => pn(u) === '/simplebuilding/'); await ready(page);
  await page.goForward(); await page.waitForURL(u => pn(u) === '/simpleend/'); await ready(page);
  checks++;

  // ---- in-area pages: flat item address, back button inside the area ----
  await sideOpen();
  await page.locator('#sidebar a[href="' + PFX + '/simpleend/items' + L + '"]').click();
  await page.waitForURL(u => pn(u) === '/simpleend/items'); await ready(page);
  ok(/Simple End › (Blocks & items|Blöcke & Items|Items)/.test((await page.locator('#main .crumbs').first().innerText()).replace(/\s+/g, ' ')), tag + ' list breadcrumb');
  const rowHrefs = await page.locator('#main .rows > a.row, #main .rows > .row-rc > a.row-head').evaluateAll(as => as.map(a => a.getAttribute('href'))).then(x => x.map(st));
  ok(rowHrefs.length > 0 && rowHrefs.every(h => /^\/simpleend\/(items|blocks)\//.test(h)), tag + ' item links stay in the area');
  ok(rowHrefs.some(h => /^\/simpleend\/(items|blocks)\/simpleenddragonfight:/.test(h)), tag + ' sub-mod items listed with their namespace');
  ok(await page.locator('#main .row .badge', { hasText: 'Simple End Dragon Fight' }).count() > 0, tag + ' sub-mod badge in the list');
  await page.locator('#main a.row[href="' + PFX + '/simpleend/items/ashwood_boat' + L + '"], #main a.row-head[href="' + PFX + '/simpleend/items/ashwood_boat' + L + '"]').first().click();
  await page.waitForURL(u => pn(u) === '/simpleend/items/ashwood_boat'); await ready(page);
  await noOverflow(page, tag + ' item page');
  await page.goBack(); await page.waitForURL(u => pn(u) === '/simpleend/items'); await ready(page);
  checks++;

  // ---- search inside the mod + "search in all mods" ----
  await page.fill('#q', 'ash');
  await page.locator('#qs .qs-scope').waitFor();
  ok(/Simple End \+ 1/.test(await page.locator('#qs .qs-scope').textContent()), tag + ' quick search scope');
  const qsHrefs = await page.locator('#qs a.qs-item:not(.qs-all)').evaluateAll(as => as.map(a => a.getAttribute('href'))).then(x => x.map(st));
  ok(qsHrefs.length > 0 && qsHrefs.every(h => h.indexOf('/simpleend/') === 0), tag + ' quick search stays in the area ' + JSON.stringify(qsHrefs.slice(0, 3)));
  const wide = page.locator('#qs [data-qs-wide]');
  ok(await wide.count() === 1, tag + ' "search in all mods" row');
  const wideN = Number((await wide.textContent()).replace(/\D/g, ''));
  const narrowN = Number((await page.locator('#qs .qs-scope span').nth(1).textContent()).replace(/\D/g, ''));
  ok(wideN > narrowN, tag + ` all mods give more (${wideN} > ${narrowN})`);
  await contrast(page, tag + ' quick search', ['#qs .qs-scope', '#qs .qs-wide']);
  if (!phone) await page.screenshot({ path: path.join(shots, `n74-search-${width}-${scheme}.png`) });
  await wide.click();
  ok(new RegExp(de ? 'allen' : 'all').test(await page.locator('#qs .qs-scope').textContent()), tag + ' widened once');
  ok(await page.locator('#qs [data-qs-narrow]').count() === 1, tag + ' back to the mod');
  await page.locator('#q').focus();
  await page.keyboard.press('Control+Enter');
  await page.waitForURL(u => new URL(u).search.indexOf('q=ash') >= 0 && pn(u) === '/'); await ready(page);
  const allHits = await page.locator('#main .search-group a.row').count();
  ok(allHits > narrowN, tag + ' wide search page ' + allHits);
  await page.goBack(); await page.waitForURL(u => pn(u) === '/simpleend/items'); await ready(page);
  await page.fill('#q', 'ash');
  await page.locator('#qs .qs-scope').waitFor();
  await page.keyboard.press('Control+Enter');
  await page.waitForURL(u => pn(u) === '/simpleend/' && new URL(u).search.indexOf('q=ash') >= 0); await ready(page);
  const inHits = await page.locator('#main .search-group a.row').count();
  ok(inHits > 0 && inHits < allHits, tag + ` search page in the mod (${inHits})`);
  await page.locator('#main a[data-search-wide]').click();
  await page.waitForURL(u => pn(u) === '/' && new URL(u).search.indexOf('q=ash') >= 0); await ready(page);
  checks++;

  // ---- back to all mods ----
  await open(page, base, 'simplequalityoflife/' + L);
  await sideOpen();
  await page.locator('#sidebar a.sb-back').click();
  await page.waitForURL(u => pn(u) === '/' && !new URL(u).searchParams.has('mod')); await ready(page);
  eq(await page.locator('.modtile').count(), 7, tag + ' back on the start page');

  // ---- old links ----
  const legacy = [
    ['?mod=simplebuilding', '/simplebuilding/'],
    ['?mod=simpleend&item=simpleend:ashwood_boat&lang=de', '/simpleend/items/ashwood_boat?lang=de'],
    ['?mod=simplebuilding&tab=recipes', '/simplebuilding/recipes'],
    ['?mod=simplebuilding#/allmods', '/'],
    ['#/items/simplebuilding:enderite_chisel', '/simplebuilding/items/enderite_chisel'],
    ['?item=simplechisels:copper_chisel', '/simplechisels/items/copper_chisel'],
    ['#/items/simplechisels:copper_chisel', '/simplechisels/items/copper_chisel'],
    ['?mod=nosuchmod', '/'],
    ['simplenosuch/items/x', '/']
  ];
  if (scheme === 'light' && !phone) {
    for (const [from, to] of legacy) {
      await page.goto(base + from); await ready(page);
      eq(rel(page), to, tag + ' old link ' + from);
      // enderite_chisel (concept table) is no longer an item: the old link still lands in the area, on "not found" with suggestions
      if (from.indexOf('enderite_chisel') < 0) ok(!/not found|nicht gefunden/i.test(await page.locator('#main h1').first().textContent()), tag + ' old link renders ' + from);
      else ok(await page.locator('#main .notfound .rows a.row').count() > 0, tag + ' old link to a removed item offers suggestions');
    }
    // modpack filter from an old link: start page with the hint, the bar only on the structure page
    await open(page, base, '?pack=super:simplebuilding');
    ok(await page.locator('#main .pb-note [data-pack-set="all"]').count() === 1, tag + ' filter hint on the start page');
    ok(await page.locator('.modtile').count() < 7, tag + ' filter narrows the tiles');
    await page.locator('#main [data-pack-set="all"]').click();
    await page.waitForFunction(() => document.querySelectorAll('.modtile').length === 7);
    await open(page, base, '?tab=structure');
    eq(await page.locator('#main .packbar').count(), 1, tag + ' modpack bar on mod structure');
    await open(page, base, 'simplebuilding/structure');
    eq(await page.locator('#main .packbar').count(), 1, tag + ' modpack bar on mod structure in an area');
    await open(page, base, 'simplebuilding/items');
    eq(await page.locator('#main .packbar').count(), 0, tag + ' no modpack bar elsewhere');
    // root list pages: entries open in the area of their mod
    await open(page, base, '?tab=items');
    const href = await page.locator('#main a.row[href*="/items/"]').first().getAttribute('href').then(st);
    ok(/^\/simple[a-z0-9_]+\/items\//.test(href), tag + ' root item list links into the mod area ' + href);
  }
  // phone: no horizontal scrolling on the main pages
  if (phone) {
    for (const p of ['', 'simplebuilding/', 'simpleend/items/ashwood_boat', 'simplebuilding/items', '?tab=structure']) {
      await open(page, base, p + (p.indexOf('?') >= 0 ? L.replace('?', '&') : L));
      await noOverflow(page, tag + ' /' + p);
    }
    await open(page, base, 'simpleend/' + L);
    await page.screenshot({ path: path.join(shots, `n74-phone-mod-${width}-${scheme}.png`) });
    await page.click('#menu-btn');
    await page.locator('#sidebar .sb-mod-name').waitFor();
    await page.screenshot({ path: path.join(shots, `n74-phone-sidebar-${width}-${scheme}.png`) });
  }
  eq(errors, [], tag + ' script errors');
  await ctx.close();
}

async function queryRun(browser, base) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  const tag = 'query';
  await open(page, base, '');
  eq(await page.locator('.modtile').count(), 7, tag + ' start tiles');
  eq(await page.locator('base').count(), 0, tag + ' no base without the server rule');
  const tile = await page.locator('.modtile[data-mod="simplebuilding"]').getAttribute('href').then(st);
  ok(/^\/\?mod=simplebuilding$/.test(tile), tag + ' tile link in query form ' + tile);
  await page.locator('.modtile[data-mod="simplebuilding"]').click();
  await page.waitForURL(u => new URL(u).searchParams.get('mod') === 'simplebuilding'); await ready(page);
  ok(await page.locator('#sidebar a.sb-back').count() === 1, tag + ' mod area sidebar');
  await page.locator('#sidebar a.sub', { hasText: /^Simple End$/ }).click();
  await page.waitForURL(u => new URL(u).searchParams.get('mod') === 'simpleend'); await ready(page);
  const items = page.locator('#sidebar a[href="/?mod=simpleend&tab=items"]');
  ok(await items.count() === 1, tag + ' section links in query form');
  await items.click(); await ready(page);
  eq(rel(page), '/?mod=simpleend&tab=items', tag + ' list address');
  await page.goBack(); await page.waitForURL(u => !new URL(u).searchParams.has('tab'));
  checks++;
  for (const [from, to] of [['?mod=simpleend&item=simpleend:ashwood_boat', '/?mod=simpleend&item=simpleend:ashwood_boat'], ['?mod=simplebuilding#/allmods', '/'],
    ['#/items/simplebuilding:enderite_chisel', '/?mod=simplebuilding&item=simplebuilding:enderite_chisel'], ['?item=simplechisels:copper_chisel', '/?mod=simplechisels&item=simplechisels:copper_chisel']]) {
    await page.goto(base + from); await ready(page);
    eq(rel(page), to, tag + ' old link ' + from);
  }
  ok((await page.request.get(base + 'simplebuilding/')).status() === 404, tag + ' path form is not served without the rule (wiki never links there)');
  await open(page, base, '?mod=simpleend');
  const links = await page.locator('a[href]').evaluateAll(as => as.map(a => a.getAttribute('href')).filter(h => /^\/simple/.test(h)));
  eq(links, [], tag + ' no path-form links without the rule');
  eq(errors, [], tag + ' script errors');
  await ctx.close();
}

(async () => {
  const servers = { path: makeServer('path'), query: makeServer('query') };
  await Promise.all(Object.values(servers).map(s => new Promise(r => s.listen(0, '127.0.0.1', r))));
  let browser;
  try {
    const exe = process.env.WIKI_CHROME;
    browser = await pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    const pbase = process.env.WIKI_BASE || `http://127.0.0.1:${servers.path.address().port}/`, qbase = `http://127.0.0.1:${servers.query.address().port}/`;
    const t0 = Date.now();
    for (const [w, scheme] of [[1280, 'light'], [1280, 'dark'], [390, 'light'], [390, 'dark']]) await pathRun(browser, pbase, w, scheme);
    if (!process.env.WIKI_BASE) await queryRun(browser, qbase);
    console.log(`nav74 ui: ok (${checks} checks, ${((Date.now() - t0) / 1000).toFixed(1)} s, shots in ${shots})`);
  } finally {
    if (browser) await browser.close();
    Object.values(servers).forEach(s => s.close());
  }
})().catch(e => { console.error(e); process.exit(1); });
