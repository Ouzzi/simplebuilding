// Browser test of the wiki start and module pages (S-B + M-1, docs/ai/PLAN-A-WIKI24-2026-10-23.md). Playwright (or
// playwright-core) in NODE_PATH, no Minecraft runtime. Runs on sb-test:
//   NODE_PATH=/var/tmp/sb/pw/node_modules WIKI_CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   HOME_SHOTS=/var/tmp/sb/pw node wiki/tests/home_ui.cjs
// SimpleBuilding, Simple Blocks and Simple Weather at 1280 and 400 px, light and dark: head with chips and three ways,
// highlights only with a loaded picture (at most 6), categories only on SimpleBuilding, compatibility chips, no
// horizontal scrolling, text contrast >= 4.5:1, no script errors, the ways lead somewhere.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const root = path.resolve(__dirname, '..');
const shots = process.env.HOME_SHOTS || os.tmpdir();
const TYPES = { '.js': 'text/javascript', '.html': 'text/html', '.json': 'application/json', '.png': 'image/png' };
const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const file = path.resolve(root, '.' + decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end(); return; }
    res.setHeader('Content-Type', TYPES[path.extname(file)] || 'application/octet-stream');
    res.end(data);
  });
});
// text contrast against the first opaque background behind the element
const CONTRAST = el => {
  const rgb = s => (s.match(/[\d.]+/g) || []).map(Number);
  const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
  let fg = rgb(getComputedStyle(el).color), n = el, bg = null;
  while (n && n.nodeType === 1) { const c = rgb(getComputedStyle(n).backgroundColor); if (c.length === 3 || (c.length === 4 && c[3] > 0.95)) { bg = c; break; } n = n.parentElement; }
  bg = bg || rgb(getComputedStyle(document.body).backgroundColor);
  const a = lum(fg), b = lum(bg);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
};
const TEXT = ['.home-hero h1', '.home-hero .lead', '.home-hero .badge', '.way b', '.way small', '.home-search .grow', '.home-hl .itile-name',
  '.cat-card .cd', '.guide span', '.tile .lbl', '.compat-l', '.chip-req', '.chip-opt'];

async function check(browser, base, mod, width, scheme) {
  const ctx = await browser.newContext({ viewport: { width, height: 900 }, colorScheme: scheme });
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  await page.goto(base + '?mod=' + mod + '&lang=' + (scheme === 'dark' ? 'de' : 'en') + '#/');
  await page.locator('#main .home-hero h1').waitFor();
  await page.waitForLoadState('networkidle');
  const tag = `${mod} ${width} ${scheme}`;
  assert.match(await page.locator('#main h1').first().textContent(), /Wiki$/, tag);
  const ways = await page.locator('.home-ways .way').count();
  assert.ok(ways >= 2 && ways <= 3, tag + ' ways ' + ways);
  assert.equal(await page.locator('.home-head img.home-icon').count(), 1, tag + ' module icon');
  // highlights: at most 6 visible, every visible one with a loaded picture
  const hl = await page.$$eval('.home-hl .itile', ts => ts.filter(t => t.offsetParent !== null).map(t => { const i = t.querySelector('img'); return i ? i.naturalWidth : 0; }));
  assert.ok(hl.length <= 6, tag + ' highlights ' + hl.length);
  assert.ok(hl.every(w => w > 0), tag + ' highlight without picture ' + JSON.stringify(hl));
  if (mod !== 'simpleweather') assert.ok(hl.length >= 1, tag + ' highlights shown');
  assert.equal(await page.locator('.cat-grid').count(), mod === 'simplebuilding' ? 1 : 0, tag + ' categories');
  assert.equal(await page.locator('.home-compat .compat-row').count(), 2, tag + ' compatibility');
  if (mod === 'simpleblocks') assert.ok(await page.locator('.home-hero .badge').filter({ hasText: /works alone|läuft allein/ }).count() === 1, tag + ' works alone');
  const over = await page.evaluate(() => ({ doc: document.documentElement.scrollWidth, w: window.innerWidth }));
  assert.ok(over.doc <= over.w, tag + ' horizontal scroll ' + JSON.stringify(over));
  for (const sel of TEXT) {
    const els = page.locator(sel);
    if (!(await els.count())) continue;
    const c = await els.first().evaluate(CONTRAST);
    assert.ok(c >= 4.5, `${tag} ${sel} contrast ${c.toFixed(2)}`);
  }
  // touch targets of the ways on the phone
  if (width < 500) {
    const small = await page.$$eval('.home-ways .way', ws => ws.map(w => w.getBoundingClientRect().height).filter(h => h < 44));
    assert.deepEqual(small, [], tag + ' way height');
  }
  await page.screenshot({ path: path.join(shots, `home-${mod}-${width}-${scheme}.png`), fullPage: false });
  if (mod === 'simplebuilding') {
    const more = page.locator('.compat-more > summary');
    if (await more.count()) {
      const before = await page.locator('.home-compat .chip-opt').filter({ visible: true }).count();
      await more.click();
      assert.ok(await page.locator('.home-compat .chip-opt').filter({ visible: true }).count() > before, tag + ' +N more opens');
    }
    await page.locator('.home-ways .way').first().click();
    await page.waitForFunction(() => location.hash === '#/features');
  }
  assert.deepEqual(errors, [], tag);
  await ctx.close();
}

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    const exe = process.env.WIKI_CHROME;
    browser = await pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    const base = `http://127.0.0.1:${server.address().port}/`;
    let n = 0;
    for (const mod of ['simplebuilding', 'simpleblocks', 'simpleweather']) {
      for (const [width, scheme] of [[1280, 'light'], [400, 'dark'], [400, 'light'], [1280, 'dark']]) { await check(browser, base, mod, width, scheme); n++; }
    }
    console.log('home ui: ok (' + n + ' views, shots in ' + shots + ')');
  } finally {
    if (browser) await browser.close();
    server.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
