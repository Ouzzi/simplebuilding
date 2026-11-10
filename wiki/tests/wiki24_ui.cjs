// N24 (docs/ai/PLAN-B-WIKI24-2026-11-10.md): sidebar groups, Getting Started, Submods, Guidebook, Questionary, old URLs.
// Run with Playwright in NODE_PATH (WIKI_CHROME = browser executable, else channel WIKI_BROWSER || msedge).
// Every page in desktop 1280 px and phone 390 px, light and dark, English and German: no script error, no failed request
// except git-ignored images (Vanilla textures, ui-shots), text contrast >= 4.5:1 (3:1 for large text) on the new parts.
// WIKI_BASE=https://host/dev/ runs the same clicks against a deployed wiki (dev instance) instead of a local server.
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
const IGNORED_404 = /\/assets\/(textures|vanilla|ui)\/|\/data\/(ui-shots|vanilla-textures)\.js|favicon|\/api\/auth\/check/;  // sitebar.js asks for a login; hosts without one answer 404
let checks = 0;
const ok = (cond, msg) => { assert.ok(cond, msg); checks++; };

async function contrast(page, selector) {
  return page.evaluate(sel => {
    function rgb(s) { const m = s.match(/[\d.]+/g); return m ? m.map(Number) : [0, 0, 0, 0]; }
    function lum(c) { const a = c.slice(0, 3).map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }); return 0.2126 * a[0] + 0.7152 * a[1] + 0.0722 * a[2]; }
    function bg(el) {
      for (let e = el; e; e = e.parentElement) {
        const c = rgb(getComputedStyle(e).backgroundColor);
        if ((c[3] === undefined || c[3] > 0.5) && getComputedStyle(e).backgroundColor !== 'rgba(0, 0, 0, 0)') return c;
      }
      return [255, 255, 255];
    }
    let worst = { ratio: 99 };
    document.querySelectorAll(sel).forEach(el => {
      if (!el.offsetParent || !el.textContent.trim() || el.closest('[disabled]')) return;
      const own = Array.from(el.childNodes).some(n => n.nodeType === 3 && n.textContent.trim());
      if (!own) return;
      const cs = getComputedStyle(el), fg = rgb(cs.color), b = bg(el);
      const l1 = lum(fg), l2 = lum(b), ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
      const large = parseFloat(cs.fontSize) >= 24 || (parseFloat(cs.fontSize) >= 18.66 && +cs.fontWeight >= 700);
      const need = large ? 3 : 4.5;
      if (ratio / need < worst.ratio / (worst.need || 1)) worst = { ratio: Math.round(ratio * 100) / 100, need, text: el.textContent.trim().slice(0, 40), cls: el.className };
    });
    return worst;
  }, selector);
}

async function run(browser, base, opts) {
  const tag = `${opts.w} ${opts.theme} ${opts.lang}`;
  const ctx = await browser.newContext({ viewport: { width: opts.w, height: 900 }, colorScheme: opts.theme });
  // WIKI_COOKIE="name=value": the login session of a deployed dev wiki (/dev/ sits behind the login)
  if (process.env.WIKI_COOKIE) {
    const [name, ...rest] = process.env.WIKI_COOKIE.split('=');
    await ctx.addCookies([{ name, value: rest.join('='), url: base }]);
  }
  const devSite = /\/dev\/?$/.test(new URL(base).pathname);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error' && !/Failed to load resource/.test(m.text())) errors.push('console: ' + m.text()); });
  page.on('response', r => { if (r.status() >= 400 && !IGNORED_404.test(r.url())) errors.push(r.status() + ' ' + r.url()); });
  page.on('requestfailed', r => { if (!IGNORED_404.test(r.url())) errors.push('failed ' + r.url()); });
  const q = s => base + s + (s.includes('?') ? '&' : '?') + 'lang=' + opts.lang;
  const h1 = async () => (await page.locator('#main h1').first().textContent()).trim();
  const phone = opts.w < 700;
  const openNav = async () => { if (phone) { await page.click('#menu-btn'); await page.waitForTimeout(250); } };
  const worst = [];

  // sidebar groups, active entries and old addresses
  await page.goto(q('?tab=start')); await page.locator('#main h1').waitFor();
  ok(/^(Getting started|Erste Schritte)$/.test(await h1()), tag + ' start page');
  await openNav();
  for (const g of ['browse', 'reference', 'progress']) ok(await page.locator(`#sidebar details[data-sb="${g}"]`).count() === 1, tag + ' group ' + g);
  ok(await page.locator('#sidebar details[data-sb="dev"]').count() === (devSite ? 1 : 0), tag + ' dev group only in dev');
  ok(await page.locator('#sidebar a[href*="tab=enchanting"]').count() === (devSite ? 1 : 0), tag + ' enchanting view only in dev');
  ok(await page.locator('#sidebar a.active[href*="tab=start"]').count() === 1, tag + ' start active');
  for (const t of ['submods', 'guidebook', 'questionary', 'items', 'allrecipes', 'trades', 'loot', 'advancements', 'config', 'commands', 'textures', 'tags'])
    ok(await page.locator(`#sidebar a[href*="tab=${t}"]`).count() === 1, tag + ' link ' + t);
  ok(await page.locator('#sidebar details[data-sb="reference"] a[href*="tab=blocks"]').count() === 0, tag + ' blocks merged into blocks & items');
  ok(await page.locator('#sidebar details[data-sb="progress"] a[href*="tab=tags"]').count() === 1, tag + ' tags under progress');
  worst.push(['sidebar', await contrast(page, '#sidebar a span, #sidebar summary span, #sidebar .n')]);
  if (!phone) {
    // collapse REFERENCE, reload: stays closed (localStorage), open again
    await page.click('#sidebar details[data-sb="reference"] > summary');
    await page.waitForTimeout(250);
    await page.reload(); await page.locator('#main h1').waitFor();
    ok(!(await page.locator('#sidebar details[data-sb="reference"]').evaluate(d => d.open)), tag + ' group state kept');
    await page.click('#sidebar details[data-sb="reference"] > summary');
  }
  for (const [old, want, active] of [['?tab=features', /^(Getting started|Erste Schritte)$/, 'start'], ['?tab=blocks', null, 'items'],
    ['?tab=inworld', null, 'allrecipes'], ['?tab=recipes', null, 'allrecipes'], ['?tab=enchanting', null, null]]) {
    await page.goto(q(old)); await page.locator('#main h1').waitFor();
    if (want) ok(want.test(await h1()), tag + ' old address ' + old);
    ok(!/not found|nicht gefunden/i.test(await page.locator('#main').textContent()), tag + ' old address works ' + old);
    if (active) ok(await page.locator(`#sidebar a.active[href*="tab=${active}"]`).count() === 1, tag + ' active for ' + old);
  }
  await page.goto(q('?tab=start&dev=1')); await page.locator('#main h1').waitFor();
  ok(await page.locator('#sidebar details[data-sb="dev"] a[href*="tab=enchanting"]').count() === 1, tag + ' dev group with enchanting view');

  // submods: open tree, a click leads to the mod's own wiki
  await page.goto(q('?tab=submods')); await page.locator('.sm-super').first().waitFor();
  const folds = await page.locator('details.sm-fold').count();
  ok(folds > 1 && await page.locator('details.sm-fold[open]').count() === folds, tag + ' submods all open');
  worst.push(['submods', await contrast(page, '.sm-mod span, .sm-n, .pb-tag')]);
  const sub = page.locator('.sm-tree .sm-mod').first();
  const href = await sub.getAttribute('href');
  ok(/mod=simple/.test(href), tag + ' submod links to its wiki');

  // guidebook: tabs, contents, turning pages (button, key, swipe-free), deep link
  await page.goto(q('?tab=guidebook')); await page.locator('.gb-book').waitFor();
  ok(await page.locator('.gb-tab').count() >= 2, tag + ' guidebook sections');
  ok(await page.locator('.gb-page').count() === (phone ? 1 : 2), tag + ' spread size');
  worst.push(['guidebook', await contrast(page, '.gb-page *, .gb-tab span, .gb-pos')]);
  const pos0 = await page.locator('.gb-pos').textContent();
  await page.click('[data-gb-turn="1"]'); await page.waitForTimeout(150);
  ok(await page.locator('.gb-pos').textContent() !== pos0, tag + ' next page');
  ok(/book=simplebuilding\/\d+/.test(page.url()), tag + ' page in the address');
  await page.keyboard.press('ArrowLeft'); await page.waitForTimeout(150);
  ok(await page.locator('.gb-pos').textContent() === pos0, tag + ' arrow key back');
  await page.locator('.gb-toc .gb-link').nth(1).click(); await page.waitForTimeout(150);
  ok(await page.locator('.gb-h, .gb-h3').count() > 0 && await page.locator('.gb-pos').textContent() !== pos0, tag + ' contents link');
  const deep = page.url();
  await page.reload(); await page.locator('.gb-book').waitFor();
  ok(page.url() === deep && await page.locator('.gb-pos').textContent() !== pos0, tag + ' deep link keeps the page');
  worst.push(['guidebook chapter', await contrast(page, '.gb-page *')]);
  await page.locator('.gb-back').first().click(); await page.waitForTimeout(150);
  ok(await page.locator('.gb-toc').count() > 0, tag + ' back to contents');
  const tabs = page.locator('.gb-tab');
  await tabs.nth(1).click(); await page.waitForTimeout(150);
  ok(await tabs.nth(1).getAttribute('aria-selected') === 'true', tag + ' section switch');

  // questionary: search, open an entry (three pages), families, fallback
  await page.goto(q('?tab=questionary')); await page.locator('.qn-grid').waitFor();
  const all = await page.locator('.qn-item').count();
  await page.fill('#qn-q', 'diamond'); await page.waitForTimeout(450);
  const some = await page.locator('.qn-item').count();
  ok(some > 0 && some < all, tag + ' questionary search');
  await page.locator('.qn-item').first().click(); await page.locator('#qn-open').waitFor();
  ok(await page.locator('#qn-open .gb-page').count() === 3, tag + ' three pages');
  ok(/questionary=/.test(page.url()), tag + ' entry in the address');
  worst.push(['questionary', await contrast(page, '#qn-open .gb-page *, .qn-item span, .qn-filters .chip')]);
  await page.click('[data-qn-close]'); await page.waitForTimeout(150);
  ok(await page.locator('#qn-open').count() === 0, tag + ' close');
  await page.click('[data-qn-view="families"]'); await page.waitForTimeout(150);
  ok(await page.locator('.qn-item').count() > 0, tag + ' families');
  await page.click('[data-qn-view="fallback"]'); await page.waitForTimeout(150);
  ok(await page.locator('details.section').count() >= 5, tag + ' fallback classes');

  // recipes: the type filter includes in-world transformations
  await page.goto(q('?tab=allrecipes')); await page.locator('#ar-results').waitFor();
  ok(await page.locator('[data-ar="st"][data-v="inworld"]').count() === 1, tag + ' recipes type filter has in-world');

  for (const [where, w] of worst) ok(w.ratio >= w.need || w.ratio === 99, `${tag} contrast ${where}: ${JSON.stringify(w)}`);
  assert.deepEqual(errors, [], tag + ' errors');
  await ctx.close();
  return worst;
}

(async () => {
  let base = process.env.WIKI_BASE;
  if (!base) { await new Promise(resolve => server.listen(0, '127.0.0.1', resolve)); base = `http://127.0.0.1:${server.address().port}/`; }
  let browser;
  try {
    const exe = process.env.WIKI_CHROME;
    browser = await pw.chromium.launch(exe ? { headless: true, executablePath: exe } : { headless: true, channel: process.env.WIKI_BROWSER || 'msedge' });
    const report = [];
    for (const w of [1280, 390]) for (const theme of ['light', 'dark']) for (const lang of ['en', 'de']) {
      const worst = await run(browser, base, { w, theme, lang });
      report.push(`${w} ${theme} ${lang}: ` + worst.map(([k, v]) => `${k} ${v.ratio}`).join(', '));
    }
    console.log(report.join('\n'));
    console.log(`wiki24 ui ok: ${checks} checks`);
  } finally {
    if (browser) await browser.close();
    server.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
