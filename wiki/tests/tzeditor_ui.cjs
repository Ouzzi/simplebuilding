// N72b browser test of the test centre editor (docs/ai/PLAN-N72-TZEDITOR-2026-11-16.md): own cell menu (mouse and
// keyboard) instead of the wiki menu, change block with search + preview, missing / does not exist / empty / frame,
// insert row and column, undo over everything, "All areas" side by side, "As built" (line arrangement of the game) and
// the compact check from the game plan. Playwright or playwright-core in NODE_PATH, no Minecraft runtime. On sb-test:
//   NODE_PATH=/var/tmp/sb/pw/node_modules WIKI_CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   TZ_SHOTS=/var/tmp/sb/n72 node wiki/tests/tzeditor_ui.cjs
// WIKI_BASE=https://wiki.simplemods.de/dev/ (+ WIKI_COOKIE = sb_session) runs it against the dev instance (nothing is saved).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const root = path.resolve(__dirname, '..');
const shots = process.env.TZ_SHOTS || os.tmpdir();
const TYPES = { '.js': 'text/javascript', '.html': 'text/html', '.json': 'application/json', '.png': 'image/png' };
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
const cellSel = (r, c) => `.tz-body .tz-c[data-r="${r}"][data-c="${c}"]`;
const cellId = (page, r, c) => page.locator(cellSel(r, c)).first().getAttribute('data-id');

(async () => {
  let base = process.env.WIKI_BASE;
  if (!base) { await new Promise(r => server.listen(0, '127.0.0.1', r)); base = `http://127.0.0.1:${server.address().port}/`; }
  const exe = process.env.WIKI_CHROME;
  const browser = await pw.chromium.launch(exe ? { executablePath: exe.replace(/^~/, os.homedir()) } : { channel: process.env.WIKI_BROWSER || 'chromium' });
  for (const [scheme, lang] of [['dark', 'de'], ['light', 'en']]) {
    const ctx = await browser.newContext({ viewport: { width: 1500, height: 950 }, colorScheme: scheme });
    if (process.env.WIKI_COOKIE) await ctx.addCookies([{ name: 'sb_session', value: process.env.WIKI_COOKIE, url: base }]);
    const page = await ctx.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    page.on('dialog', d => d.accept(d.type() === 'prompt' ? (d.message().match(/Spalte|column/i) ? 'probe col' : 'Probe Reihe') : undefined));
    const q = base.startsWith('http://127.0.0.1') ? '&dev=1' : '';
    await page.goto(base + `index.html?mod=simplebuilding&tab=tzeditor&lang=${lang}${q}`);
    await page.locator('.tz-body .tz-c').first().waitFor({ timeout: 60000 });
    const T = await page.evaluate(() => window.SBTzEditor.STR[document.documentElement.lang === 'de' ? 'de' : 'en']);
    const L = lang === 'de' ? await page.evaluate(() => window.SBTzEditor.STR.de) : await page.evaluate(() => window.SBTzEditor.STR.en);

    // 1. right-click: the editor menu, not the wiki menu
    await page.locator(cellSel(0, 1)).first().click({ button: 'right' });
    const menu = page.locator('.tz-menu');
    ok(await menu.isVisible(), 'right-click opens the editor menu');
    ok(!(await page.locator('.ctxmenu').isVisible().catch(() => false)), 'the wiki menu stays closed');
    for (const k of ['mChange', 'mMissing', 'mAbsent', 'mClear', 'mFrame', 'mRow', 'mCol', 'mUndo']) ok(await menu.getByText(L[k], { exact: false }).count() === 1, 'menu item ' + k);
    await page.screenshot({ path: path.join(shots, `tz-menu-${scheme}.png`) });
    const before = await cellId(page, 0, 1);
    await menu.locator('[data-m=missing]').click();
    ok(await cellId(page, 0, 1) === '#red', 'mark as missing = #red');
    await page.locator(cellSel(0, 1)).first().click({ button: 'right' });
    await menu.locator('[data-m=absent]').click();
    ok(await cellId(page, 0, 1) === '#light', 'does not exist = #light');
    await page.locator(cellSel(0, 1)).first().click({ button: 'right' });
    await menu.locator('[data-m=frame]').click();
    ok(await cellId(page, 0, 1) === '#frame', 'frame on');
    await page.locator(cellSel(0, 1)).first().click({ button: 'right' });
    await menu.locator('[data-m=clear]').click();
    ok(await cellId(page, 0, 1) === '', 'empty');
    for (let i = 0; i < 4; i++) await page.locator('[data-act=undo]').click();
    ok(await cellId(page, 0, 1) === before, 'undo brings the block back');

    // 2. change block: search with preview, Enter sets the hit; any id works
    await page.locator(cellSel(0, 2)).first().click({ button: 'right' });
    await menu.locator('[data-m=change]').click();
    const dlg = page.locator('.tz-dlg');
    ok(await dlg.isVisible(), 'change dialog opens');
    await dlg.locator('input').fill('stone_bricks');
    await page.waitForTimeout(200);
    ok(await dlg.locator('li').count() > 0 && await dlg.locator('.tz-pv .tz-c').count() === 1, 'search hits with preview');
    await page.screenshot({ path: path.join(shots, `tz-change-${scheme}.png`) });
    const hit = await dlg.locator('li.on').getAttribute('data-id');
    await dlg.locator('input').press('Enter');
    ok(await cellId(page, 0, 2) === hit, 'Enter sets the marked hit ' + hit);
    await page.locator(cellSel(0, 2)).first().click({ button: 'right' });
    await menu.locator('[data-m=change]').click();
    await dlg.locator('input').fill('somemod:brand_new_block');
    await dlg.locator('[data-d=set]').click();
    ok(await cellId(page, 0, 2) === 'somemod:brand_new_block', 'any id can be set');

    // 3. insert row / column, undo over all of it
    const rows0 = await page.locator('.tz-body .tz-r').count(), cols0 = await page.locator('.tz-body .tz-h').count();
    await page.locator(cellSel(1, 0)).first().click({ button: 'right' });
    await menu.locator('[data-m=row]').click();
    ok(await page.locator('.tz-body .tz-r').count() === rows0 + 1 && (await page.locator('.tz-body .tz-r').nth(1).textContent()) === 'Probe Reihe', 'row inserted at the cell');
    await page.locator(cellSel(0, 3)).first().click({ button: 'right' });
    await menu.locator('[data-m=col]').click();
    ok(await page.locator('.tz-body .tz-h').count() === cols0 + 1 && (await page.locator('.tz-body .tz-h').nth(3).textContent()) === 'probe_col', 'column inserted at the cell');
    for (let i = 0; i < 4; i++) await page.keyboard.press('Control+z');
    ok(await page.locator('.tz-body .tz-r').count() === rows0 && await page.locator('.tz-body .tz-h').count() === cols0 && await cellId(page, 0, 2) !== 'somemod:brand_new_block', 'Ctrl+Z undoes inserts and changes');

    // 4. keyboard only: focus the grid, arrows, Shift+F10, arrow down, Enter
    await page.locator('.tz-main').focus();
    await page.keyboard.press('ArrowRight'); await page.keyboard.press('ArrowRight');
    ok(await page.locator('.tz-body .tz-c.sel').count() === 1, 'arrows select a cell');
    await page.keyboard.press('Shift+F10');
    ok(await menu.isVisible() && await page.evaluate(() => document.activeElement && document.activeElement.dataset.m === 'change'), 'Shift+F10 opens the menu with focus');
    await page.keyboard.press('ArrowDown');
    const selCell = await page.locator('.tz-body .tz-c.sel').first();
    const r = await selCell.getAttribute('data-r'), c = await selCell.getAttribute('data-c');
    await page.keyboard.press('Enter');
    ok(await cellId(page, r, c) === '#red', 'keyboard: menu item via arrow + Enter');
    await page.keyboard.press('Shift+F10'); await page.keyboard.press('Escape');
    ok(!(await menu.isVisible().catch(() => false)), 'Esc closes the menu');
    await page.keyboard.press('Control+z');

    // 5. All areas: every area side by side, editable
    await page.selectOption('select[data-k=area]', '*all');
    await page.locator('.tz-all .tz-area').first().waitFor({ timeout: 60000 });
    const areas = await page.locator('select[data-k=area] option').count() - 1;
    ok(await page.locator('.tz-all .tz-area').count() === areas, 'all ' + areas + ' areas side by side');
    ok(await page.locator('.zm[data-zm=tzeditor]').count() === 1, 'overview inside the zoom/full screen component');
    const last = page.locator('.tz-all .tz-area').last().locator('.tz-c[data-r="0"][data-c="0"]');
    await last.click({ button: 'right' });
    await menu.locator('[data-m=missing]').click();
    ok(await page.locator('.tz-all .tz-area').last().locator('.tz-c[data-r="0"][data-c="0"]').getAttribute('data-id') === '#red', 'a table of the overview is editable');
    ok(/\*/.test(await page.locator('.tz-all .tz-area').last().locator('h3').textContent()), 'changed area marked unsaved');
    await page.locator('.zm[data-zm=tzeditor] .zm-full').click();
    await page.locator('.zm[data-zm=tzeditor] [data-zm-act=out]').click(); await page.locator('.zm[data-zm=tzeditor] [data-zm-act=out]').click();
    await page.screenshot({ path: path.join(shots, `tz-all-${scheme}.png`) });
    await page.keyboard.press('Escape');
    await page.locator('[data-act=undo]').click();

    // 6. As built: line arrangement of the game
    await page.selectOption('select[data-k=layout]', 'built');
    await page.locator('.tz-bg').first().waitFor({ timeout: 60000 });
    ok(await page.locator('.tz-line').count() >= 1 && await page.locator('.tz-side-l').count() >= 2, 'as built: grey line and sides A/B');
    const signs = await page.locator('.tz-sign').allTextContents();
    ok(signs.length > 100, 'as built: a sign per row (' + signs.length + ')');
    const plan = await page.evaluate(() => fetch('data/testcentre/plan.json').then(r => r.ok ? r.json() : null));
    if (plan) {
      const head = await page.locator('.tz-bandh').first().textContent();
      const a = Object.values(plan.lines.side).filter(v => v[0] === 'A').length, b = Object.values(plan.lines.side).filter(v => v[0] === 'B').length;
      ok(head.includes(' ' + a) && head.includes(' ' + b), `as built: side widths of the game plan (${a}/${b}): ${head}`);
      ok(await page.locator('.tz-cmp').count() === 1, 'compact check from the game plan in the side panel');
    }
    const builtCell = page.locator('.tz-bg .tz-c[data-f]:not(.tz-fill)').first();
    await builtCell.click({ button: 'right' });
    ok(await menu.isVisible(), 'as built: cells have the menu too');
    await page.keyboard.press('Escape');
    await page.screenshot({ path: path.join(shots, `tz-built-${scheme}.png`) });
    await page.locator('.zm[data-zm=tzeditor] .zm-full').click();
    await page.screenshot({ path: path.join(shots, `tz-built-full-${scheme}.png`) });
    await page.keyboard.press('Escape');
    await page.locator('.tz-side').screenshot({ path: path.join(shots, `tz-side-${scheme}.png`) });
    ok(!errors.length, scheme + ': no script errors ' + errors.join(' | '));
    await ctx.close();
  }
  await browser.close();
  server.close();
  console.log(`tzeditor ui ok (${checks} checks)`);
})().catch(e => { console.error(e); process.exit(1); });
