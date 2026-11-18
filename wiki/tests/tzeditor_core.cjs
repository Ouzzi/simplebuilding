// Logic of wiki/assets/tzeditor.js under node (run by wiki/tests/test_tzeditor.py): column move, save payload, duplicates.
const assert = require('node:assert/strict');
const path = require('node:path');
const T = require(path.resolve(__dirname, '..', 'assets', 'tzeditor.js'));
const cols = ['button', 'plate', 'storage'];
const rows = [{ key: 'iron', label: 'Iron', cells: ['minecraft:iron_button', null, '#red'], tops: { storage: 'x:octet', gone: 'x:y' } }];
T.moveColumn(cols, rows, 0, 2);
assert.deepEqual(cols, ['plate', 'storage', 'button']);
assert.deepEqual(rows[0].cells, [null, '#red', 'minecraft:iron_button']);
const save = T.layoutOf({ variant: 'lines', area: 'metals', excluded: ['a:b'] }, cols, rows);
assert.deepEqual(save.rows[0].tops, { storage: 'x:octet' });   // tops of unknown columns are dropped
assert.deepEqual(save.excluded, ['a:b']);
assert.deepEqual(T.placedCount([{ cells: ['a:a', 'a:a', '#red', null] }]), { 'a:a': 2 });
assert.ok(T.STR.en.title && T.STR.de.title);
assert.deepEqual(Object.keys(T.STR.en).sort(), Object.keys(T.STR.de).sort());
console.log('tzeditor core ok');

// N72b: menu actions (insert row/column, frame toggle), undo over several areas, as-built arrangement, compact check.
const area = { cols: ['a', 'b'], rows: [{ key: 'r1', label: 'R1', cells: ['x:1', null] }, { key: 'r2', label: 'R2', cells: ['#red', 'x:2'] }] };
const other = { cols: ['c'], rows: [{ key: 'o', label: 'O', cells: ['y:1'] }] };
const all = { f1: area, f2: other };
const H = new T.History(5);
H.record({ f1: area });
assert.equal(T.insertRow(area, 1, 'New row'), 'new_row');
assert.deepEqual(area.rows.map(r => r.key), ['r1', 'new_row', 'r2']);
assert.deepEqual(area.rows[1].cells, [null, null]);                       // as many cells as columns (check_save)
H.record({ f1: area });
assert.equal(T.insertColumn(area, 1, 'Neu Spalte!'), 'neu_spalte');
assert.deepEqual(area.cols, ['a', 'neu_spalte', 'b']);
assert.deepEqual(area.rows[0].cells, ['x:1', null, null]);
assert.equal(T.insertColumn(area, 0, 'a'), 'a_2');                        // unique names
H.record({ f1: area, f2: other });                                        // one step over two areas (overview)
area.rows[0].cells[0] = '#light'; other.rows[0].cells[0] = '#red';
assert.deepEqual(H.undo(all).sort(), ['f1', 'f2']);
assert.equal(all.f1.rows[0].cells[1], 'x:1'); assert.equal(all.f2.rows[0].cells[0], 'y:1');
H.redo(all);
assert.equal(all.f2.rows[0].cells[0], '#red');
H.undo(all); H.undo(all); H.undo(all);
assert.deepEqual(all.f1.cols, ['a', 'b']); assert.deepEqual(all.f1.rows.map(r => r.key), ['r1', 'r2']);  // back to the start
assert.equal(H.undo(all), null);
// frame: only on fields without an item
assert.equal(T.toggleFrame(null), '#frame'); assert.equal(T.toggleFrame('#frame'), null);
assert.equal(T.toggleFrame('#light'), '#frame'); assert.equal(T.toggleFrame('x:1'), 'x:1');
// the inserted row/column survive the save payload unchanged
const saved = T.layoutOf({ variant: 'lines', area: 'metals' }, ['a', 'n'], [{ key: 'new_row', label: 'New row', cells: [null, '#frame'], tops: {} }]);
assert.deepEqual(saved.rows[0], { key: 'new_row', label: 'New row', cells: [null, '#frame'] });
// as built: side A metals/wood/aging/rest + last stone families, cut on a family border, both sides about equal
const mk = (n, fam) => Array.from({ length: n }, (_, i) => ({ key: fam + i, family: fam }));
const bp = T.builtPlan({ metals: mk(1, 'm'), wood: mk(1, 'w'), aging: [], rest: [], stone: mk(3, 'a').concat(mk(5, 'b')) }, 114);
assert.deepEqual(bp.b.map(r => r.family).join(''), 'aaa');                 // cut at the a|b border (4 would be inside b)
assert.deepEqual(bp.a.map(r => r.family).join(''), 'mwbbbbb');             // the last stone family follows on side A
assert.equal(bp.bands.length, 1);
const even = T.builtPlan({ metals: mk(6, 'm'), stone: mk(4, 'a').concat(mk(4, 'b')) }, 114);
assert.equal(even.b.length, 8);                                             // 6 | 8 is closer than 10 | 4
const bp2 = T.builtPlan({ metals: mk(5, 'm'), stone: mk(5, 's') }, 3);
assert.equal(bp2.bands.length, 2);                                          // more rows than a pair holds: further pairs
// compact: ids of the game plan without a field
assert.deepEqual(T.unplaced({ x: ['x:1', 'x:9'], y: ['y:1'] }, [{ cells: ['x:1', 'y:1'] }]), ['x:9']);
assert.equal(T.STR.de.mMissing, 'Als fehlend markieren (rot)');
console.log('tzeditor N72b core ok');

// N99: group move with push-back (nothing lost), range, copy/paste, saved-vs-online helpers
{
  const g = { f: { rows: [['a', 'b', 'c', null], ['d', null, 'e', 'x']] } };
  const get = (f, r, c) => g[f].rows[r][c];
  const inside = (f, r, c) => !!g[f] && r >= 0 && c >= 0 && r < g[f].rows.length && c < g[f].rows[0].length;
  const apply = plan => { const v = plan.map(x => x.v); plan.forEach((x, i) => { g[x.f].rows[x.r][x.c] = v[i]; }); };
  apply(T.planMove([{ f: 'f', r: 0, c: 0 }], 'f', 1, 2, get, inside));                       // single cell = swap
  assert.deepEqual(g.f.rows, [['e', 'b', 'c', null], ['d', null, 'a', 'x']]);
  apply(T.planMove([{ f: 'f', r: 0, c: 0 }, { f: 'f', r: 0, c: 1 }, { f: 'f', r: 0, c: 2 }], 'f', 0, 1, get, inside));  // run shifts right, pushed field wraps to the left
  assert.deepEqual(g.f.rows[0], [null, 'e', 'b', 'c']);
  assert.equal(T.planMove([{ f: 'f', r: 1, c: 3 }], 'f', 0, 1, get, inside), null);           // beyond the edge
  const flat = g.f.rows.flat().filter(Boolean).sort();
  apply(T.planMove([{ f: 'f', r: 0, c: 1 }, { f: 'f', r: 1, c: 1 }], 'f', 0, 2, get, inside));
  assert.deepEqual(g.f.rows.flat().filter(Boolean).sort(), flat);                              // no id lost or doubled
  assert.equal(T.rectOf({ f: 'f', r: 2, c: 3 }, { f: 'f', r: 1, c: 1 }).length, 6);
  const clip = T.copyOf([{ f: 'f', r: 1, c: 2 }, { f: 'f', r: 0, c: 3 }], get);
  assert.deepEqual(clip.map(p => [p.dr, p.dc]), [[1, 0], [0, 1]]);
  assert.equal(T.pasteAt(clip, 'f', 0, 3, inside).length, 0 + 1);                            // cut off at the edge
  const cols = ['a', 'b'], rows = [{ key: 'k', label: 'K', cells: ['x:1', null], tops: { b: 'x:o' } }];
  assert.equal(T.layoutKey(cols, rows), T.layoutKey(['a', 'b'], [{ key: 'k', label: 'K', cells: ['x:1', undefined], tops: { b: 'x:o' } }]));
  assert.notEqual(T.layoutKey(cols, rows), T.layoutKey(cols, [{ key: 'k', label: 'K', cells: [null, 'x:1'], tops: { b: 'x:o' } }]));
  const base = T.baseMapOf(cols, rows);
  assert.equal(T.differs(base, 'k', 'a', 'x:1'), false); assert.equal(T.differs(base, 'k', 'b', 'x:1'), true);
  assert.equal(T.differs(base, 'new', 'a', null), false); assert.equal(T.differs(base, 'new', 'a', '#red'), true);
  assert.equal(T.differs(null, 'k', 'a', 'z'), false);
  const list = [{ sid: '17', variant: 'lines', area: 'wood' }, { sid: '19', variant: 'lines', area: 'wood' }, { sid: '20', variant: 'lines', area: 'stone' }];
  assert.equal(T.newestSave(list, 'lines', 'wood').sid, '19');
  assert.equal(T.newestSave(list, 'blocks', 'wood'), null);
  assert.equal(T.newestSave(null, 'lines', 'wood'), null);
  console.log('tzeditor N99 core ok');
}
