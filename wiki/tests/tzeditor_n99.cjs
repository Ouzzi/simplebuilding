// N99 browser steps of the test centre editor (wiki/assets/tzeditor.js), shared by the local harness wiki/tests/tzeditor_ui.cjs
// and the dev click test (/root/n74dev/n99_editor.cjs): timings of click / mark (all views), group move with live preview,
// swap + Esc, row and column move, undo/redo (buttons, Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z), copy/paste, frame selection,
// Shift range, arrow keys, Esc. Every step leaves the data as it found it (undo), nothing is saved here.
// run(page, { ok, shot(name), log }) -> { timings, counts }
const cellSel = (r, c, f) => `.tz-body .tz-c${f ? `[data-f="${f}"]` : ''}[data-r="${r}"][data-c="${c}"]`;

async function center(page, sel) {
  const loc = page.locator(sel).first();
  await loc.scrollIntoViewIfNeeded();
  const b = await loc.boundingBox();
  return { x: b.x + b.width / 2, y: b.y + b.height / 2 };
}
const idAt = (page, r, c, f) => page.locator(cellSel(r, c, f)).first().getAttribute('data-id');
const ids = (page, list, f) => Promise.all(list.map(([r, c]) => idAt(page, r, c, f)));
async function drag(page, from, to, opt = {}) {
  const a = await center(page, from), b = await center(page, to);
  await page.mouse.move(a.x, a.y); await page.mouse.down();
  await page.mouse.move(a.x + 8, a.y + 3, { steps: 2 });
  await page.mouse.move(b.x, b.y, { steps: 6 });
  await page.waitForTimeout(80);
  if (opt.during) await opt.during();
  if (opt.esc) { await page.keyboard.press('Escape'); await page.waitForTimeout(50); }
  await page.mouse.up();
  await page.waitForTimeout(80);
}

/* timing of a click / Ctrl-click inside the page: the handler and the time until the next frame */
async function timeClicks(page, n, mod) {
  return page.evaluate(async ([n, mod]) => {
    const m = document.querySelector('.tz-main').getBoundingClientRect(), vh = window.innerHeight;  // visible cells only: no scrolling inside the timing
    const cells = [...document.querySelectorAll('.tz-body .tz-c[data-f]:not(.tz-ro)')].filter(e => { const r = e.getBoundingClientRect(); return r.left >= m.left && r.right <= m.right && r.top >= Math.max(0, m.top) && r.bottom <= Math.min(m.bottom, vh); });
    const out = [];
    for (let i = 0; i < n; i++) {
      const e = cells[Math.floor((i + 1) * cells.length / (n + 2))];
      const r = e.getBoundingClientRect(), o = { bubbles: true, cancelable: true, button: 0, pointerId: 1, isPrimary: true, clientX: r.left + 3, clientY: r.top + 3, ctrlKey: !!mod };
      const t0 = performance.now();
      e.dispatchEvent(new PointerEvent('pointerdown', o)); document.dispatchEvent(new PointerEvent('pointerup', o));
      const t1 = performance.now();
      await new Promise(res => requestAnimationFrame(() => setTimeout(res, 0)));
      out.push([Math.round((t1 - t0) * 10) / 10, Math.round(performance.now() - t0)]);
    }
    return out;
  }, [n, mod]);
}

/* the pictures of a freshly drawn view load and decode for a while; the timing is about the click, not that */
const settled = page => page.waitForFunction(() => [...document.images].every(i => i.complete), null, { timeout: 60000 }).catch(() => {}).then(() => page.waitForTimeout(300));

async function run(page, { ok, shot = async () => {}, log = console.log }) {
  const res = { timings: {}, counts: {} };
  const L = await page.evaluate(() => window.SBTzEditor.STR[document.documentElement.lang === 'de' ? 'de' : 'en']);
  // ---- timings in all three views
  await page.selectOption('select[data-k=layout]', 'built');
  await page.locator('.tz-bg .tz-c[data-f]').first().waitFor({ timeout: 60000 });
  await settled(page);
  res.timings.built = { click: await timeClicks(page, 4), mark: await timeClicks(page, 6, true) };
  await page.selectOption('select[data-k=layout]', 'table');
  await page.selectOption('select[data-k=area]', '*all');
  await page.locator('.tz-all .tz-area').first().waitFor({ timeout: 60000 });
  await settled(page);
  const allClick = await timeClicks(page, 4);
  await page.keyboard.press('Escape');
  res.timings.all = { click: allClick, mark: await timeClicks(page, 10, true) };
  res.counts.allCells = await page.locator('.tz-body .tz-c').count();
  const marked = await page.locator('.tz-body .tz-c.sel').count();
  ok(marked === 10, `all areas: ${marked} cells marked by Ctrl-click`);
  // Shift range inside one area of the overview, timed
  const f0 = await page.locator('.tz-all .tz-area').first().getAttribute('data-f');
  await page.locator(cellSel(0, 0, f0)).click();
  const t0 = Date.now();
  await page.locator(cellSel(3, 4, f0)).click({ modifiers: ['Shift'] });
  res.timings.allShiftRange = Date.now() - t0;
  ok(await page.locator('.tz-body .tz-c.sel').count() === 20, 'Shift-click selects the 4 x 5 range');
  await shot('n99-all-range');
  await page.keyboard.press('Escape');
  ok(await page.locator('.tz-body .tz-c.sel').count() === 0, 'Esc clears the selection');
  const all = res.timings.all;
  const worst = Math.max(...all.click.map(x => x[1]), ...all.mark.map(x => x[1]));
  res.timings.allWorst = worst;
  log('timings', JSON.stringify(res.timings));

  // ---- table of the first area (metals)
  await page.selectOption('select[data-k=area]', f0);
  await page.waitForFunction(() => !document.querySelector('.tz-all') && document.querySelector('.tz-body .tz-grid'), null, { timeout: 60000 });
  await settled(page);
  const undoBtn = page.locator('[data-act=undo]'), redoBtn = page.locator('[data-act=redo]');
  const snapshot = () => page.$$eval('.tz-body .tz-c[data-f]', es => es.map(e => e.dataset.id).join('|'));
  const start = await snapshot();
  ok(await undoBtn.isDisabled(), 'undo button disabled while there is nothing to undo');

  // group move: (0,1)+(0,2) by two rows down; the two fields there swap into the group's place; live preview first
  const before = await ids(page, [[0, 1], [0, 2], [2, 1], [2, 2]]);
  await page.locator(cellSel(0, 1)).click();
  await page.locator(cellSel(0, 2)).click({ modifiers: ['Control'] });
  let preview = null;
  await drag(page, cellSel(0, 1), cellSel(2, 1), { during: async () => {
    preview = { ids: await ids(page, [[2, 1], [2, 2], [0, 1], [0, 2]]), pv: await page.locator('.tz-body .tz-c.tz-pv').count() };
    await shot('n99-group-preview');
  } });
  ok(preview.pv === 4 && preview.ids.join() === [before[0], before[1], before[2], before[3]].join(), 'group move: live preview shows the result before release');
  const after = await ids(page, [[2, 1], [2, 2], [0, 1], [0, 2]]);
  ok(after.join() === [before[0], before[1], before[2], before[3]].join(), 'group move committed on release (pushed fields take the free places)');
  ok(await page.locator('.tz-body .tz-c.sel').count() === 2 && await page.locator(cellSel(2, 1) + '.sel').count() === 1, 'the selection moved along');
  await undoBtn.click();
  ok((await ids(page, [[0, 1], [0, 2], [2, 1], [2, 2]])).join() === before.join(), 'undo (button) restores the group move');
  await page.keyboard.press('Control+Shift+z');
  ok((await ids(page, [[2, 1], [2, 2]])).join() === [before[0], before[1]].join(), 'Ctrl+Shift+Z redoes');
  await page.keyboard.press('Control+z');
  await redoBtn.click();
  ok((await ids(page, [[2, 1], [2, 2]])).join() === [before[0], before[1]].join(), 'redo (button)');
  await page.keyboard.press('Control+z');
  ok((await ids(page, [[0, 1], [0, 2], [2, 1], [2, 2]])).join() === before.join(), 'Ctrl+Z restores again');

  // live swap on an occupied field + Esc
  await page.keyboard.press('Escape');
  const s0 = await ids(page, [[0, 3], [1, 3]]);
  let live = null;
  await drag(page, cellSel(0, 3), cellSel(1, 3), { esc: true, during: async () => { live = await ids(page, [[1, 3], [0, 3]]); await shot('n99-swap-live'); } });
  ok(live.join() === [s0[0], s0[1]].join(), 'live swap shown while dragging onto an occupied field');
  ok((await ids(page, [[0, 3], [1, 3]])).join() === s0.join() && await page.locator('.tz-body .tz-c.tz-pv').count() === 0, 'Esc cancels: nothing changed, no preview left');
  await drag(page, cellSel(0, 3), cellSel(1, 3));
  ok((await ids(page, [[0, 3], [1, 3]])).join() === [s0[1], s0[0]].join(), 'swap committed on release');
  await page.keyboard.press('Control+z');
  ok((await ids(page, [[0, 3], [1, 3]])).join() === s0.join(), 'swap undone');

  // rows and columns
  const rows0 = await page.locator('.tz-body .tz-r').allTextContents();
  await drag(page, '.tz-body .tz-r[data-row="0"]', '.tz-body .tz-r[data-row="2"]', { during: () => shot('n99-row-drag') });
  const rows1 = await page.locator('.tz-body .tz-r').allTextContents();
  ok(rows1[2] === rows0[0] && rows1[0] === rows0[1] && rows1[1] === rows0[2], 'row 0 moved to place 2');
  await page.keyboard.press('Control+z');
  ok((await page.locator('.tz-body .tz-r').allTextContents()).join() === rows0.join(), 'row move undone');
  const cols0 = await page.locator('.tz-body .tz-h').allTextContents(), c0cells = await ids(page, [[0, 0], [1, 0]]);
  await drag(page, '.tz-body .tz-h[data-col="0"]', '.tz-body .tz-h[data-col="2"]');
  const cols1 = await page.locator('.tz-body .tz-h').allTextContents();
  ok(cols1[2] === cols0[0] && (await ids(page, [[0, 2], [1, 2]])).join() === c0cells.join(), 'column 0 moved to place 2 with its cells');
  await page.keyboard.press('Control+z');
  ok((await page.locator('.tz-body .tz-h').allTextContents()).join() === cols0.join(), 'column move undone');

  // copy / paste (keys and buttons)
  const src = await idAt(page, 0, 1), dstOld = await ids(page, [[4, 4], [4, 5]]);
  await page.locator(cellSel(0, 1)).click();
  await page.locator(cellSel(0, 2)).click({ modifiers: ['Control'] });
  await page.keyboard.press('Control+c');
  const pair = await ids(page, [[0, 1], [0, 2]]);
  await page.locator(cellSel(4, 4)).click();
  await page.keyboard.press('Control+v');
  ok((await ids(page, [[4, 4], [4, 5]])).join() === pair.join(), 'Ctrl+C / Ctrl+V pastes the pattern at the focus');
  await undoBtn.click();
  ok((await ids(page, [[4, 4], [4, 5]])).join() === dstOld.join(), 'paste undone');
  await page.locator(cellSel(0, 1)).click();
  await page.locator('[data-act=copy]').click();
  await page.locator(cellSel(5, 5)).click();
  const old55 = await idAt(page, 5, 5);
  await page.locator('[data-act=paste]').click();
  ok(await idAt(page, 5, 5) === src, 'copy / paste buttons');
  await page.keyboard.press('Control+z');
  ok(await idAt(page, 5, 5) === old55, 'button paste undone');

  // frame selection (Alt+drag), Shift range, clear button
  await page.keyboard.down('Alt');
  await drag(page, cellSel(1, 1), cellSel(3, 3), { during: () => shot('n99-frame') });
  await page.keyboard.up('Alt');
  ok(await page.locator('.tz-body .tz-c.sel').count() === 9, 'Alt+drag frames 3 x 3 fields');
  await page.locator('[data-act=selnone]').click();
  ok(await page.locator('.tz-body .tz-c.sel').count() === 0, 'button clears the selection');
  await page.locator(cellSel(1, 1)).click();
  await page.locator(cellSel(2, 4), {}).click({ modifiers: ['Shift'] });
  ok(await page.locator('.tz-body .tz-c.sel').count() === 8, 'Shift-click selects the 2 x 4 range');

  // arrow keys: focus, Shift+arrows extend, Ctrl+arrows move (swap), undo
  await page.locator(cellSel(1, 1)).click();
  const a0 = await ids(page, [[1, 1], [1, 2]]);
  await page.keyboard.press('Control+ArrowRight');
  ok((await ids(page, [[1, 1], [1, 2]])).join() === [a0[1], a0[0]].join() && await page.locator(cellSel(1, 2) + '.sel').count() === 1, 'Ctrl+arrow moves the selection (swap) and keeps it selected');
  await page.keyboard.press('Control+z');
  ok((await ids(page, [[1, 1], [1, 2]])).join() === a0.join(), 'arrow move undone');
  await page.locator(cellSel(1, 1)).click();
  await page.keyboard.press('ArrowDown');
  ok(await page.locator(cellSel(2, 1) + '.sel').count() === 1, 'arrow moves the focus');
  await page.keyboard.press('Shift+ArrowRight'); await page.keyboard.press('Shift+ArrowRight');
  ok(await page.locator('.tz-body .tz-c.sel').count() === 3, 'Shift+arrows extend the selection');
  await page.keyboard.press('Escape');
  while (!(await undoBtn.isDisabled())) await undoBtn.click();
  ok(await snapshot() === start, 'undo to the start: the table is as it was');
  // a single click stays fast in the single table too
  res.timings.table = { click: await timeClicks(page, 4), mark: await timeClicks(page, 6, true) };
  await page.keyboard.press('Escape');
  ok(L.keysHelp.length > 20, 'key help shown');
  return res;
}

module.exports = { run, timeClicks, cellSel, drag, center, idAt };
