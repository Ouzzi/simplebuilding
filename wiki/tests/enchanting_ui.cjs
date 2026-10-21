// Browser test for the enchanting view (?tab=enchanting). Run on sb-test:
//   PW=/var/tmp/sb/pw/node_modules/playwright-core CHROME=~/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome \
//   SHOTS=/var/tmp/sb/wikiench-shots node wiki/tests/enchanting_ui.cjs
// Hover/focus/tap tooltips, texts in DE/EN, click to enchant, phone width, light/dark contrast (A7), screenshots.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require(process.env.PW || 'playwright-core');
const root = path.resolve(__dirname, '..');
const shots = process.env.SHOTS || '';
if (shots) fs.mkdirSync(shots, { recursive: true });
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
function lum(c) { const v = c.match(/[\d.]+/g).slice(0, 3).map(Number).map(x => { x /= 255; return x <= .03928 ? x / 12.92 : Math.pow((x + .055) / 1.055, 2.4); }); return .2126 * v[0] + .7152 * v[1] + .0722 * v[2]; }
const contrast = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
const report = [];
(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${server.address().port}/`;
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME });
  let checks = 0; const ok = (c, m) => { assert.ok(c, m); checks++; };
  try {
    for (const view of [{ w: 1280, h: 900, name: 'desktop' }, { w: 390, h: 844, name: 'phone', touch: true }]) {
      for (const scheme of ['light', 'dark']) {
        for (const lang of ['de', 'en']) {
          const ctx = await browser.newContext({ viewport: { width: view.w, height: view.h }, colorScheme: scheme, hasTouch: !!view.touch });
          const page = await ctx.newPage(); const errors = []; page.on('pageerror', e => errors.push(e.message));
          await page.goto(base + `?mod=simplebuilding&tab=enchanting&lang=${lang}`);
          await page.locator('.ench-gui').waitFor({ timeout: 20000 });
          const tip = page.locator('#mc-tip');
          const rows = page.locator('.ench-row[data-slot]');
          ok(await rows.count() === 3, 'three offers for the diamond pickaxe');
          // hover the bottom offer: "<name> <level> . . . ?" + lapis + levels
          const row3 = rows.nth(2);
          if (view.touch) await row3.tap(); else await row3.hover();
          ok(await tip.isVisible(), 'offer tooltip visible');
          const t3 = await tip.innerText();
          if (lang === 'de') { ok(/…\?/.test(t3), 'DE clue: ' + t3); ok(/3 Lapislazuli/.test(t3) && /3 Erfahrungsstufen/.test(t3), 'DE costs: ' + t3); }
          else { ok(/\. \. \. \?/.test(t3), 'EN clue: ' + t3); ok(/3 Lapis Lazuli/.test(t3) && /3 Enchantment Levels/.test(t3), 'EN costs: ' + t3); }
          const name = tip.locator('.mc-line').first().locator('span.mc-gray, span.mc-red').first();
          ok(await name.count() === 1, 'enchantment name in game gray/red');
          const tipBg = await tip.evaluate(e => getComputedStyle(e).backgroundColor);
          for (const cls of ['mc-white', 'mc-gray']) {
            const c = await tip.locator('.' + cls).first().evaluate(e => getComputedStyle(e).color);
            const k = contrast(c, tipBg); ok(k >= 4.5, `${cls} contrast ${k}`); report.push(`${view.name}/${scheme}/${lang} tooltip ${cls} ${k.toFixed(2)}:1`);
          }
          if (shots && lang === 'de') await page.screenshot({ path: path.join(shots, `ench-${view.name}-${scheme}-${lang}-offer.png`), fullPage: false });
          // too few levels: red requirement, row disabled
          const lvl = page.locator('input[data-k="level"]');
          await lvl.fill('5'); await lvl.press('Enter'); await lvl.blur();
          ok(await page.locator('.ench-row.r-off').count() >= 1, 'rows above the level are disabled');
          const off3 = page.locator('.ench-row[data-slot="2"]');
          if (view.touch) await off3.tap({ force: true }); else await off3.hover({ force: true });
          const req = await tip.innerText();
          ok(lang === 'de' ? /Erforderliche Erfahrungsstufe: 30/.test(req) : /Level Requirement: 30/.test(req), 'requirement: ' + req);
          ok(await tip.locator('.mc-red').count() >= 1, 'requirement in red');
          await lvl.fill('30'); await lvl.press('Enter'); await lvl.blur();
          // click the first offer: enchanted item in the slot, tooltip lists the rolled enchantments
          await page.locator('.ench-row[data-slot="0"]').click();
          const slot = page.locator('.ench-gui .ench-slot').first();
          if (view.touch) await slot.tap(); else await slot.hover();
          const res = await tip.innerText();
          ok(res.split('\n').filter(Boolean).length >= 2, 'result tooltip has name + enchantments: ' + res);
          ok(await tip.locator('.mc-aqua').count() === 1, 'enchanted item name aqua');
          ok(await page.locator('input[data-k="level"]').inputValue() === '29', 'one level paid');
          // books of the family: yellow name + our names
          const bookCell = page.locator('[data-sec="books"] .ench-slot').first();
          await bookCell.scrollIntoViewIfNeeded();
          if (view.touch) await bookCell.tap(); else await bookCell.hover();
          const bt = await tip.innerText();
          ok(lang === 'de' ? /Verzaubertes Buch/.test(bt) : /Enchanted Book/.test(bt), 'book name: ' + bt);
          ok(await tip.locator('.mc-yellow').count() === 1, 'book name yellow');
          ok(await page.locator('[data-sec="books"] .ench-slot').count() >= 20, 'all family books');
          ok(await page.locator('[data-sec="items"] .ench-slot').count() >= 8, 'enchanted items');
          // take the item back: offers again; keyboard focus shows the tooltip
          await page.locator('[data-act="reset"]').click();
          await page.locator('.ench-row[data-slot="1"]').focus();
          ok(await tip.isVisible(), 'focus shows the tooltip');
          // page text contrast against the page background
          const pageBg = await page.evaluate(() => getComputedStyle(document.body).backgroundColor);
          const label = await page.locator('.ench-ctrl label').first().evaluate(e => getComputedStyle(e).color);
          const k2 = contrast(label, pageBg); ok(k2 >= 4.5, 'control label contrast ' + k2); report.push(`${view.name}/${scheme}/${lang} label ${k2.toFixed(2)}:1`);
          const title = await page.locator('.ench-gui .g-title').evaluate(e => getComputedStyle(e).color);
          const k3 = contrast(title, 'rgb(198, 198, 198)'); ok(k3 >= 4.5, 'window title contrast ' + k3); report.push(`window title ${k3.toFixed(2)}:1`);
          // phone: nothing wider than the screen
          const sw = await page.evaluate(() => document.documentElement.scrollWidth);
          ok(sw <= view.w + 1, `no horizontal scroll (${sw} > ${view.w})`);
          // all tooltips open (for review on a phone)
          await page.locator('input[data-k="open"]').check();
          ok(await page.locator('.mc-tip.mc-static').count() >= 28, 'static tooltips');
          if (shots && lang === 'de') await page.screenshot({ path: path.join(shots, `ench-${view.name}-${scheme}-${lang}-open.png`), fullPage: true });
          ok(errors.length === 0, 'page errors: ' + errors.join('; '));
          await ctx.close();
        }
      }
    }
  } finally {
    await browser.close(); server.close();
  }
  console.log([...new Set(report)].join('\n'));
  console.log(`enchanting ui ok: ${checks} checks`);
})().catch(e => { console.error(e); process.exit(1); });
