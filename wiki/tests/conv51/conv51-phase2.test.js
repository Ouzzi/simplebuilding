// node --test wiki/tests/conv51/   (N51 phase 2: all building formats, renames, tiles, code parser, way back, meshes)
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('fs'), os = require('os'), path = require('path'), zlib = require('zlib'), cp = require('child_process');
const C = require('../../assets/conv/conv-core.js');
require('../../assets/conv/conv-formats.js'); require('../../assets/conv/conv-lang.js'); require('../../assets/conv/conv-mesh.js');
const EX = path.join(__dirname, 'examples'), REF = path.join(__dirname, '../../../tools/conv51/ref_blueprint.py');
const gunzip = (b) => new Uint8Array(zlib.gunzipSync(b)), gz = (b) => new Uint8Array(zlib.gzipSync(Buffer.from(b)));
const bytes = (f) => new Uint8Array(fs.readFileSync(path.join(EX, f)));
const read = (f, opts) => C.readFile(f, bytes(f), opts || {}, gunzip);
const ref = (file) => cp.execFileSync('python3', ['-I', REF, file], { encoding: 'utf8' });
const tmp = (name, data) => { const f = path.join(os.tmpdir(), 'conv51-' + process.pid + '-' + name); fs.writeFileSync(f, data); return f; };
const cellsOf = (m) => Array.from(m.cells.entries()).sort((a, b) => a[0] - b[0]);

test('every hut format reads to the same code as hut.nbt and matches the Python reference', async () => {
  const base = C.serialize(await read('hut.nbt'));
  for (const f of ['hut.schem', 'hut_v2.schem', 'hut_v1.schem', 'hut.litematic']) {
    const m = await read(f);
    assert.equal(C.serialize(m), base, f);
    assert.equal(C.serialize(m), ref(path.join(EX, f)), f + ' vs reference');
  }
  assert.equal((await read('hut.schem')).info.version, 3);
  assert.equal((await read('hut_v2.schem')).info.version, 2);
  assert.equal((await read('hut_v1.schem')).info.dataVersion, null);
});

test('litematica: regions listed (negative size handled), one region on its own', async () => {
  const all = await read('hut.litematic');
  assert.deepEqual(all.info.regions.map((r) => [r.name, r.min.join(','), r.size.join(',')]), [['walls', '0,0,0', '9,4,9'], ['roof', '0,4,0', '9,4,9']]);
  const roof = await read('hut.litematic', { region: 'roof' }), walls = await read('hut.litematic', { region: 'walls' });
  assert.equal(roof.n + walls.n, all.n); assert.ok(roof.n > 0 && walls.n > 0);
  await assert.rejects(read('hut.litematic', { region: 'nope' }), /litematic_no_region/);
});

test('MCEdit .schematic: 1.12 ids and meta mapped (stairs, logs, slabs, torch, door halves, sunflower), reference equal', async () => {
  const m = await read('mini.schematic'), code = C.serialize(m);
  assert.equal(code, ref(path.join(EX, 'mini.schematic')));
  for (const s of ['oak_stairs[facing=west,half=top] 4,1,0', 'spruce_log[axis=x] 2,1,0', 'oak_wood 3,1,0', 'cobblestone_slab[type=top] 0,1,1', 'birch_slab 1,1,1',
    'wall_torch[facing=south] 2,1,1', 'torch 3,1,1', 'oak_door[open=true] 4,1,2', 'oak_door[half=upper,hinge=right,open=true] 4,2,2',
    'sunflower 0,1,2', 'sunflower[half=upper] 0,2,2', 'red_wool 0,1,0', 'blue_wool 1,1,0', 'yellow_terracotta 1,1,2', 'chiseled_stone_bricks 2,1,2'])
    assert.ok(code.includes(s), s);
  assert.equal(m.info.approx, 0); assert.equal(m.info.noItem['minecraft:water'], 1); assert.deepEqual(m.info.unknown, {});
});

/* random buildings written by hand in each format and compared with the Python reference (bit widths 2..7) */
const NAMES = ['stone', 'oak_planks', 'glass', 'dirt', 'cobblestone', 'bricks', 'white_wool', 'oak_log', 'spruce_planks', 'sand', 'gravel', 'andesite',
  'granite', 'diorite', 'deepslate', 'tuff', 'calcite', 'mud_bricks', 'end_stone', 'netherrack', 'basalt', 'blackstone', 'obsidian', 'ice', 'snow_block',
  'clay', 'terracotta', 'red_wool', 'blue_wool', 'lime_wool', 'quartz_block', 'prismarine', 'sea_lantern', 'glowstone', 'bookshelf', 'hay_block', 'melon',
  'pumpkin', 'moss_block', 'mud', 'packed_mud', 'copper_block', 'iron_block', 'gold_block', 'emerald_block', 'diamond_block', 'lapis_block', 'coal_block',
  'amethyst_block', 'honeycomb_block', 'sponge', 'bone_block', 'magma_block', 'shroomlight', 'crying_obsidian', 'purpur_block', 'smooth_stone', 'cut_sandstone',
  'chiseled_stone_bricks', 'mossy_cobblestone', 'cracked_stone_bricks', 'polished_andesite', 'polished_diorite', 'polished_granite', 'oak_stairs', 'oak_slab'];
function randomBuild(seed, kinds) {
  let s = seed; const rnd = () => (s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  const w = 1 + Math.floor(rnd() * 12), h = 1 + Math.floor(rnd() * 6), l = 1 + Math.floor(rnd() * 12), pal = [{ Name: 'minecraft:air' }];
  for (let i = 0; i < kinds; i++) { const n = NAMES[i % NAMES.length]; pal.push(n === 'oak_stairs' ? { Name: 'minecraft:' + n, Properties: { facing: 'east', half: 'top', shape: 'straight', waterlogged: 'false' } } : { Name: 'minecraft:' + n }); }
  const vals = []; for (let i = 0; i < w * h * l; i++) vals.push(rnd() < 0.4 ? 0 : 1 + Math.floor(rnd() * kinds));
  return { w, h, l, pal, vals };
}
const st = (e) => e.Name + (e.Properties ? '[' + Object.keys(e.Properties).map((k) => k + '=' + e.Properties[k]).join(',') + ']' : '');
function writeLitematic(b) {
  const bits = Math.max(2, 32 - Math.clz32(b.pal.length - 1)), la = new C.LongArray(Math.ceil(b.vals.length * bits / 64));
  b.vals.forEach((v, i) => { for (let k = 0; k < bits; k++) if (v >> k & 1) { const q = i * bits + k; la.u32[q >> 5] |= 1 << (q & 31); } });
  return gz(C.writeNbt({ Version: 6, MinecraftDataVersion: 5023, Metadata: {}, Regions: { r: { Position: { x: 3, y: -2, z: 5 }, Size: { x: b.w, y: b.h, z: b.l }, BlockStatePalette: b.pal, BlockStates: la } } }, ''));
}
function writeSchem(b) {
  const pal = {}; b.pal.forEach((e, i) => { pal[st(e)] = i; });
  const d = []; b.vals.forEach((v) => { while (v >= 128) { d.push((v & 127) | 128); v >>>= 7; } d.push(v); });
  return gz(C.writeNbt({ Schematic: { Version: 3, DataVersion: 5023, Width: C.tag(2, b.w), Height: C.tag(2, b.h), Length: C.tag(2, b.l), Blocks: { Palette: pal, Data: Int8Array.from(d.map((x) => (x << 24) >> 24)) } } }, ''));
}
test('random Sponge v3 and Litematica files (palettes 3..66, spanning longs) match the Python reference', async () => {
  for (let t = 0; t < 16; t++) {
    const b = randomBuild(777 + t * 31, 2 + (t * 4) % 65);
    for (const [ext, data] of [['schem', writeSchem(b)], ['litematic', writeLitematic(b)]]) {
      const f = tmp('fuzz-' + t + '.' + ext, data), m = await C.readFile(f, new Uint8Array(data), {}, gunzip);
      assert.equal(m.info.format, ext);
      assert.equal(C.serialize(m), ref(f), ext + ' case ' + t); fs.unlinkSync(f);
    }
  }
});

test('old ids are renamed (DataVersion aware), wall booleans mapped, cauldron levels split; reference equal', () => {
  const nbt = { DataVersion: 1500, size: [8, 1, 1], entities: [], palette: [{ Name: 'minecraft:grass_path' }, { Name: 'minecraft:sign', Properties: { rotation: '4', waterlogged: 'false' } },
    { Name: 'minecraft:grass' }, { Name: 'minecraft:chain', Properties: { axis: 'x', waterlogged: 'false' } }, { Name: 'minecraft:cobblestone_wall', Properties: { east: 'true', north: 'false', south: 'false', west: 'true', up: 'true', waterlogged: 'false' } },
    { Name: 'minecraft:stone_slab', Properties: { type: 'top', waterlogged: 'false' } }, { Name: 'minecraft:cauldron', Properties: { level: '2' } }, { Name: 'minecraft:cauldron', Properties: { level: '0' } }],
    blocks: [0, 1, 2, 3, 4, 5, 6, 7].map((i) => ({ pos: [i, 0, 0], state: i })) };
  const m = C.structureToModel(nbt), code = C.serialize(m);
  for (const s of ['dirt_path 0,0,0', 'oak_sign[rotation=4] 1,0,0', 'short_grass 2,0,0', 'iron_chain[axis=x] 3,0,0', 'cobblestone_wall[east=low,west=low] 4,0,0',
    'smooth_stone_slab[type=top] 5,0,0', 'water_cauldron[level=2] 6,0,0', 'cauldron 7,0,0']) assert.ok(code.includes(s), s + '\n' + code);
  assert.deepEqual(m.info.unknown, {}); assert.equal(m.info.renamed.grass_path, 'dirt_path');
  const f = tmp('renames.nbt', zlib.gzipSync(Buffer.from(C.writeNbt(nbt, ''))));
  assert.equal(code, ref(f)); fs.unlinkSync(f);
  // a modern file keeps stone_slab
  nbt.DataVersion = 5023; assert.ok(C.serialize(C.structureToModel(nbt)).includes('stone_slab[type=top] 5,0,0'));
});

test('unknown blocks: listed, kept by default, can be skipped or replaced; blocks without item counted', () => {
  const nbt = { size: [4, 1, 1], entities: [{}], palette: [{ Name: 'othermod:gizmo', Properties: { lit: 'true' } }, { Name: 'minecraft:fire', Properties: { age: '0' } }, { Name: 'simplebuilding:blueprint_table' }, { Name: 'minecraft:stone' }],
    blocks: [{ pos: [0, 0, 0], state: 0 }, { pos: [1, 0, 0], state: 1 }, { pos: [2, 0, 0], state: 3, nbt: { x: 1 } }, { pos: [3, 0, 0], state: 0 }] };
  const keep = C.structureToModel(nbt);
  assert.equal(keep.info.unknown['othermod:gizmo'], 2); assert.equal(keep.info.noItem['minecraft:fire'], 1);
  assert.equal(keep.info.entities, 1); assert.equal(keep.info.blockEntities, 1);
  assert.ok(C.serialize(keep).includes('othermod:gizmo[lit=true] 0,0,0 3,0,0'));
  const skip = C.structureToModel(nbt, { unknown: 'skip' });
  assert.equal(skip.n, 2); assert.deepEqual(skip.info.unknown, {});
  const rep = C.structureToModel(nbt, { unknown: 'minecraft:oak_planks' });
  assert.ok(C.serialize(rep).includes('oak_planks 0,0,0 3,0,0'));
  assert.ok(C.isKnown('simplebuilding:blueprint_table') || C.DATA.blocks.simplebuilding.length > 0);
});

test('Axiom .bp is recognised, broken files give readable error codes', async () => {
  await assert.rejects(C.readFile('a.bp', new Uint8Array([0x0a, 0xe5, 0xbb, 0x36, 0, 0, 0, 0]), {}, gunzip), /axiom_bp/);
  await assert.rejects(C.readFile('x.nbt', new Uint8Array([1, 2, 3, 4]), {}, gunzip), /not_nbt/);
  await assert.rejects(C.readFile('x.nbt', gz(C.writeNbt({ foo: 1 }, '')), {}, gunzip), /unknown_format/);
  await assert.rejects(C.readFile('x.nbt', gz(C.writeNbt({ Width: C.tag(2, 1), Height: C.tag(2, 1), Length: C.tag(2, 1), Palette: {}, BlockData: new Int8Array(0), Version: 2 }, '').slice(0, 20)), {}, gunzip), /not_nbt/);
});

test('tiles: a building over 256 is cut into 256er tiles, offsets put every cell back in place', () => {
  const bld = C.builder(C.newInfo('test'));
  for (let x = 0; x < 300; x++) for (let z = 0; z < 3; z++) bld.put(x, x % 4, z, bld.state(x % 7 ? 'minecraft:stone' : 'minecraft:glass', {}));
  const m = bld.done();
  assert.equal(m.info.tooBig, true);
  const r = C.toTiles(m);
  assert.equal(r.error, null); assert.equal(r.tiles, 2);
  const back = new Map();
  r.parts.forEach((p) => {
    assert.match(p.code, /^# \d+x\d+x\d+, \d+ blocks\n# tile \d\/2, part 1\/1, place at \+\d+,\d+,\d+\n/);
    C.expand(p.code).forEach((s, k) => back.set(C.key(C.kx(k) + p.offset[0], C.ky(k) + p.offset[1], C.kz(k) + p.offset[2]), s));
    const parsed = C.parseCode(p.code); assert.ok(parsed.ok, JSON.stringify(parsed.problems));
  });
  assert.deepEqual(Array.from(back.entries()).sort((a, b) => a[0] - b[0]), cellsOf(m));
  const small = C.toTiles(C.structureToModel({ size: [1, 1, 1], palette: [{ Name: 'minecraft:stone' }], blocks: [{ pos: [0, 0, 0], state: 0 }] }));
  assert.equal(small.tiles, 1); assert.equal(small.parts.length, 1);
});

test('code parser: parse(serialize(m)) == m for the examples and the format fuzz', async () => {
  for (const f of ['hut.nbt', 'tower.nbt', 'mini.schematic', 'hut.litematic']) {
    const m = await read(f), r = C.parseCode(C.serialize(m));
    assert.ok(r.ok, f + JSON.stringify(r.problems.slice(0, 3)));
    assert.deepEqual(cellsOf(C.normalized(r.model)), cellsOf(m), f);
  }
  const big = C.toCodes(await read('tower.nbt'));
  big.parts.forEach((p) => assert.ok(C.parseCode(p.code).ok));
});

test('code parser: language features of docs/BLUEPRINT.md', () => {
  const hut = C.parseCode('# Huette 5x6x5\n$dach = oak_stairs[facing=north]\ncobblestone 0..4,0,0..4\noak_planks 0..4,1..3,0..4\nair 1..3,1..3,1..3          # innen hohl\n' +
    'glass_pane 2,2,0\noak_door 2,1,4 \noak_door[half=upper] 2,2,4\n$dach 0..4,4,0\noak_fence 0,5,0*3@2,0,0');
  assert.ok(hut.ok, JSON.stringify(hut.problems));
  const at = (r, x, y, z) => r.model.cells.get(C.key(x, y, z));
  assert.equal(at(hut, 2, 2, 2), undefined); assert.equal(at(hut, 0, 4, 0), 'oak_stairs'); assert.equal(at(hut, 4, 5, 0), 'oak_fence');
  assert.equal(at(hut, 2, 2, 4), 'oak_door[half=upper]'); assert.equal(hut.model.n, 25 + 75 - 27 + 5 + 3);
  const vars = C.parseCode('$r = 5\n$h = $r * 2 + 1             # 11\n$n = ($h + 1) / 4           # 3\nstone 0..$r,0,0..$r-1\noak_fence 0,1,0*$n+1@2,0,0\ndirt ($r*3),0,20\nsand 9,9,9..9,9,((-7 / 2) + 14)');
  assert.ok(vars.ok, JSON.stringify(vars.problems));
  assert.equal(Array.from(vars.model.cells.values()).filter((s) => s === 'stone').length, 30);
  assert.deepEqual([0, 2, 4, 6].map((x) => at(vars, x, 1, 0)), ['oak_fence', 'oak_fence', 'oak_fence', 'oak_fence']);
  assert.equal(at(vars, 15, 0, 20), 'dirt'); assert.equal(at(vars, 9, 9, 10), 'sand'); assert.equal(at(vars, 9, 9, 9), 'sand');
  const line = C.parseCode('stone line(0,0,0..4,2,0)');
  assert.deepEqual(Array.from(line.model.cells.keys()).map((k) => [C.kx(k), C.ky(k), C.kz(k)].join(',')).sort(), ['0,0,0', '1,1,0', '2,1,0', '3,2,0', '4,2,0']);
  const sph = C.parseCode('glass sphere(8,8,8,5)'), hol = C.parseCode('glass hollow_sphere(8,8,8,5)'), dome = C.parseCode('glass dome(8,8,8,5)');
  assert.ok(hol.model.n < sph.model.n && hol.model.n > 0); hol.model.cells.forEach((s, k) => assert.ok(sph.model.cells.has(k)));
  dome.model.cells.forEach((s, k) => { assert.ok(C.ky(k) >= 8); assert.ok(sph.model.cells.has(k)); });
  sph.model.cells.forEach((s, k) => assert.ok(sph.model.cells.has(C.key(16 - C.kx(k), C.ky(k), C.kz(k))))); // symmetric
  assert.ok(sph.model.cells.has(C.key(13, 8, 8)) && !sph.model.cells.has(C.key(14, 8, 8)));
  const cyl = C.parseCode('stone cylinder(5,0,5,2,3)'), pyr = C.parseCode('stone pyramid(5,0,5,2,3)'), bx = C.parseCode('stone hollow_box(0,0,0,3,3,3)');
  assert.equal(cyl.model.sy, 3); assert.ok(pyr.model.n < 25 * 3); assert.equal(bx.model.n, 26);
  const px = C.parseCode('$a = stone\n$b = dirt\npixels 0,5,0 3a2b.a');
  assert.ok(px.ok); assert.equal(px.pixels, 6); assert.equal(at(px, 3, 5, 0), 'dirt'); assert.equal(at(px, 5, 5, 0), undefined); assert.equal(at(px, 6, 5, 0), 'stone');
});

test('code parser: errors keep their Java keys and the other lines', () => {
  const keys = (code) => C.parseCode(code).problems.map((p) => p.key);
  assert.deepEqual(keys('stone'), ['no_region']);
  assert.deepEqual(keys('stone 0,0,$x'), ['unknown_variable']);
  assert.deepEqual(keys('$z = 0\nstone (4/$z),0,0'), ['division_by_zero']);
  const cr = C.parseCode('glass sphere(128,128,128,200)').problems[0];
  assert.equal(cr.key, 'coordinate_range'); assert.equal(cr.args[0], '-72');
  assert.deepEqual(keys('stone 0,0,0*300@1,0,0'), ['repeat_range']);
  assert.deepEqual(keys('stone 0,0'), ['bad_region']);
  assert.deepEqual(keys('$nope 0,0,0'), ['unknown_alias']);
  assert.deepEqual(keys('stone[facing 0,0,0'), ['unclosed_properties', 'no_region']); // the open bracket swallows the line, as in Java
  assert.deepEqual(keys('stone star(1,2,3)'), ['unknown_shape']);
  assert.deepEqual(keys('stone 250,0,0*3@4,0,0'), ['repeat_outside']);
  assert.deepEqual(keys('$a = stone\n$b = $a'), ['alias_of_alias']);
  assert.deepEqual(keys('$a = stone\nstone 0,0,$a'), ['alias_as_number']);
  assert.deepEqual(keys('stone 0..255,0..255,0..255'), ['too_many_cells']);
  assert.deepEqual(keys('STONE 0,0,0'), ['bad_block_id']);
  const mixed = C.parseCode('stone 0,0,0\nbroken 0,0\ndirt 1,0,0');
  assert.equal(mixed.model.n, 2); assert.equal(mixed.problems[0].line, 1);
  const w = C.parseCode('othermod:gizmo 0,0,0'); assert.ok(w.ok); assert.equal(w.warnings['othermod:gizmo'], 1);
});

test('way back: code -> .nbt and .schem v3 -> same cells; the Python reference reads the written files', async () => {
  const m = await read('hut.nbt'), code = C.serialize(m), parsed = C.parseCode(code).model;
  for (const kind of ['nbt', 'schem']) {
    const file = await C.writeFile(parsed, kind, gz), back = await C.readFile('back.' + kind, file, {}, gunzip);
    assert.equal(back.info.format, kind); assert.equal(C.serialize(back), code, kind);
    const f = tmp('back.' + kind, file); assert.equal(ref(f), code, kind + ' reference'); fs.unlinkSync(f);
  }
  const off = C.parseCode('stone 5,1,5\nglass 6,1,5'), nb = C.toStructureNbt(off.model);
  assert.deepEqual(nb.size, [7, 2, 6]); assert.equal(nb.DataVersion, C.DATA.mc.dataVersion);
  assert.equal(C.DATA.mc.dataVersion, 5023);
});

test('meshes: .obj+.mtl, .gltf and .glb become a cube model within the editor limits; gltf == glb', () => {
  const obj = C.meshToModel(C.parseObj(fs.readFileSync(path.join(EX, 'house.obj'), 'utf8'), fs.readFileSync(path.join(EX, 'house.mtl'), 'utf8')), { name: 'house' });
  const code = C.modelCode(obj.model), j = JSON.parse(code);
  assert.ok(j.elements.length > 1 && j.elements.length <= 256);
  assert.deepEqual(Object.keys(j.textures), ['particle', 'wall', 'roof']); assert.equal(j.textures.roof, 'mymodels:block/house_roof');
  assert.deepEqual(obj.textures.map((t) => t.color.slice(0, 3)), [[230, 217, 179], [153, 38, 26]]);
  j.elements.forEach((e) => [...e.from, ...e.to].forEach((v) => assert.ok(v >= -16 && v <= 32)));
  const glb = C.meshToModel(C.parseGlb(bytes('house.glb')), { name: 'house' }), gltf = C.meshToModel(C.parseGltf(fs.readFileSync(path.join(EX, 'house.gltf'), 'utf8')), { name: 'house' });
  assert.equal(C.modelCode(glb.model), C.modelCode(gltf.model));
  const fine = C.meshToModel(C.parseObj(fs.readFileSync(path.join(EX, 'house.obj'), 'utf8')), { res: 64, size: 48, fill: false });
  assert.ok(fine.model.elements.length <= 256); C.modelCode(fine.model);
  assert.throws(() => C.parseObj('v 0 0 0\nf 1 2 3'), /obj_bad_face/);
  assert.throws(() => C.parseGltf({ asset: {}, extensionsRequired: ['KHR_draco_mesh_compression'] }), /gltf_compressed/);
  const ext = JSON.parse(fs.readFileSync(path.join(EX, 'house.gltf'), 'utf8')); const raw = Buffer.from(ext.buffers[0].uri.split(',')[1], 'base64'); ext.buffers[0].uri = 'house.bin';
  assert.throws(() => C.parseGltf(ext, null, {}), /gltf_missing_bin/);
  assert.equal(C.modelCode(C.meshToModel(C.parseGltf(ext, null, { 'house.bin': new Uint8Array(raw) }), { name: 'house' }).model), C.modelCode(gltf.model));
});

test('model way back: code -> .bbmodel -> code is stable; embedded textures and the resource pack', () => {
  const warns = [], a = C.modelCode(C.bbmodelToVanilla(JSON.parse(fs.readFileSync(path.join(EX, 'stool.bbmodel'), 'utf8')), (w) => warns.push(w)));
  const bb = C.modelToBbmodel(JSON.parse(a), 'Stool'), b = C.modelCode(C.bbmodelToVanilla(JSON.parse(JSON.stringify(bb))));
  assert.equal(b, a); assert.equal(bb.name, 'stool'); assert.equal(bb.elements.length, 2);
  const j = C.modelCode(JSON.parse(fs.readFileSync(path.join(EX, 'stool.json'), 'utf8'))), jb = C.modelCode(C.bbmodelToVanilla(C.modelToBbmodel(JSON.parse(j), 'stool')));
  assert.deepEqual(Object.keys(JSON.parse(jb).textures).sort(), ['leg', 'particle', 'top']); assert.equal(JSON.parse(jb).textures.top, 'mymodels:block/stool_top');
  const tex = C.bbTextures(JSON.parse(fs.readFileSync(path.join(EX, 'stool_tex.bbmodel'), 'utf8')));
  assert.equal(tex.length, 1); assert.equal(tex[0].name, 'stool_top');
  const png = tex[0].data; assert.deepEqual(Array.from(png.slice(1, 4)), [80, 78, 71]);
  const idat = png.indexOf(0x49) ; assert.ok(idat > 0);
  const pk = C.resourcePack('My Stool!', a, tex.map((t) => ({ name: t.name, data: t.data })));
  const names = []; let p = 0; const dv = new DataView(pk.buffer);
  while (dv.getUint32(p, true) === 0x04034b50) {
    const crc = dv.getUint32(p + 14, true), size = dv.getUint32(p + 18, true), nl = dv.getUint16(p + 26, true), name = Buffer.from(pk.slice(p + 30, p + 30 + nl)).toString();
    const data = pk.slice(p + 30 + nl, p + 30 + nl + size); assert.equal(C.crc32(data), crc, name); names.push(name);
    if (name === 'pack.mcmeta') assert.equal(JSON.parse(Buffer.from(data).toString()).pack.min_format, 97);
    p += 30 + nl + size;
  }
  assert.deepEqual(names, ['pack.mcmeta', 'assets/mymodels/models/item/my_stool.json', 'assets/mymodels/items/my_stool.json', 'assets/mymodels/textures/block/stool_top.png']);
  const solid = C.solidPng([10, 20, 30]), body = solid.slice(41, solid.length - 16);
  const pix = zlib.inflateSync(Buffer.from(body)); assert.equal(pix.length, 16 * (16 * 4 + 1)); assert.deepEqual(Array.from(pix.slice(1, 5)), [10, 20, 30, 255]);
});
