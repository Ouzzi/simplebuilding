// Browser test (b-texed16): the dev overlay shows saved textures at once and their state. Playwright in NODE_PATH:
//   NODE_PATH=/root/pwlocal/node_modules WIKI_CHROME=<chrome> node tools/zentrale-web/tests/overlay_saves_ui.cjs
// Fake API: one pending save (layers composed in the page), one taken over (cache-busting ?v=), then a live save event.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
let pw; try { pw = require('playwright'); } catch (e) { pw = require('playwright-core'); }
const repo = path.resolve(__dirname, '../../..');
const C = require(path.join(repo, 'wiki/assets/pixeleditor-core.js'));
const red = C.blank(1, 1, 'a'); red.layers[0].px.set([200, 0, 0, 255]);
const LAYERS = C.dumps(C.toDoc(red));
const saves = [
  { sid: '1794000000003', texture: 'x:block/a', state: 'new', createdAt: 1794000000003 },
  { sid: '1794000000002', texture: 'x:block/b', state: 'new', createdAt: Date.now(), adopted: { commit: 'abc1234', subject: 's', bundle: 'b-bundle170' } },
  { sid: '1794000000001', texture: 'x:block/a', state: 'new', createdAt: 1794000000001 }];
const PAGE = `<!doctype html><html lang="de"><body>
<script>window.LANG='de';window.WIKI_TEXTURES={textures:[
 {id:'x:block/a',file:'t/a.png',source:'src/main/resources/assets/x/textures/block/a.png',w:1,h:1},
 {id:'x:block/b',file:'t/b.png',source:'src/main/resources/assets/x/textures/block/b.png',w:1,h:1},
 {id:'x:block/c',file:'t/c.png',source:'src/main/resources/assets/x/textures/block/c.png',w:1,h:1}]};</script>
<div id="tx-results">
 <button class="tx-tile" data-tx-id="x:block/a" data-tx-path="t/a.png"><img src="t/a.png" id="ia"></button>
 <button class="tx-tile" data-tx-id="x:block/b" data-tx-path="t/b.png"><img src="t/b.png" id="ib"></button>
 <button class="tx-tile" data-tx-id="x:block/c" data-tx-path="t/c.png"><img src="t/c.png" id="ic"></button>
</div><script src="dev-overlay.js"></script></body></html>`;
const server = http.createServer((req, res) => {
  const u = req.url.split('?')[0];
  const json = (d, code = 200) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(d)); };
  if (u === '/') { res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' }); return res.end(PAGE); }
  if (u === '/dev-overlay.js') { res.writeHead(200, { 'Content-Type': 'text/javascript; charset=utf-8' }); return res.end(fs.readFileSync(path.join(repo, 'tools/zentrale-web/dev-overlay.js'))); }
  if (u === '/assets/pixeleditor-core.js') { res.writeHead(200, { 'Content-Type': 'text/javascript; charset=utf-8' }); return res.end(fs.readFileSync(path.join(repo, 'wiki/assets/pixeleditor-core.js'))); }
  if (u === '/api/queue') return json([]);
  if (u === '/api/textures') return json(saves);
  if (u.startsWith('/api/textures/')) { const s = saves.find(x => x.sid === u.split('/').pop()); return s ? json(Object.assign({ layers: LAYERS }, s)) : json({}, 404); }
  if (u.startsWith('/t/')) { res.writeHead(200, { 'Content-Type': 'image/png' }); return res.end(Buffer.alloc(0)); }
  json({ error: 'not found' }, 404);
});
(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const base = 'http://127.0.0.1:' + server.address().port + '/';
  const browser = await pw.chromium.launch(process.env.WIKI_CHROME ? { executablePath: process.env.WIKI_CHROME } : {});
  try {
    const page = await browser.newPage();
    const errors = []; page.on('pageerror', e => errors.push(String(e)));
    await page.goto(base);
    await page.waitForFunction(() => /^blob:/.test(document.getElementById('ia').getAttribute('src')), null, { timeout: 10000 });
    // pending save: the newest one of x:block/a composed from its layers, badge "gespeichert"
    const pix = await page.evaluate(() => new Promise(ok => { const i = new Image(); i.onload = () => { const c = document.createElement('canvas'); c.width = c.height = 1; const g = c.getContext('2d'); g.drawImage(i, 0, 0); ok(Array.from(g.getImageData(0, 0, 1, 1).data)); }; i.src = document.getElementById('ia').src; }));
    assert.deepEqual(pix, [200, 0, 0, 255], 'tile shows the saved composition');
    const badge = page.locator('[data-tx-id="x:block/a"] .dev-saved');
    assert.equal(await badge.textContent(), 'gespeichert');
    assert.match(await badge.getAttribute('title'), /wartet auf Übernahme/);
    assert.equal(await page.locator('#ia').getAttribute('data-dev-src'), 't/a.png');
    // taken over: checkout file with ?v=<sid>, badge "übernommen", bundle in the title
    await page.waitForFunction(() => document.getElementById('ib').getAttribute('src') === 't/b.png?v=1794000000002');
    assert.equal(await page.locator('[data-tx-id="x:block/b"] .dev-saved').textContent(), 'übernommen');
    assert.match(await page.locator('[data-tx-id="x:block/b"] .dev-saved').getAttribute('title'), /Bündel 170 \(abc1234\)/);
    // untouched texture stays as it is
    assert.equal(await page.locator('#ic').getAttribute('src'), 't/c.png');
    assert.equal(await page.locator('[data-tx-id="x:block/c"] .dev-saved').count(), 0);
    // live save from the editor: event -> tile swapped at once
    await page.evaluate(async () => {
      const C = window.SBPixelCore, m = C.blank(1, 1, 'a'); m.layers[0].px.set([0, 0, 250, 255]);
      const png = await C.pngEncode(1, 1, C.compose(m));
      window.dispatchEvent(new CustomEvent('sb-texture-saved', { detail: { id: 'x:block/c', sid: '1794000000009', png: png, layers: C.dumps(C.toDoc(m)) } }));
    });
    await page.waitForFunction(() => /^blob:/.test(document.getElementById('ic').getAttribute('src')));
    await page.locator('[data-tx-id="x:block/c"] .dev-saved').waitFor();
    // contrast of the badges: white on #9a6200 / #2f7d3a (>= 4.5:1, checked in the source comment) - colour as set
    assert.equal(await badge.evaluate(e => getComputedStyle(e).backgroundColor), 'rgb(154, 98, 0)');
    assert.deepEqual(errors, []);
    console.log('overlay saves ui: ok');
  } finally { await browser.close(); server.close(); }
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
