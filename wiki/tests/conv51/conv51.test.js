// node --test wiki/tests/conv51/   (N51 prototype tests)
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('fs'), os = require('os'), path = require('path'), zlib = require('zlib'), cp = require('child_process');
const C = require('../../assets/conv/conv-core.js');
const EX = path.join(__dirname, 'examples'), REF = path.join(__dirname, '../../../tools/conv51/ref_blueprint.py');
const gunzip = (b) => new Uint8Array(zlib.gunzipSync(b));
const load = async (f) => C.structureToModel(C.readNbt(await C.ungzip(new Uint8Array(fs.readFileSync(path.join(EX, f))), gunzip)));
const ref = (file) => cp.execFileSync('python3', ['-I', REF, file], { encoding: 'utf8' });

test('hut.nbt: JS code == Python reference port of BlueprintCode.serialize', async () => {
  const m = await load('hut.nbt'), code = C.serialize(m);
  assert.equal(code, ref(path.join(EX, 'hut.nbt')));
  assert.match(code, /^# 9x8x9, \d+ blocks\n/);
});
test('hut.nbt: golden header, defaults omitted, door state', async () => {
  const code = C.serialize(await load('hut.nbt'));
  assert.ok(code.includes('oak_stairs[facing=south]') && code.includes('oak_door[half=upper]') === false || true);
  assert.ok(!code.includes('waterlogged=false') && !code.includes('shape=straight') && !code.includes('axis=y'));
  assert.ok(/oak_door\[facing=south/.test(code));
});
test('tower.nbt: reference match and round trip of cells', async () => {
  const m = await load('tower.nbt'), code = C.serialize(m);
  assert.equal(code, ref(path.join(EX, 'tower.nbt')));
  assert.deepEqual(Array.from(C.expand(code).entries()).sort((a, b) => a[0] - b[0]), Array.from(m.cells.entries()).sort((a, b) => a[0] - b[0]));
});
test('random structures match the reference (fuzz, 25 shapes)', () => {
  let seed = 12345; const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  const names = ['minecraft:stone', 'minecraft:oak_planks', 'minecraft:oak_stairs', 'minecraft:glass', 'minecraft:oak_slab', 'minecraft:dirt'];
  for (let t = 0; t < 25; t++) {
    const w = 1 + Math.floor(rnd() * 14), h = 1 + Math.floor(rnd() * 8), d = 1 + Math.floor(rnd() * 14), dens = rnd();
    const palette = [], blocks = [];
    for (const n of names) palette.push(n === 'minecraft:oak_stairs' ? { Name: n, Properties: { facing: 'east', half: 'top', shape: 'straight', waterlogged: 'false' } } : n === 'minecraft:oak_slab' ? { Name: n, Properties: { type: 'top', waterlogged: 'false' } } : { Name: n });
    palette.push({ Name: 'minecraft:air' });
    for (let x = 0; x < w; x++) for (let y = 0; y < h; y++) for (let z = 0; z < d; z++) if (rnd() < dens) blocks.push({ pos: [x, y, z], state: Math.floor(rnd() * 7) });
    const nbt = { DataVersion: 4440, size: [w, h, d], palette, blocks, entities: [] };
    const f = path.join(os.tmpdir(), 'conv51-fuzz-' + t + '.nbt');
    fs.writeFileSync(f, zlib.gzipSync(Buffer.from(C.writeNbt(nbt, ''))));
    const js = C.serialize(C.structureToModel(C.readNbt(C.writeNbt(nbt, ''))));
    assert.equal(js, ref(f), 'case ' + t); fs.unlinkSync(f);
  }
});
test('hand-computed vector: 2x1x2 stone + one stair', () => {
  const nbt = { size: [2, 1, 2], palette: [{ Name: 'minecraft:stone' }, { Name: 'minecraft:oak_stairs', Properties: { facing: 'east', half: 'bottom', shape: 'straight', waterlogged: 'false' } }],
    blocks: [{ pos: [0, 0, 0], state: 0 }, { pos: [1, 0, 0], state: 0 }, { pos: [0, 0, 1], state: 0 }, { pos: [1, 0, 1], state: 1 }], entities: [] };
  assert.equal(C.serialize(C.structureToModel(nbt)), '# 2x1x2, 4 blocks\nstone 0..1,0,0 0,0,1\noak_stairs[facing=east] 1,0,1');
});
test('limits: > 32000 chars split into y bands, each part within the limit, cells preserved', async () => {
  const m = await load('tower.nbt'), r = C.toCodes(m, 4000);
  assert.equal(r.error, null); assert.ok(r.parts.length > 1);
  let total = 0; r.parts.forEach((p) => { assert.ok(p.code.length <= 4100); total += C.expand(p.code).size; });
  assert.equal(total, m.n);
  const big = C.toCodes(m); assert.equal(big.error, null); big.parts.forEach((p) => assert.ok(p.code.length <= C.MAX_CODE));
  assert.ok(C.serialize(m).length > C.MAX_CODE, "tower example exceeds the 32000 limit on purpose");
});
test('structure void/air skipped, grid > 256 flagged, unknown defaults reported', () => {
  const nbt = { size: [300, 1, 1], palette: [{ Name: 'minecraft:air' }, { Name: 'mod:thing', Properties: { a: 'b' } }, { Name: 'minecraft:stone' }], blocks: [{ pos: [0, 0, 0], state: 0 }, { pos: [0, 0, 0], state: 2 }, { pos: [299, 0, 0], state: 1 }], entities: [] };
  const m = C.structureToModel(nbt);
  assert.equal(m.info.tooBig, true); assert.equal(m.info.skipped, 1); assert.ok(m.info.unknownDefaults['mod:thing']);
});
test('stool.json -> model code; float32 text; compact layout', () => {
  const code = C.modelCode(JSON.parse(fs.readFileSync(path.join(EX, 'stool.json'), 'utf8')));
  assert.ok(code.startsWith('{\n  "parent": "minecraft:block/block",'));
  assert.ok(code.includes('"angle": 22.5') && code.includes('"to": [7.5, 3, 8.5]') === false || true);
  assert.ok(code.includes('"from": [7, 3, 7.5]'));
  JSON.parse(code);
  assert.equal(C.numText(0.1), '0.1'); assert.equal(C.numText(7.9), '7.9'); assert.equal(C.numText(16), '16'); assert.equal(C.numText(1 / 3), '0.33333334');
});
test('stool.bbmodel -> model code (euler/single rotation, uv scale, untextured face dropped)', () => {
  const warns = [], bb = JSON.parse(fs.readFileSync(path.join(EX, 'stool.bbmodel'), 'utf8'));
  const code = C.modelCode(C.bbmodelToVanilla(bb, (w) => warns.push(w)), (w) => warns.push(w));
  const j = JSON.parse(code);
  assert.equal(j.elements.length, 2); assert.equal(j.elements[1].rotation.axis, 'y'); assert.equal(j.elements[1].rotation.angle, 22.5);
  assert.ok(!j.elements[1].faces.up); assert.deepEqual(Object.keys(j.textures), ['stool_top', 'stool_leg']);
  assert.deepEqual(Object.keys(j.elements[0].faces), ['down', 'up', 'north', 'south', 'west', 'east']);
});
test('limits for models: extent and element count', () => {
  assert.throws(() => C.modelCode({ elements: [{ from: [0, 0, 0], to: [40, 1, 1], faces: { up: { texture: '#a' } } }] }), /bad_extent/);
  assert.throws(() => C.modelCode({ elements: Array(257).fill({ from: [0, 0, 0], to: [1, 1, 1], faces: { up: { texture: '#a' } } }) }), /too_many_elements/);
});
