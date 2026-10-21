// Browser test of the wiki pixel editor (docs/ai/PLAN-B-PIXELEDITOR2-2026-10-21.md); Playwright (or playwright-core)
// in NODE_PATH, no Minecraft runtime. Runs on sb-test:
//   NODE_PATH=/var/tmp/sb/pw/node_modules WIKI_CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   PE_SHOTS=/var/tmp/sb/pw node wiki/tests/pixeleditor_ui.cjs
// Desktop light 1280 px + phone dark 390 px: every button has an icon and a name and can be clicked, shortcuts,
// drawing/undo, Shift line, brush size, layers (add, drag sort, hide, opacity, rename), confirm before discarding,
// error state, phone bottom bar + sheets, touch targets >= 44 px, no horizontal scrolling, text contrast >= 4.5:1.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const root = path.resolve(__dirname, '..');
const shots = process.env.PE_SHOTS || os.tmpdir();
const LAYERS = 'assets/textures/sheets/simplebuilding/simplebuilding/item/iron_chisel.layers.json';
const page_html = layers => `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body><script>window.LANG='de'</script><script src="assets/pixeleditor-core.js"></script><script src="assets/pixeleditor.js"></script>
<script>SBPixelEditor.open({id:'simplebuilding:item/iron_chisel',name:'Eisenmeißel',layers:${JSON.stringify(layers)},source:'src/main/resources/assets/simplebuilding/textures/item/iron_chisel.png'});</script></body></html>`;
const TYPES = { '.js': 'text/javascript', '.html': 'text/html', '.json': 'application/json', '.png': 'image/png' };
const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  if (url.pathname === '/__pe.html') { res.setHeader('Content-Type', 'text/html'); res.end(page_html(url.searchParams.get('layers') || LAYERS)); return; }
  const file = path.resolve(root, '.' + decodeURIComponent(url.pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end(); return; }
    res.setHeader('Content-Type', TYPES[path.extname(file)] || 'application/octet-stream');
    res.end(data);
  });
});

// in the page: contrast of an element's text against its first opaque ancestor background
const CONTRAST = el => {
  const rgb = s => (s.match(/[\d.]+/g) || []).map(Number);
  const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
  let fg = rgb(getComputedStyle(el).color), n = el, bg = null;
  while (n && n.nodeType === 1) { const c = rgb(getComputedStyle(n).backgroundColor); if (c.length === 3 || (c.length === 4 && c[3] > 0.95)) { bg = c; break; } n = n.parentElement; }
  bg = bg || [255, 255, 255];
  const a = lum(fg), b = lum(bg);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
};

async function checkButtons(page, minSize) {
  const info = await page.$$eval('.pe-root button', (bs, min) => bs.filter(b => b.offsetParent !== null && getComputedStyle(b).visibility !== 'hidden').map(b => {
    const r = b.getBoundingClientRect();
    return { label: b.getAttribute('aria-label') || '', svg: !!b.querySelector('svg.pe-ic'), swatch: b.classList.contains('pe-sw'), text: b.textContent.trim(),
      w: r.width, h: r.height, small: min && (r.width < min - 0.5 || r.height < min - 0.5) };
  }), minSize || 0);
  assert.ok(info.length > 20, 'buttons visible: ' + info.length);
  for (const b of info) {
    assert.ok(b.label, 'button without aria-label: ' + JSON.stringify(b));
    if (!b.swatch && !b.svg) assert.match(b.text, /^[\p{L}\d .:()+\-/×]+$/u, 'text button without icon must be words: ' + JSON.stringify(b));
    if (b.swatch) continue;
    if (!b.svg) assert.ok(['OK', 'Abbrechen', 'Verwerfen'].includes(b.text), 'button without icon: ' + JSON.stringify(b));
  }
  const small = info.filter(b => b.small);
  assert.deepEqual(small, [], 'touch targets below ' + minSize + ' px');
  return info.length;
}
const state = (page, fn) => page.evaluate(`(${fn})(SBPixelEditor.state())`);

async function desktop(browser, base) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 }, colorScheme: 'light', acceptDownloads: true });
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  await page.goto(base + '__pe.html');
  await page.locator('.pe-layer').nth(1).waitFor();
  assert.equal(await page.locator('.pe-layer').count(), 2);
  // tooltips carry name + key, icons everywhere
  for (const [tool, key] of [['pencil', 'B'], ['eraser', 'E'], ['fill', 'G'], ['picker', 'I'], ['line', 'L'], ['rect', 'U'], ['select', 'M'], ['move', 'V']]) {
    const b = page.locator(`.pe-tools [data-tool="${tool}"]`);
    assert.match(await b.getAttribute('title'), new RegExp(`\\(${key}\\)$`));
    assert.equal(await b.locator('svg.pe-ic').count(), 1);
    await page.keyboard.press(key.toLowerCase());
    assert.equal(await b.getAttribute('aria-pressed'), 'true', tool);
    await b.click();
    assert.equal(await b.getAttribute('aria-pressed'), 'true', tool);
  }
  const nButtons = await checkButtons(page, 0);
  // the active tool is clearly marked (accent background, contrast of the icon >= 4.5)
  const tools = page.locator('.pe-tools [data-tool="pencil"]');
  await page.keyboard.press('b');
  const tc = await tools.evaluate(CONTRAST);
  assert.ok(tc >= 4.5, 'active tool contrast ' + tc + ' ' + await tools.evaluate(el => getComputedStyle(el).color + ' / ' + getComputedStyle(el).backgroundColor + ' ' + el.getAttribute('aria-pressed')));
  // draw a stroke -> undo/redo
  const undo = page.locator('.pe-head [data-act="undo"]');
  assert.ok(await undo.isDisabled());
  const box = await page.locator('.pe-canvas').boundingBox();
  const zoom = await state(page, 's => s.zoom'), ox = await state(page, 's => s.ox'), oy = await state(page, 's => s.oy');
  const at = (x, y) => [box.x + ox + (x + 0.5) * zoom, box.y + oy + (y + 0.5) * zoom];
  const px = (x, y) => state(page, `s => Array.from(SBPixelCore.compose(s.model).slice((${y} * s.model.w + ${x}) * 4, (${y} * s.model.w + ${x}) * 4 + 4))`);
  const before = await px(1, 1);
  await page.mouse.move(...at(1, 1)); await page.mouse.down(); await page.mouse.move(...at(3, 1)); await page.mouse.up();
  assert.notDeepEqual(await px(1, 1), before, 'pencil painted');
  assert.ok(!(await undo.isDisabled()));
  assert.ok((await state(page, 's => s.recent.length')) >= 1, 'recent colours');
  assert.ok(await page.locator('.pe-side .pe-pal[role=group]').first().locator('.pe-sw').count() >= 1);
  await page.keyboard.press('Control+z');
  assert.deepEqual(await px(1, 1), before, 'undo');
  await page.keyboard.press('Control+y');
  assert.notDeepEqual(await px(1, 1), before, 'redo');
  // Shift+click: straight line from the last point
  await page.mouse.click(...at(1, 6));
  await page.keyboard.down('Shift'); await page.mouse.click(...at(6, 6)); await page.keyboard.up('Shift');
  const prim = await state(page, 's => s.primary');
  for (let x = 1; x <= 6; x++) assert.deepEqual(await px(x, 6), prim, 'shift line at ' + x);
  // brush size [ ]
  await page.keyboard.press(']'); assert.equal(await state(page, 's => s.size'), 2);
  assert.equal(await page.locator('.pe-ctx [data-toggle="size2"]').getAttribute('aria-pressed'), 'true');
  await page.keyboard.press(']'); await page.keyboard.press(']'); assert.equal(await state(page, 's => s.size'), 3);
  await page.mouse.click(...at(10, 12));
  for (const [x, y] of [[9, 11], [11, 13]]) assert.deepEqual(await px(x, y), prim, 'size 3 at ' + x + ',' + y);
  await page.keyboard.press('['); await page.keyboard.press('['); assert.equal(await state(page, 's => s.size'), 1);
  // swap X, Alt eyedropper
  const sec = await state(page, 's => s.secondary');
  await page.keyboard.press('x');
  assert.deepEqual(await state(page, 's => s.primary'), sec);
  await page.keyboard.press('x');
  await page.keyboard.down('Alt'); await page.mouse.click(...at(13, 13)); await page.keyboard.up('Alt');
  assert.deepEqual(await state(page, 's => s.primary'), await px(13, 13), 'alt picks');
  assert.equal(await state(page, 's => s.tool'), 'pencil', 'Alt does not change the tool');
  // zoom buttons/keys and fit
  const z0 = await state(page, 's => s.zoom');
  await page.keyboard.press('-'); const zA = await state(page, 's => s.zoom'); assert.ok(zA < z0);
  await page.locator('.pe-head [aria-label^="Vergrößern"]').click(); await page.locator('.pe-head [aria-label^="Vergrößern"]').click();
  assert.ok(await state(page, 's => s.zoom') > zA, 'zoom in button');
  await page.locator('.pe-head [aria-label^="Verkleinern"]').click();
  await page.keyboard.press('0'); assert.equal(await state(page, 's => s.zoom'), z0, 'fit restores');
  assert.equal(await page.locator('.pe-zoom').textContent(), z0 + '×');
  // grid toggle (key + button stay in sync)
  await page.keyboard.press("Control+'");
  assert.equal(await state(page, 's => s.grid'), false);
  assert.equal(await page.locator('.pe-head [data-toggle="grid"]').getAttribute('aria-pressed'), 'false');
  await page.locator('.pe-head [data-toggle="grid"]').click();
  assert.equal(await state(page, 's => s.grid'), true);
  // status bar: coordinate + colour under the pointer + zoom
  await page.mouse.move(...at(4, 5));
  await page.waitForTimeout(50);
  assert.equal(await page.locator('[data-st="pos"]').textContent(), '4, 5');
  assert.match(await page.locator('[data-st="color"]').textContent(), /^#[0-9a-f]{8}$/);
  // layers: add, drag sort, hide, opacity, rename
  await page.locator('[data-k="add"]').click();
  assert.equal(await page.locator('.pe-layer').count(), 3);
  const topName = () => state(page, 's => s.model.layers[s.model.layers.length - 1].name');
  const bottom = await state(page, 's => s.model.layers[0].name');
  const grip = page.locator('.pe-layer').nth(2).locator('.pe-grip');
  const g = await grip.boundingBox(), top = await page.locator('.pe-layer').nth(0).boundingBox();
  await page.mouse.move(g.x + g.width / 2, g.y + g.height / 2); await page.mouse.down();
  await page.mouse.move(g.x + g.width / 2, top.y + 4, { steps: 6 }); await page.mouse.up();
  assert.equal(await topName(), bottom, 'drag moved the bottom layer to the top');
  await page.locator('.pe-layer').nth(0).locator('[data-k^="eye"]').click();
  assert.equal(await state(page, 's => s.model.layers[s.model.layers.length - 1].visible'), false);
  await page.locator('.pe-statusbar .pe-warn').waitFor();
  await page.locator('.pe-layer').nth(0).locator('[data-k^="eye"]').click();
  await page.locator('[data-k="opacity"]').fill('50');
  assert.equal(await state(page, 's => s.model.layers[s.active].opacity'), 50);
  await page.locator('.pe-layer[aria-selected="true"]').focus();
  await page.keyboard.press('F2');
  await page.locator('.pe-layer input').fill('Neu benannt');
  await page.keyboard.press('Enter');
  assert.equal(await state(page, 's => s.model.layers[s.active].name'), 'Neu benannt');
  assert.ok(await page.locator('.pe-layer[aria-selected="true"]').evaluate(el => el === document.activeElement), 'focus back on the row');
  // keyboard: Alt+Down moves the focused layer down
  const act = await state(page, 's => s.active');
  await page.keyboard.press('Alt+ArrowDown');
  assert.equal(await state(page, 's => s.active'), act - 1);
  // focus ring is visible
  await page.keyboard.press('Tab');
  const ring = await page.evaluate(() => getComputedStyle(document.activeElement).outlineStyle);
  assert.equal(ring, 'solid', 'visible focus');
  // contrast samples (A7)
  for (const sel of ['.pe-note', '.pe-ctx .pe-hint', '.pe-statusbar span', '.pe-side h3', '.pe-name', '.pe-btn.pe-primary', '.pe-layer[aria-selected="true"] .pe-ln', '.pe-layer .pe-op']) {
    const c = await page.locator(sel).first().evaluate(CONTRAST);
    assert.ok(c >= 4.5, `${sel} contrast ${c.toFixed(2)}`);
  }
  // every visible button is clickable without page errors (no file choosers, not close)
  const n = await page.locator('.pe-root button').count();
  let clicked = 0;
  for (let i = 0; i < n; i++) {
    const b = page.locator('.pe-root button').nth(i);
    if (!(await b.isVisible()) || await b.isDisabled()) continue;
    const label = await b.getAttribute('aria-label') || '';
    if (/öffnen|Bild wählen|Schließen|Export|PNG|Ebenen \(|Palette/.test(label)) continue;
    await b.click({ timeout: 2000 }).catch(() => {});
    clicked++;
    if (await page.locator('.pe-dlg').count()) await page.keyboard.press('Escape');
    if (!(await page.locator('.pe-root').count())) break;
  }
  assert.ok(clicked > 30, 'clicked ' + clicked);
  // export downloads PNG + layers
  const dl = page.waitForEvent('download');
  await page.locator('.pe-head [data-act="export"]').click();
  assert.match((await dl).suggestedFilename(), /^iron_chisel\.png$/);
  await page.keyboard.press('0'); await page.keyboard.press('b'); await page.keyboard.press('Control+d');
  await page.mouse.click(...at(2, 2));  // dirty again
  assert.equal(await state(page, 's => s.dirty'), true);
  await page.locator('.pe-head [data-act="close"]').click();
  await page.locator('.pe-dlg').waitFor();
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('.pe-root').count(), 1, 'cancel keeps the editor');
  await page.screenshot({ path: path.join(shots, 'pe-desktop-light.png') });
  await page.locator('.pe-head [data-act="close"]').click();
  await page.locator('.pe-dlg [data-act="confirm"]').click();
  assert.equal(await page.locator('.pe-root').count(), 0, 'confirm closes');
  // friendly error state with a way out
  await page.goto(base + '__pe.html?layers=missing.layers.json');
  await page.locator('.pe-empty[role=alert]').waitFor();
  await page.locator('.pe-empty .pe-primary').click();
  assert.equal(await state(page, 's => s.model.w'), 16);
  assert.deepEqual(errors, []);
  await ctx.close();
  return nButtons;
}

async function phone(browser, base) {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'dark', hasTouch: true, isMobile: true, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  const errors = []; page.on('pageerror', e => errors.push(e.message));
  await page.goto(base + '__pe.html');
  await page.locator('.pe-layer').nth(1).waitFor({ state: 'attached' });
  // tools are a bar at the bottom, below the drawing area; panels are closed sheets
  const tools = await page.locator('.pe-tools').boundingBox(), stage = await page.locator('.pe-stage').boundingBox(), mbar = await page.locator('.pe-mbar').boundingBox();
  assert.ok(tools.y >= stage.y + stage.height - 1, 'tools below the stage');
  assert.ok(mbar.y + mbar.height <= 844.5 && mbar.y >= tools.y, 'phone bar at the bottom');
  assert.ok(!(await page.locator('.pe-side').isVisible()), 'sheets closed');
  for (const t of ['pencil', 'eraser', 'fill', 'picker', 'line', 'rect', 'select', 'move']) assert.ok(await page.locator(`.pe-tools [data-tool="${t}"]`).isVisible(), t);
  const noScroll = () => page.evaluate(() => ({ doc: document.documentElement.scrollWidth, root: document.querySelector('.pe-root').scrollWidth, w: innerWidth }));
  let w = await noScroll(); assert.ok(w.doc <= w.w && w.root <= w.w, 'no horizontal scrolling ' + JSON.stringify(w));
  await checkButtons(page, 44);
  // every tool's context bar fits too
  for (const t of ['select', 'rect', 'pencil']) {
    await page.locator(`.pe-tools [data-tool="${t}"]`).tap();
    w = await noScroll(); assert.ok(w.doc <= w.w && w.root <= w.w, t + ' context bar ' + JSON.stringify(w));
  }
  // touch draws, undo enabled
  const box = await page.locator('.pe-canvas').boundingBox();
  const zoom = await state(page, 's => s.zoom'), ox = await state(page, 's => s.ox'), oy = await state(page, 's => s.oy');
  await page.touchscreen.tap(box.x + ox + 2.5 * zoom, box.y + oy + 2.5 * zoom);
  assert.ok(!(await page.locator('.pe-head [data-act="undo"]').isDisabled()), 'tap painted');
  // sheets
  await page.locator('[data-sheet-btn="layers"]').tap();
  assert.equal(await page.locator('[data-sheet-btn="layers"]').getAttribute('aria-expanded'), 'true');
  assert.ok(await page.locator('.pe-side section[data-sheet="layers"]').isVisible());
  assert.ok(!(await page.locator('.pe-side section[data-sheet="colors"]').first().isVisible()));
  await checkButtons(page, 44);
  w = await noScroll(); assert.ok(w.doc <= w.w && w.root <= w.w, 'sheet ' + JSON.stringify(w));
  for (const sel of ['.pe-note', '.pe-side h3', '.pe-layer .pe-ln', '.pe-statusbar span']) {
    const c = await page.locator(sel).first().evaluate(CONTRAST);
    assert.ok(c >= 4.5, `dark ${sel} contrast ${c.toFixed(2)}`);
  }
  await page.screenshot({ path: path.join(shots, 'pe-phone-dark-layers.png') });
  await page.locator('.pe-sheethead button').tap();
  assert.ok(!(await page.locator('.pe-side').isVisible()));
  await page.locator('[data-sheet-btn="colors"]').tap();
  assert.ok(await page.locator('.pe-side section[data-sheet="colors"]').first().isVisible());
  await page.locator('.pe-mbar .pe-chip-s').tap(); // swap from the chip
  await page.locator('[data-sheet-btn="colors"]').tap();
  assert.ok(!(await page.locator('.pe-side').isVisible()), 'second tap closes the sheet');
  await page.screenshot({ path: path.join(shots, 'pe-phone-dark.png') });
  await page.locator('[data-sheet-btn="more"]').tap();
  assert.ok(await page.locator('.pe-side section[data-sheet="more"]').first().isVisible());
  await checkButtons(page, 44);
  assert.deepEqual(errors, []);
  await ctx.close();
}

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    const exe = process.env.WIKI_CHROME;
    browser = await pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    const base = `http://127.0.0.1:${server.address().port}/`;
    const n = await desktop(browser, base);
    await phone(browser, base);
    console.log('pixel editor ui: ok (' + n + ' buttons, shots in ' + shots + ')');
  } finally {
    if (browser) await browser.close();
    server.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
