// N72 browser test (docs/ai/PLAN-N72-TZEDITOR-2026-11-16.md): the shared zoom / full screen bar (assets/zoomarea.js) on
// the test centre editor grid, the texture list, the recipe lists; and the pictures of the editor cells.
// Playwright or playwright-core in NODE_PATH, no Minecraft runtime. Runs on sb-test:
//   NODE_PATH=/var/tmp/sb/pw/node_modules WIKI_CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   ZM_SHOTS=/var/tmp/sb/n72 node wiki/tests/zoomarea_ui.cjs
// WIKI_BASE=https://wiki.simplemods.de/dev/ (+ WIKI_COOKIE = sb_session) runs the same clicks against the dev instance.
// TZ_EXPECT_ALL=1: every id cell of the editor must show a picture (after wiki/vanilla_textures.py, as on the dev host).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const root = path.resolve(__dirname, '..');
const shots = process.env.ZM_SHOTS || os.tmpdir();
const TYPES = { '.js': 'text/javascript', '.html': 'text/html', '.json': 'application/json', '.png': 'image/png', '.css': 'text/css' };
const server = http.createServer((req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end(); return; }
    res.setHeader('Content-Type', TYPES[path.extname(file)] || 'application/octet-stream');
    res.end(data);
  });
});
let checks = 0;
const ok = (cond, msg) => { assert.ok(cond, msg); checks++; };
const log = (...a) => console.log(...a);

async function zoomOf(page, key) {
  return page.evaluate(k => { const a = window.SBZoom.get(k); return a ? a.zoom() : null; }, key);
}
// wheel with or without Ctrl over a point; returns whether the page saw the event as handled (preventDefault)
async function wheel(page, x, y, dy, ctrl) {
  await page.evaluate(() => { window.__wp = null; window.addEventListener('wheel', e => { window.__wp = e.defaultPrevented; }, { once: true }); });
  await page.mouse.move(x, y);
  if (ctrl) await page.keyboard.down('Control');
  await page.mouse.wheel(0, dy);
  if (ctrl) await page.keyboard.up('Control');
  await page.waitForTimeout(150);
  return page.evaluate(() => window.__wp);
}

async function checkArea(page, key, name) {
  const zm = page.locator(`.zm[data-zm="${key}"]`).first();
  await zm.waitFor({ timeout: 30000 });
  const bar = zm.locator('.zm-bar');
  ok(await bar.locator('.zm-in').isVisible() && await bar.locator('.zm-out').isVisible() && await bar.locator('.zm-full').isVisible(), name + ': bar with -, +, full screen');
  await bar.locator('[data-zm-act=reset]').click();
  ok(Math.abs(await zoomOf(page, key) - 1) < 1e-6, name + ': reset = 100 %');
  await bar.locator('.zm-in').click();
  const z1 = await zoomOf(page, key);
  ok(z1 > 1 && (await bar.locator('.zm-lvl').textContent()).trim() === Math.round(z1 * 100) + ' %', name + ': plus zooms in, level shown');
  await bar.locator('.zm-out').click(); await bar.locator('.zm-out').click();
  ok(await zoomOf(page, key) < 1, name + ': minus zooms out');
  await bar.locator('[data-zm-act=reset]').click();
  // Ctrl + wheel over the area: zooms the area, the browser does not get the event (no page zoom)
  const box = await zm.locator('.zm-view').boundingBox();
  const cx = box.x + Math.min(box.width / 2, 200), cy = box.y + Math.min(box.height / 2, 200);
  const prevented = await wheel(page, cx, cy, -100, true);
  ok(prevented === true && await zoomOf(page, key) > 1, name + ': Ctrl+wheel over the area zooms it (preventDefault there)');
  const dpr = await page.evaluate(() => window.devicePixelRatio);
  ok(dpr === 1, name + ': page zoom untouched');
  // Ctrl + wheel outside: not handled by the area
  const before = await zoomOf(page, key);
  const outside = await wheel(page, 5, 5, -100, true);
  ok(outside !== true && await zoomOf(page, key) === before, name + ': Ctrl+wheel outside leaves area and browser alone');
  // plain wheel: no zoom
  await wheel(page, cx, cy, 100, false);
  ok(await zoomOf(page, key) === before, name + ': plain wheel does not zoom');
  // full screen, zoom kept, Esc back, zoom kept
  await bar.locator('.zm-full').click();
  const vp = page.viewportSize();
  const fb = await zm.boundingBox();
  ok(await zm.evaluate(e => e.classList.contains('zm-on')) && Math.abs(fb.width - vp.width) < 2 && Math.abs(fb.height - vp.height) < 2, name + ': full screen fills the window');
  ok(await page.evaluate(() => document.body.classList.contains('zm-lock')), name + ': page behind does not scroll');
  ok(await zoomOf(page, key) === before, name + ': zoom stays when entering full screen');
  await page.screenshot({ path: path.join(shots, `zm-${name}-full.png`) });
  await bar.locator('.zm-in').click();
  const inFull = await zoomOf(page, key);
  await page.keyboard.press('Escape');
  ok(!(await zm.evaluate(e => e.classList.contains('zm-on'))) && !(await page.evaluate(() => document.body.classList.contains('zm-lock'))), name + ': Esc leaves full screen');
  ok(await zoomOf(page, key) === inFull, name + ': zoom stays when leaving full screen');
  await bar.locator('.zm-full').click();
  ok((await bar.locator('.zm-full').textContent()).trim().length > 0 && await zm.evaluate(e => e.classList.contains('zm-on')), name + ': full screen again');
  await bar.locator('.zm-full').click();
  ok(!(await zm.evaluate(e => e.classList.contains('zm-on'))), name + ': back button leaves full screen');
  await bar.locator('[data-zm-act=reset]').click();
}

(async () => {
  let base = process.env.WIKI_BASE;
  if (!base) { await new Promise(r => server.listen(0, '127.0.0.1', r)); base = `http://127.0.0.1:${server.address().port}/`; }
  const exe = process.env.WIKI_CHROME;
  const browser = await pw.chromium.launch(exe ? { executablePath: exe.replace(/^~/, os.homedir()) } : { channel: process.env.WIKI_BROWSER || 'chromium' });
  for (const [scheme, lang] of [['light', 'de'], ['dark', 'en']]) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: scheme });
    if (process.env.WIKI_COOKIE) await ctx.addCookies([{ name: 'sb_session', value: process.env.WIKI_COOKIE, url: base }]);
    const page = await ctx.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    const q = base.startsWith('http://127.0.0.1') ? '&dev=1' : '';

    // 1. editor grid
    await page.goto(base + `index.html?mod=simplebuilding&tab=tzeditor&lang=${lang}${q}`);
    await page.locator('.tz-grid .tz-c').first().waitFor({ timeout: 60000 });
    await checkArea(page, 'tzeditor', 'tzeditor-' + scheme);
    // cell size follows the zoom (sharp pixels instead of scaling), slider in sync
    await page.locator('.zm[data-zm=tzeditor] .zm-in').click();
    const z = await page.evaluate(() => [getComputedStyle(document.querySelector('.tz-page')).getPropertyValue('--z').trim(), document.querySelector('[data-k=zoom]').value]);
    ok(z[0] === '40px' && z[1] === '40', 'tzeditor: zoom 110 % = 40 px cells, slider 40 (' + z + ')');
    await page.locator('[data-k=zoom]').fill('72');
    ok(Math.abs(await zoomOf(page, 'tzeditor') - 2) < 1e-6, 'tzeditor: slider drives the shared zoom (72 px = 200 %)');
    // area switch redraws the editor: zoom and full screen stay
    await page.locator('.zm[data-zm=tzeditor] .zm-full').click();
    const areas = await page.locator('select[data-k=area] option').count();
    if (areas > 1) {
      await page.evaluate(() => { const s = document.querySelector('select[data-k=area]'); s.selectedIndex = (s.selectedIndex + 1) % s.options.length; s.dispatchEvent(new Event('change', { bubbles: true })); });
      await page.waitForTimeout(1500);
      ok(await page.locator('.zm[data-zm=tzeditor]').evaluate(e => e.classList.contains('zm-on')) && Math.abs(await zoomOf(page, 'tzeditor') - 2) < 1e-6, 'tzeditor: full screen and zoom survive an area switch');
      await page.evaluate(() => { const s = document.querySelector('select[data-k=area]'); s.selectedIndex = 0; s.dispatchEvent(new Event('change', { bubbles: true })); });
      await page.waitForTimeout(1500);
    }
    await page.keyboard.press('Escape');
    await page.locator('.zm[data-zm=tzeditor] [data-zm-act=reset]').click();
    // pictures: scroll the whole grid once (lazy pictures), then count
    await page.evaluate(async () => { const v = document.querySelector('.tz-main'); for (let y = 0; y < v.scrollHeight; y += 400) { for (let x = 0; x < v.scrollWidth; x += 800) { v.scrollTo(x, y); await new Promise(r => setTimeout(r, 30)); } } v.scrollTo(0, 0); });
    await page.waitForTimeout(3000);
    const pics = await page.evaluate(() => {
      const cells = [...document.querySelectorAll('.tz-grid .tz-c')].filter(c => c.dataset.id && c.dataset.id[0] !== '#');
      const r = { cells: cells.length, pictures: 0, names: 0, questionMarks: 0, missing: [] };
      cells.forEach(c => {
        const i = c.querySelector('img:not(.tz-t)');
        if (i && i.complete && i.naturalWidth > 0) r.pictures++;
        else { r.names++; if (r.missing.length < 40) r.missing.push(c.dataset.id); }
        if (/^\s*\?(\s*\?)?\s*$/.test(c.textContent)) r.questionMarks++;
      });
      return r;
    });
    log(scheme, 'tzeditor pictures', JSON.stringify(pics));
    ok(pics.questionMarks === 0, 'tzeditor: no "?" cells (' + pics.questionMarks + ')');
    if (process.env.TZ_EXPECT_ALL) ok(pics.names === 0, 'tzeditor: every id cell has a picture, missing: ' + pics.missing.join(' '));
    await page.screenshot({ path: path.join(shots, `zm-tzeditor-${scheme}.png`) });

    // 2. texture list, 3. all recipes, 4. recipes page
    for (const [tab, key] of [['textures', 'textures'], ['allrecipes', 'allrecipes'], ['recipes', 'recipes']]) {
      await page.goto(base + `index.html?mod=simplebuilding&tab=${tab}&lang=${lang}${q}`);
      await checkArea(page, key, key + '-' + scheme);
      await page.screenshot({ path: path.join(shots, `zm-${key}-${scheme}.png`) });
    }
    ok(!errors.length, scheme + ': no script errors ' + errors.join(' | '));
    await ctx.close();
  }
  await browser.close();
  server.close();
  console.log(`zoomarea ui ok (${checks} checks)`);
})().catch(e => { console.error(e); process.exit(1); });
