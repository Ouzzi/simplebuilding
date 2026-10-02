// Run with Playwright available in NODE_PATH; no Minecraft runtime required.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require('playwright');
const root = path.resolve(__dirname, '..');
const server = http.createServer((req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404).end(); return; }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.html') ? 'text/html' : 'application/octet-stream');
    res.end(data);
  });
});
(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    browser = await chromium.launch({ headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    const page = await browser.newPage();
    const errors = []; page.on('pageerror', e => errors.push(e.message));
    const base = `http://127.0.0.1:${server.address().port}/`;
    const visit = async query => { await page.goto(base + query); await page.locator('#main h1').waitFor(); };
    await visit('?mod=simplebuilding&tab=items');
    const filter = page.locator('[data-filter]');
    await filter.fill('hammer');
    const visible = page.locator('[data-filter-target] > .row:not(.hidden)');
    assert.ok(await visible.count() > 0);
    await visible.first().click();
    await page.goBack();
    assert.equal(await filter.inputValue(), 'hammer');
    await page.reload(); await filter.waitFor();
    assert.equal(await filter.inputValue(), 'hammer');
    await visit('?mod=simplebuilding&tab=blocks');
    assert.equal(await filter.inputValue(), '');
    await visit('?mod=simplebuilding&tab=items&lang=de');
    assert.equal(await filter.inputValue(), 'hammer');
    await filter.fill('no-such-item-xyz');
    assert.equal(await visible.count(), 0);
    await page.getByRole('button', { name: 'Filter zurücksetzen' }).click();
    assert.equal(await filter.inputValue(), '');
    assert.ok(await filter.evaluate(el => el === document.activeElement));
    assert.ok(await visible.count() > 0);
    await visit('?mod=simplebuilding&tab=recipes');
    await filter.fill('hammer');
    await visit('?mod=simplemoney&tab=recipes');
    assert.equal(await filter.inputValue(), '');
    await visit('?mod=simplebuilding&tab=allrecipes');
    await page.locator('#ar-q').fill('hammer');
    // Reload immediately: persistence must not wait for the render debounce.
    await page.reload(); await page.locator('#ar-q').waitFor();
    assert.equal(await page.locator('#ar-q').inputValue(), 'hammer');
    await page.locator('#ar-merge').uncheck();
    await visit('?mod=simplemoney&tab=allrecipes');
    assert.equal(await page.locator('#ar-q').inputValue(), '');
    assert.ok(await page.locator('#ar-merge').isChecked());
    await visit('?mod=simplebuilding&tab=allrecipes');
    assert.equal(await page.locator('#ar-q').inputValue(), 'hammer');
    assert.equal(await page.locator('#ar-merge').isChecked(), false);
    // Items page: items and blocks on one page, kind filter, grid view and recipes remembered.
    await visit('?mod=simplebuilding&tab=items');
    await filter.fill('');
    const all = await visible.count();
    await page.locator('[data-iv=kind][data-v=blocks]').click();
    const blocksOnly = await visible.count();
    assert.ok(blocksOnly > 0 && blocksOnly < all, `${blocksOnly} of ${all}`);
    assert.equal(await page.locator('.iv-rows > .row:not(.hidden)[data-k~=blocks]').count(), blocksOnly);
    await page.locator('[data-iv=kind][data-v=all]').click();
    await page.locator('[data-iv=view][data-v=grid]').click();
    await page.locator('#iv-rc').check();
    await page.reload(); await page.locator('.iv-rows').waitFor();
    assert.equal(await page.locator('.iv-rows.as-grid.with-rc').count(), 1);
    assert.ok(await page.locator('.iv-rows .row-recipes .recipe-body').count() > 0);
    assert.equal(await page.locator('.iv-rows a a').count(), 0);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('#iv-rc').uncheck();
    const cols = await page.locator('.iv-rows').evaluate(el => getComputedStyle(el).gridTemplateColumns.split(' ').length);
    assert.ok(cols >= 3, `grid columns at phone width: ${cols}`);
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= 390));
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.locator('[data-iv=view][data-v=list]').click();
    await visit('?mod=simplebuilding&tab=blocks');
    assert.equal(await page.locator('.iv-rows').getAttribute('data-kind-filter'), 'blocks');
    // All recipes grouped by station: collapsible, expand/collapse all.
    await visit('?mod=simplebuilding&tab=allrecipes');
    await page.locator('#ar-groupst').check();
    const secs = page.locator('details.ar-sec');
    assert.ok(await secs.count() > 0);
    await page.getByRole('button', { name: 'Collapse all' }).click();
    assert.equal(await page.locator('details.ar-sec[open]').count(), 0);
    await page.getByRole('button', { name: 'Expand all' }).click();
    assert.equal(await page.locator('details.ar-sec[open]').count(), await secs.count());
    assert.ok(await page.locator('details.ar-sec .rc').count() > 0);
    await page.locator('#ar-groupst').uncheck();
    await visit('?mod=simplebuilding&tab=config');
    const wrap = page.locator('.table-wrap').first();
    assert.equal(await wrap.getAttribute('tabindex'), '0');
    await wrap.evaluate(el => { el.scrollTop = 400; });
    const geometry = await wrap.evaluate(el => ({ top: el.getBoundingClientRect().top, head: el.querySelector('th').getBoundingClientRect().top, scroll: el.scrollTop }));
    assert.ok(geometry.scroll > 0);
    assert.ok(Math.abs(geometry.top - geometry.head) < 3, JSON.stringify(geometry));
    await page.setViewportSize({ width: 390, height: 844 });
    await wrap.focus();
    assert.ok(await wrap.evaluate(el => el.clientWidth <= 390));
    // Unavailable storage must still preserve filters within the current session.
    await page.addInitScript(() => {
      Storage.prototype.getItem = Storage.prototype.setItem = () => { throw new Error('storage disabled'); };
    });
    await visit('?mod=simplebuilding&tab=items');
    await filter.fill('hammer'); await visible.first().click(); await page.goBack();
    assert.equal(await filter.inputValue(), 'hammer');
    assert.deepEqual(errors, []);
    console.log('PASS: list navigation/reload, module isolation, reset, recipe query, items+blocks views, grouped recipes, sticky headers, mobile and unavailable storage');
  } finally {
    if (browser) await browser.close();
    server.close();
  }
})().catch(err => { console.error(err); process.exitCode = 1; });
