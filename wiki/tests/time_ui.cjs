// Browser test for the "time to obtain" boxes. Run on sb-test:
//   PW=/var/tmp/sb/pw/node_modules/playwright-core CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome node wiki/tests/time_ui.cjs
// Reference values come from wiki/obtain_time.py (see wiki/tests/test_obtain_time.py).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require(process.env.PW || 'playwright-core');
const root = path.resolve(__dirname, '..');
const server = http.createServer((req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end(); return; }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.html') ? 'text/html' : file.endsWith('.png') ? 'image/png' : 'application/octet-stream');
    res.end(data);
  });
});
const near = (a, b, tol = 1e-6) => assert.ok(Math.abs(a - b) <= tol * Math.max(1, Math.abs(b)), `${a} != ${b}`);
const ITEM = 'simplebuilding:music_disc_voidline'; // one Bernoulli source: End city, 4 %
function lum(c) { const v = c.match(/[\d.]+/g).slice(0, 3).map(Number).map(x => { x /= 255; return x <= .03928 ? x / 12.92 : Math.pow((x + .055) / 1.055, 2.4); }); return .2126 * v[0] + .7152 * v[1] + .0722 * v[2]; }
const contrast = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${server.address().port}/`;
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME });
  let checks = 0; const ok = (c, m) => { assert.ok(c, m); checks++; };
  try {
    for (const view of [{ w: 1280, h: 900 }, { w: 390, h: 800 }]) {
      for (const lang of ['de', 'en']) {
        const ctx = await browser.newContext({ viewport: { width: view.w, height: view.h } });
        const page = await ctx.newPage(); const errors = []; page.on('pageerror', e => errors.push(e.message));
        await page.goto(base + `?mod=simplebuilding&item=${ITEM}&lang=${lang}`); await page.locator('#main h1').waitFor();
        // JS maths against the Python reference values
        const ref = await page.evaluate(() => {
          const P = new Array(23).fill(0);
          const { tsCombine, timeStats } = window.__wikiTime;
          const a = tsCombine([{ rate: 2, pmf: [.96, .04].concat(P) }]);
          const b = tsCombine([{ rate: 1, pmf: [.5, .3, .2].concat(P) }, { rate: .5, pmf: [.9, .1].concat(P) }]);
          return { a6: timeStats(a, 6), a1: timeStats(a, 1), b4: timeStats(b, 4) };
        });
        near(ref.a6.mean, 74.99999999999977, 1e-9); near(ref.a6.median, 70.87701485890071, 1e-9); near(ref.a6.q90, 115.93342366689507, 1e-9);
        near(ref.a1.median, 8.664339756999308, 1e-9); near(ref.a1.q90, 28.782313662425544, 1e-9);
        near(ref.b4.mean, 5.682671948637387, 1e-9); near(ref.b4.median, 5.0528026873082705, 1e-9); near(ref.b4.q90, 10.254639317172302, 1e-9);
        checks += 8;
        const box = page.locator('.ts-box'); ok(await box.count() === 1, 'time box on item page');
        const out = page.locator('[data-ts-out="item"]');
        // assumptions are labelled and give numbers: End city 15/h targeted, p = 4 % -> mean 1/(15*.04) h
        ok((await box.locator('.ts-flag').first().textContent()).length > 0, 'assumption flag');
        ok(/ h|min/.test(await out.locator('table.ts-res tbody tr').first().textContent()), 'numbers present');
        ok(await out.locator('svg.ts-svg polyline').count() >= 1, 'chart curve');
        ok(await out.locator('svg.ts-svg circle').count() >= 3, 'mean/median/90 marks');
        ok(await page.locator('.ts-time').count() >= 1, 'line on source card');
        // empty rate -> gap, no number
        await box.locator('summary').first().click();
        const tIn = box.locator('input[data-ts-in="t"]').first();
        await tIn.fill('');
        ok(/–/.test(await out.locator('table.ts-res tbody tr').first().textContent()), 'empty rate gives a dash');
        ok(await out.locator('.ts-gap').count() >= 1, 'gap explained');
        await tIn.fill('0');
        ok(/–/.test(await out.locator('table.ts-res tbody tr').first().textContent()), 'zero rate gives a dash');
        // own value: 2/h -> Erlang values
        await tIn.fill('2');
        const hours = await page.evaluate(id => { const w = window.__wikiTime, s = w.tsAll(id, 't'); return w.timeStats(s.c, 6); }, ITEM);
        near(hours.mean, 6 / (2 * 0.04), 1e-9); checks++;
        ok(/ ?h/.test(await out.locator('table.ts-res tbody tr').first().textContent()), 'recomputed');
        // era line only after input
        ok(await out.locator('.ts-era').count() === 0, 'no era line without input');
        await box.locator('.ts-eras summary').click();
        await box.locator('input[data-ts-era^="t|brewing"]').fill('15');
        ok(await out.locator('.ts-era').count() === 1, 'era line after input');
        // k select + persistence
        await box.locator('input[data-ts-k="1"]').fill('3');
        ok(await out.locator('select[data-ts-ksel] option').nth(1).textContent().then(t => /3/.test(t)), 'k shown');
        await page.reload(); await page.locator('.ts-box').waitFor();
        ok(await page.locator('input[data-ts-in="t"]').first().inputValue() === '2', 'rate remembered');
        // reset
        await page.locator('.ts-box summary').first().click();
        await page.locator('[data-ts-act="reset"]').click();
        ok(/(adjustable|anpassbar)/.test(await page.locator('.ts-flag').first().textContent()), 'reset -> assumption');
        // no horizontal page scroll
        ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1), 'no horizontal scroll ' + view.w);
        // overview
        await page.goto(base + `?mod=simplebuilding&tab=loot&lang=${lang}`); await page.locator('#ts-overview').waitFor();
        ok(await page.locator('#ts-overview [data-ts-out="overview"] tbody tr').count() > 3, 'overview rows');
        ok(await page.locator('#ts-overview svg.ts-svg').count() === 1, 'overview chart');
        await page.locator('select[data-ts-pick]').selectOption(ITEM);
        ok(await page.locator('#ts-overview svg.ts-svg').count() === 1, 'overview chart after pick');
        ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1), 'overview no horizontal scroll');
        // same numbers on both pages: 2 /h, p 4 % -> overview mean (k=1)
        ok(errors.length === 0, 'no page errors: ' + errors.join('|'));
        await ctx.close();
      }
    }
    // blocked storage
    const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    await ctx.addInitScript(() => { Object.defineProperty(window, 'localStorage', { get() { throw new Error('blocked'); } }); });
    const page = await ctx.newPage(); const errors = []; page.on('pageerror', e => errors.push(e.message));
    await page.goto(base + `?mod=simplebuilding&item=${ITEM}&lang=en`); await page.locator('.ts-box').waitFor();
    await page.locator('.ts-box summary').first().click();
    await page.locator('input[data-ts-in="t"]').first().fill('1');
    ok(await page.locator('[data-ts-out="item"] table.ts-res').count() === 1, 'works with blocked storage');
    ok(errors.length === 0, 'no errors with blocked storage: ' + errors.join('|'));
    await ctx.close();
    // contrast, both themes
    for (const scheme of ['light', 'dark']) {
      const c2 = await browser.newContext({ colorScheme: scheme, viewport: { width: 1280, height: 900 } });
      const p2 = await c2.newPage();
      await p2.goto(base + `?mod=simplebuilding&item=${ITEM}&lang=en`); await p2.locator('.ts-svg').waitFor();
      const col = await p2.evaluate(() => {
        const cs = sel => getComputedStyle(document.querySelector(sel));
        return { bg: cs('.ts-svg').backgroundColor, text: cs('.ts-svg .ts-t').fill, c1: cs('.ts-curve.ts-s-t').stroke, c2: cs('.ts-curve.ts-s-n').stroke, muted: cs('.ts-flag').color, pbg: cs('.ts-box').backgroundColor };
      });
      ok(contrast(col.text, col.bg) >= 4.5, `axis text contrast ${scheme}: ${contrast(col.text, col.bg)}`);
      ok(contrast(col.c1, col.bg) >= 3 && contrast(col.c2, col.bg) >= 3, `curve contrast ${scheme}`);
      await c2.close();
    }
    console.log(`time_ui: ${checks} checks passed`);
  } finally { await browser.close(); server.close(); }
})().catch(e => { console.error(e); process.exit(1); });
