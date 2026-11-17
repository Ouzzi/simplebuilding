// N80 (docs/ai/PLAN-B-N80-2026-11-16.md): Guidebook and Questionary drawn like the game screens.
// Run with Playwright in NODE_PATH (WIKI_CHROME = browser executable, else channel WIKI_BROWSER || msedge); WIKI_BASE = a deployed wiki.
// Desktop 1280 and phone 390, light and dark: route opens, book frame visible, page turn by arrow button and key, tab switch,
// single page on the phone, integer scale, no script/console errors.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const pw = require('playwright');
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
// git-ignored pictures (Vanilla textures, renders that a generator run did not make here) may be missing
const IGNORED_404 = /\/assets\/(textures|vanilla|ui)\/|\/data\/(ui-shots|vanilla-textures)\.js|favicon|\/api\/auth\/check/;
let checks = 0;
const ok = (cond, msg) => { assert.ok(cond, msg); checks++; };

async function run(browser, base, w, theme) {
  const tag = `${w} ${theme}`;
  const phone = w < 700;
  const ctx = await browser.newContext({ viewport: { width: w, height: 900 }, colorScheme: theme });
  // WIKI_COOKIE="name=value": the login session of a deployed dev wiki (/dev/ sits behind the login), as in wiki24_ui
  if (process.env.WIKI_COOKIE) {
    const [name, ...rest] = process.env.WIKI_COOKIE.split('=');
    await ctx.addCookies([{ name, value: rest.join('='), url: new URL('/', base).href }]);
  }
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error' && !/Failed to load resource/.test(m.text())) errors.push('console: ' + m.text()); });
  page.on('response', r => { if (r.status() >= 400 && !IGNORED_404.test(r.url())) errors.push(r.status() + ' ' + r.url()); });
  const pos = () => page.locator('.gb-pos').textContent();

  // Guidebook
  await page.goto(base + '?tab=guidebook&lang=en'); await page.locator('.gb-book').waitFor();
  const sheet = page.locator('.gb-sheet .spr').first();
  ok(await sheet.isVisible(), tag + ' book frame visible');
  ok(await page.evaluate(() => /book\.png/.test(getComputedStyle(document.querySelector('.gb-sheet .spr')).backgroundImage)), tag + ' book texture');
  ok(await page.evaluate(() => getComputedStyle(document.querySelector('.gb-sheet .spr')).imageRendering === 'pixelated'), tag + ' pixelated');
  const s = await page.evaluate(() => +getComputedStyle(document.querySelector('.gb-stage')).getPropertyValue('--s'));
  ok(Number.isInteger(s) && s >= 1, tag + ' integer scale ' + s);
  const box = await page.locator('.gb-book').boundingBox();
  ok(Math.abs(box.width - (phone ? 154 : 292) * s) < 1 && Math.abs(box.height - 180 * s) < 1, tag + ' book size in game pixels ' + JSON.stringify(box));
  ok(box.x >= 0 && box.x + box.width <= w, tag + ' book inside the viewport');
  ok(await page.locator('.gb-page').count() === (phone ? 1 : 2), tag + (phone ? ' single page' : ' two-page spread'));
  ok(await page.locator('.gb-stab').count() === (phone ? 0 : Math.min(15, await page.locator('.gb-jump option').count())), tag + ' side bookmarks');
  const p0 = await pos();
  await page.click('[data-gb-turn="1"]'); await page.waitForTimeout(150);
  const p1 = await pos();
  ok(p1 !== p0, tag + ' arrow turns the page');
  await page.keyboard.press('ArrowRight'); await page.waitForTimeout(150);
  ok(await pos() !== p1, tag + ' key right turns the page');
  await page.keyboard.press('ArrowLeft'); await page.waitForTimeout(150);
  ok(await pos() === p1, tag + ' key left turns back');
  const tabs = page.locator('.gb-tab');
  ok(await tabs.count() >= 2, tag + ' section bookmarks');
  const second = await tabs.nth(1).getAttribute('data-gb-sec');
  await tabs.nth(1).click(); await page.waitForTimeout(200);
  ok(await page.locator(`.gb-tab[data-gb-sec="${second}"]`).getAttribute('aria-selected') === 'true' && new URL(page.url()).searchParams.get('book').startsWith(second), tag + ' section switch');
  await page.locator('.gb-toc .gb-link').first().click(); await page.waitForTimeout(150);
  ok(await page.locator('.gb-h3, .gb-h').count() > 0 && !/^Page 1 /.test(await pos()), tag + ' contents entry opens its chapter');
  if (!phone) {
    ok(await page.locator('.gb-stab[aria-current="true"]').count() === 1, tag + ' current chapter bookmark');
    await page.locator('.gb-stab:not(.gb-back)').nth(1).click(); await page.waitForTimeout(150);
    ok(await page.locator('.gb-stab[aria-current="true"]').count() === 1, tag + ' chapter bookmark jumps');
  }
  await page.locator('.gb-back').first().click(); await page.waitForTimeout(150);
  ok(await page.locator('.gb-toc').count() > 0, tag + ' back to the contents');

  // Questionary
  await page.goto(base + '?tab=questionary&lang=en'); await page.locator('.qn-book').waitFor();
  ok(await page.locator('.qn-cover').isVisible() && await page.locator('.qn-desk').isVisible(), tag + ' questionary book and desk');
  ok(/Questionary|Fragebuch/.test(await page.locator('.qn-text.l .qn-h').textContent()), tag + ' welcome page');
  ok(await page.locator('.qn-book').evaluate(e => e.classList.contains('qn-narrow')) === phone, tag + ' narrow layout only on the phone');
  await page.locator('.qn-slots .qn-slot').first().click(); await page.locator('#qn-open').waitFor();
  ok(await page.locator('#qn-open .qn-ribbon').count() === 3, tag + ' three bookmarks');
  const head = () => page.locator('.qn-text.r .qn-h').first().textContent();
  ok(/Picture plate/.test(await head()) && await page.locator('.qn-num').textContent() === '1 / 3', tag + ' page 1 picture plate');
  await page.click('[data-qn-turn="1"]'); await page.waitForTimeout(150);
  ok(/Recipe/.test(await head()) && await page.locator('.qn-num').textContent() === '2 / 3', tag + ' arrow to page 2');
  await page.locator('.qn-book').focus();
  await page.keyboard.press('ArrowRight'); await page.waitForTimeout(150);
  ok(/Magnifier/.test(await head()), tag + ' key to page 3');
  await page.keyboard.press('a'); await page.waitForTimeout(150);
  ok(/Recipe/.test(await head()), tag + ' A turns back');
  await page.click('[data-qn-page="0"]'); await page.waitForTimeout(150);
  ok(await page.locator('[data-qn-page="0"]').getAttribute('aria-pressed') === 'true', tag + ' bookmark 1');
  await page.click('[data-qn-close]'); await page.waitForTimeout(150);
  ok(await page.locator('#qn-open').count() === 0, tag + ' item taken off the table');

  assert.deepEqual(errors, [], tag + ' errors');
  await ctx.close();
}

(async () => {
  let base = process.env.WIKI_BASE;
  if (!base) { await new Promise(resolve => server.listen(0, '127.0.0.1', resolve)); base = `http://127.0.0.1:${server.address().port}/`; }
  let browser;
  try {
    const exe = process.env.WIKI_CHROME;
    browser = await pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    for (const w of [1280, 390]) for (const theme of ['light', 'dark']) await run(browser, base, w, theme);
    console.log(`n80 books ui ok: ${checks} checks`);
  } finally {
    if (browser) await browser.close();
    server.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
