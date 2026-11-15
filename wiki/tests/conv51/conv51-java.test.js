// node --test wiki/tests/conv51/   (N51 phase 3): the game is the reference. Vectors from the gametest
// simplebuildingtools:module_game_test_converter_data_dump (Conv51Dump.java, BlueprintCode.serialize of fixed models with full block
// states) via tools/conv51/make_data.py --dump; the JS serializer with the game's default table must write the same text.
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('fs'), path = require('path');
const C = require('../../assets/conv/conv-core.js');
const V = path.join(__dirname, 'vectors/java-vectors.json'), B = path.join(__dirname, '../../../tools/conv51/data/blocks-26.3.json');

function jsModel(v) {
  const info = C.newInfo('vector'), specs = v.states.map((s) => { const r = C.parseState(s); return C.spec(r.id, r.props, info); });
  // like BlueprintModel: header = bounding box (max - min + 1), positions stay absolute (the JS readers always normalise first)
  const cells = new Map(), lo = [Infinity, Infinity, Infinity], hi = [-1, -1, -1];
  for (const [x, y, z, i] of v.cells) { cells.set(C.key(x, y, z), specs[i]); [x, y, z].forEach((c, a) => { lo[a] = Math.min(lo[a], c); hi[a] = Math.max(hi[a], c); }); }
  return { cells, n: cells.size, sx: hi[0] - lo[0] + 1, sy: hi[1] - lo[1] + 1, sz: hi[2] - lo[2] + 1, info };
}

test('Java vectors exist (hut, random, wrap/alias, every block default + random)', () => {
  const vs = JSON.parse(fs.readFileSync(V, 'utf8')), names = vs.map((v) => v.name);
  for (const n of ['hut', 'random_0', 'wrap_alias', 'every_block_default', 'every_block_random']) assert.ok(names.includes(n), n);
  assert.ok(vs.find((v) => v.name === 'every_block_default').cells.length > 1000);
});

for (const v of fs.existsSync(V) ? JSON.parse(fs.readFileSync(V, 'utf8')) : []) {
  test('JS serialize == Java BlueprintCode.serialize: ' + v.name + ' (' + v.cells.length + ' blocks)', () => {
    const js = C.serialize(jsModel(v));
    if (js !== v.code) {
      const a = js.split('\n'), b = v.code.split('\n'); let i = 0; while (i < a.length && a[i] === b[i]) i++;
      assert.fail('first difference in line ' + (i + 1) + ':\n  js:   ' + a[i] + '\n  java: ' + b[i]);
    }
  });
}

test('conv-data.js holds the game defaults and items of every dumped block', () => {
  const game = JSON.parse(fs.readFileSync(B, 'utf8')).blocks;
  for (const [id, e] of Object.entries(game)) {
    const r = C.parseState(id + '[' + e.d + ']');
    assert.equal(C.spec(r.id, r.props), id.replace(/^minecraft:/, ''), id + ' default state must be written bare');
    assert.ok(C.isKnown(id), id + ' known');
    const noItem = e.item === false && !/^minecraft:(air|cave_air|void_air)$/.test(id);
    const m = { cells: new Map([[0, id.replace(/^minecraft:/, '')]]), n: 1, sx: 1, sy: 1, sz: 1, info: C.newInfo('t') };
    C.markModel(m);
    assert.equal(!!m.info.noItem[id], noItem, id + ' item flag');
  }
});
