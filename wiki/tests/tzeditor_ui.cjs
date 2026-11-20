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
const N99 = require('./tzeditor_n99.cjs');
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
    const miss = {}, asked = [];  // N110: picture requests that came back 404 (url -> count), all requested urls
    page.on('request', r => asked.push(r.url()));
    page.on('response', r => { if (r.status() === 404) miss[r.url()] = (miss[r.url()] || 0) + 1; });
    page.on('dialog', d => d.accept(d.type() === 'prompt' ? (d.message().match(/Spalte|column/i) ? 'probe col' : 'Probe Reihe') : undefined));
    const q = base.startsWith('http://127.0.0.1') ? '&dev=1' : '';
    await page.goto(base + `index.html?mod=simplebuilding&tab=tzeditor&lang=${lang}${q}`);
    await page.locator('.tz-bg .tz-c').first().waitFor({ timeout: 60000 });
    // N85 (owner 17.11.): the editor opens "as built" (the owner's line arrangement); steps 1-5 work on the table
    ok(await page.locator('select[data-k=layout]').inputValue() === 'built', 'opens in the view "as built"');
    await page.selectOption('select[data-k=layout]', 'table');
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
    // 6b. N110 (owner 19.11.: "fehlende Texturen ... Fallback-Icon + Liste 'ohne Bild'"): no broken image anywhere, every
    // cell without picture shows the symbol, the side list names them per area and jumps to the cell; an id nothing knows
    // asks for no file at all
    await page.waitForTimeout(1500);
    const npInfo = () => page.evaluate(() => ({
      np: document.querySelectorAll('.tz-body .tz-np').length,
      broken: [...document.querySelectorAll('.tz-body img, .tz-pool img')].filter(i => i.complete && !i.naturalWidth).length,
      head: (document.querySelector('.tz-nopic h3') || {}).textContent || '',
      buttons: [...document.querySelectorAll('.tz-nopic [data-npj]')].map(b => b.textContent.replace(/ ×\d+$/, '')),
      symbols: [...new Set([...document.querySelectorAll('.tz-body .tz-np, .tz-pool .tz-np')].map(n => n.dataset.np))] }));
    let ni = await npInfo();
    ok(ni.broken === 0, 'N110: no broken image in the grid (' + ni.broken + ')');
    ok(ni.head.startsWith(L.noPic), 'N110: list "' + L.noPic + '" in the side panel: ' + ni.head);
    ok(ni.symbols.every(id => ni.buttons.includes(id)), 'N110: every id with the symbol is in the list (' + ni.symbols.length + ')');
    const ghost = 'simplebuilding:n110_no_such_block';
    const gCell = page.locator('.tz-bg .tz-c[data-f]:not(.tz-fill):not(.tz-ro)').nth(3);
    const gKey = await gCell.evaluate(e => [e.dataset.f, e.dataset.r, e.dataset.c]);
    const gSel = `.tz-body .tz-c[data-f="${gKey[0]}"][data-r="${gKey[1]}"][data-c="${gKey[2]}"]`;
    await gCell.click();
    await page.locator('.tz-unk').fill(ghost);
    await page.locator('[data-act=unk]').click();
    await page.waitForFunction(id => [...document.querySelectorAll('.tz-nopic [data-npj]')].some(b => b.textContent.startsWith(id)), ghost, { timeout: 10000 });
    ok(await page.locator(gSel + ' > .tz-np').count() === 1 && await page.locator(gSel + ' > img').count() === 0, 'N110: an id without picture shows the symbol, no image');
    ok(await page.locator(gSel + ' > .tz-np').getAttribute('aria-label') === L.noPicCell.replace('{0}', ghost), 'N110: symbol says "' + L.noPicCell.replace('{0}', ghost) + '"');
    ok(!asked.some(u => u.includes('n110_no_such_block')), 'N110: no request for an id nothing knows');
    await page.locator('.tz-main').evaluate(m => { m.scrollTop = 0; m.scrollLeft = 0; });
    await page.locator('.tz-nopic [data-npj]', { hasText: ghost }).click();
    await page.waitForTimeout(400);
    ok(await page.locator(gSel + '.tz-flash.sel').count() === 1, 'N110: a click in the list selects and flashes the cell');
    ok(await page.locator(gSel).evaluate(e => { const r = e.getBoundingClientRect(), m = e.closest('.tz-main').getBoundingClientRect(); return r.top >= m.top && r.bottom <= m.bottom && r.left >= m.left && r.right <= m.right; }), 'N110: the cell is scrolled into view');
    await page.locator('.tz-side').screenshot({ path: path.join(shots, `n110-ohne-bild-${scheme}.png`) });
    await page.locator('[data-act=undo]').click();
    await page.waitForFunction(id => ![...document.querySelectorAll('.tz-nopic [data-npj]')].some(b => b.textContent.startsWith(id)), ghost, { timeout: 10000 });
    ok(true, 'N110: after undo the id is gone from the list');
    const twice = Object.entries(miss).filter(([u, n]) => n > 1 && /\.png($|\?)/.test(u));
    ok(!twice.length, 'N110: no picture asked for twice after a 404 ' + twice.slice(0, 3).map(x => x.join(' x')).join(', '));

    // 7. N99: timings, group move with live preview, swap + Esc, rows/columns, undo/redo, copy/paste, frame, range, arrows
    if (scheme === 'dark') {
      const r = await N99.run(page, { ok, shot: n => page.screenshot({ path: path.join(shots, n + '.png') }), log: (...a) => console.log(...a) });
      ok(r.timings.allWorst < 100, 'all areas: click/mark until the next frame ' + r.timings.allWorst + ' ms');
      // saved - not online yet: a save (mocked API) is shown after reload with its mark; "online state" shows the repo again
      let saved = null;
      await page.route('**/api/testcentre**', route => {
        const u = new URL(route.request().url()), m = route.request().method();
        if (m === 'POST') { saved = Object.assign(JSON.parse(route.request().postData()), { sid: '1999000000001', createdAt: Date.now(), by: 'test@local', state: 'new' }); return route.fulfill({ status: 201, json: { sid: saved.sid, createdAt: saved.createdAt, by: saved.by } }); }
        if (u.pathname.endsWith('/api/testcentre')) return route.fulfill({ json: saved ? [Object.assign({}, saved, { rows: undefined, rowCount: saved.rows.length })] : [] });
        return route.fulfill(saved ? { json: saved } : { status: 404, json: {} });
      });
      const f = await page.locator('select[data-k=area] option').nth(3).getAttribute('value');
      await page.selectOption('select[data-k=area]', f);
      await page.waitForFunction(() => !document.querySelector('.tz-all'));
      await page.locator(cellSel(0, 0)).waitFor();
      const orig = await cellId(page, 0, 0);
      await page.locator(cellSel(0, 0)).click({ button: 'right' });
      await page.locator('.tz-menu [data-m=missing]').click();
      await page.locator('[data-act=save]').click();
      await page.waitForFunction(() => /Nr\.|no\./.test(document.querySelector('.tz-msg').textContent));
      await page.reload();
      await page.locator('.tz-bg .tz-c').first().waitFor({ timeout: 60000 });
      await page.selectOption('select[data-k=layout]', 'table');
      await page.selectOption('select[data-k=area]', f);
      await page.waitForFunction(() => !document.querySelector('.tz-all') && document.querySelectorAll('.tz-body .tz-grid').length === 1);
      await page.locator(cellSel(0, 0)).waitFor();
      ok(await cellId(page, 0, 0) === '#red', 'after reload the saved state is shown');
      ok((await page.locator('.tz-pendbar').textContent()).includes(L.pending), 'bar: saved - not online yet');
      ok(await page.locator(cellSel(0, 0) + '.tz-pend').count() === 1 && await page.locator('.tz-body .tz-c.tz-pend').count() === 1, 'only the changed cell carries the mark');
      await page.screenshot({ path: path.join(shots, 'n99-pending.png') });
      await page.locator('[data-k=online]').check();
      await page.waitForFunction(() => !document.querySelector('.tz-pendbar').textContent, null, { timeout: 30000 });
      ok(await cellId(page, 0, 0) === orig && await page.locator('.tz-body .tz-c.tz-pend').count() === 0, '"online state" shows the repo again');
      await page.locator('[data-k=online]').uncheck();
      await page.unroute('**/api/testcentre**');
    }
    ok(!errors.length, scheme + ': no script errors ' + errors.join(' | '));
    await ctx.close();
  }
  await browser.close();
  server.close();
  console.log(`tzeditor ui ok (${checks} checks)`);
})().catch(e => { console.error(e); process.exit(1); });
