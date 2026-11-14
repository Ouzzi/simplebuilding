// Generates own example files for the N51 prototype (no third-party builds). Usage: node make_examples.js <outdir>
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const C = require('../../wiki/assets/conv/conv-core.js');
require('../../wiki/assets/conv/conv-formats.js'); require('../../wiki/assets/conv/conv-mesh.js');
const out = process.argv[2] || path.join(__dirname, '../../wiki/tests/conv51/examples');
fs.mkdirSync(out, { recursive: true });
const I = (v) => v; // numbers are ints in the writer
function structure(w, h, d, place) {
  const palette = [], idx = new Map(), blocks = [];
  const state = (name, props) => { const k = name + JSON.stringify(props || {}); if (!idx.has(k)) { idx.set(k, palette.length); const e = { Name: name }; if (props) e.Properties = props; palette.push(e); } return idx.get(k); };
  for (let y = 0; y < h; y++) for (let z = 0; z < d; z++) for (let x = 0; x < w; x++) {
    const b = place(x, y, z); if (!b) continue;
    blocks.push({ pos: [x, y, z], state: state(b[0], b[1]) });
  }
  return { DataVersion: 4440, size: [w, h, d], palette, blocks, entities: [] };
}
const STAIR = (f, half) => ['minecraft:oak_stairs', { facing: f, half: half || 'bottom', shape: 'straight', waterlogged: 'false' }];
const hut = structure(9, 8, 9, (x, y, z) => {
  const edge = x === 0 || x === 8 || z === 0 || z === 8;
  if (y === 0) return ['minecraft:cobblestone'];
  if (y >= 1 && y <= 3) {
    if (edge && (x === 0 || x === 8) && (z === 0 || z === 8)) return ['minecraft:oak_log', { axis: 'y' }];
    if (edge) {
      if (z === 0 && x === 4 && y <= 2) return ['minecraft:oak_door', { facing: 'south', half: y === 1 ? 'lower' : 'upper', hinge: 'left', open: 'false', powered: 'false' }];
      if (y === 2 && (x === 2 || x === 6 || z === 4 && (x === 0 || x === 8))) return ['minecraft:glass_pane', { north: 'false', east: 'false', south: 'false', west: 'false', waterlogged: 'false' }];
      return ['minecraft:oak_planks'];
    }
    return null;
  }
  if (y >= 4 && y <= 6) { // gable roof along x
    const level = y - 4;
    if (z === level) return STAIR('south');
    if (z === 8 - level) return STAIR('north');
    if (z > level && z < 8 - level && edge) return ['minecraft:oak_planks'];
    return null;
  }
  if (y === 7 && z === 4 && x === 4) return ['minecraft:lantern', { hanging: 'false', waterlogged: 'false' }];
  return null;
});
const tower = structure(40, 36, 40, (x, y, z) => {
  const dx = x - 19.5, dz = z - 19.5, r = Math.hypot(dx, dz);
  if (y === 0 && r < 19) return ['minecraft:stone_bricks'];
  if (r < 19 && r >= 17.5 && y > 0 && y < 33) {
    const name = ['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks', 'minecraft:andesite'][(x * 7 + y * 13 + z * 3) % 4];
    if (y % 8 === 4 && (x + z) % 5 === 0) return null;
    return [name];
  }
  if (y >= 33 && r < 20 && r >= 18) return ['minecraft:stone_brick_slab', { type: 'bottom', waterlogged: 'false' }];
  return null;
});
const write = (name, s) => fs.writeFileSync(path.join(out, name), zlib.gzipSync(Buffer.from(C.writeNbt(s, ''))));
// numbers in palette Properties are strings; ints for pos/size/state
function norm(s) { return JSON.parse(JSON.stringify(s)); }
write('hut.nbt', norm(hut)); write('tower.nbt', norm(tower));
// Vanilla JSON model + bbmodel (own: a small stool)
const stool = { parent: 'minecraft:block/block', textures: { top: 'mymodels:block/stool_top', leg: 'mymodels:block/stool_leg', particle: '#leg' },
  elements: [
    { name: 'seat', from: [2, 8, 2], to: [14, 10, 14], faces: { down: { texture: '#top', cullface: 'down' }, up: { texture: '#top' }, north: { uv: [2, 6, 14, 8], texture: '#leg' }, south: { uv: [2, 6, 14, 8], texture: '#leg' }, west: { uv: [2, 6, 14, 8], texture: '#leg' }, east: { uv: [2, 6, 14, 8], texture: '#leg' } } },
    { name: 'leg', from: [3, 0, 3], to: [5, 8, 5], faces: { north: { uv: [3, 8, 5, 16], texture: '#leg' }, south: { uv: [3, 8, 5, 16], texture: '#leg' } } },
    { name: 'brace', from: [7, 3, 7.5], to: [9, 4.25, 8.5], rotation: { origin: [8, 3, 8], axis: 'y', angle: 22.5 }, faces: { up: { texture: '#leg', uv: [0, 0, 2, 1] } } } ],
  display: { gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] } } };
fs.writeFileSync(path.join(out, 'stool.json'), JSON.stringify(stool, null, 2));
const bb = { meta: { format_version: '4.10', model_format: 'java_block', box_uv: false }, name: 'stool', resolution: { width: 16, height: 16 },
  elements: [
    { name: 'seat', type: 'cube', from: [2, 8, 2], to: [14, 10, 14], rotation: [0, 0, 0], origin: [8, 8, 8], faces: { north: { uv: [2, 6, 14, 8], texture: 1 }, east: { uv: [2, 6, 14, 8], texture: 1 }, south: { uv: [2, 6, 14, 8], texture: 1 }, west: { uv: [2, 6, 14, 8], texture: 1 }, up: { uv: [2, 2, 14, 14], texture: 0 }, down: { uv: [2, 2, 14, 14], texture: 0, cullface: 'down' } } },
    { name: 'leg', type: 'cube', from: [3, 0, 3], to: [5, 8, 5], rotation: [0, 22.5, 0], origin: [4, 0, 4], faces: { north: { uv: [3, 8, 5, 16], texture: 1 }, south: { uv: [3, 8, 5, 16], texture: 1 }, up: { uv: [3, 3, 5, 5], texture: null } } } ],
  textures: [{ name: 'stool_top.png' }, { name: 'stool_leg.png' }], display: { gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] } } };
fs.writeFileSync(path.join(out, 'stool.bbmodel'), JSON.stringify(bb, null, 1));

// ---- phase 2: the hut in every building format (own build; formats written by hand, not via the converter's writers) ----
const gz = (b) => zlib.gzipSync(Buffer.from(b));
const stateText = (e) => e.Name + (e.Properties ? '[' + Object.keys(e.Properties).map((k) => k + '=' + e.Properties[k]).join(',') + ']' : '');
const W = 9, H = 8, L = 9, grid = new Array(W * H * L).fill(-1); // index (y*L+z)*W+x -> palette index of hut
hut.blocks.forEach((b) => { grid[(b.pos[1] * L + b.pos[2]) * W + b.pos[0]] = b.state; });
function varints(vals) { const o = []; vals.forEach((v) => { while (v >= 128) { o.push((v & 127) | 128); v >>>= 7; } o.push(v); }); return Int8Array.from(o.map((x) => (x << 24) >> 24)); }
function spongePalette() { const pal = { 'minecraft:air': 0 }; hut.palette.forEach((e, i) => { pal[stateText(e)] = i + 1; }); return pal; }
const data = varints(grid.map((g) => g + 1));
const T = C.tag;
// v3: {Schematic:{Version 3, Blocks{Palette, Data}}}; v2: flat, BlockData + DataVersion; v1: flat, no DataVersion, TileEntities
fs.writeFileSync(path.join(out, 'hut.schem'), gz(C.writeNbt({ Schematic: { Version: 3, DataVersion: 4440, Width: T(2, W), Height: T(2, H), Length: T(2, L),
  Offset: new Int32Array([0, 0, 0]), Blocks: { Palette: spongePalette(), Data: data, BlockEntities: [] } } }, '')));
fs.writeFileSync(path.join(out, 'hut_v2.schem'), gz(C.writeNbt({ Version: 2, DataVersion: 4440, Width: T(2, W), Height: T(2, H), Length: T(2, L),
  PaletteMax: Object.keys(spongePalette()).length, Palette: spongePalette(), BlockData: data, BlockEntities: [] }, 'Schematic')));
fs.writeFileSync(path.join(out, 'hut_v1.schem'), gz(C.writeNbt({ Version: 1, Width: T(2, W), Height: T(2, H), Length: T(2, L),
  PaletteMax: Object.keys(spongePalette()).length, Palette: spongePalette(), BlockData: data, TileEntities: [] }, 'Schematic')));

// Litematica: two regions (walls+floor y 0..3 with positive size; roof y 4..7 with negative size), spanning bit packing
function litRegion(y0, y1, negative) {
  const pal = [{ Name: 'minecraft:air' }], map = new Map(), vals = [];
  const sy = y1 - y0 + 1;
  for (let y = 0; y < sy; y++) for (let z = 0; z < L; z++) for (let x = 0; x < W; x++) {
    const g = grid[((y0 + y) * L + z) * W + x];
    if (g < 0) { vals.push(0); continue; }
    if (!map.has(g)) { map.set(g, pal.length); pal.push(hut.palette[g]); }
    vals.push(map.get(g));
  }
  const bits = Math.max(2, 32 - Math.clz32(pal.length - 1)), longs = Math.ceil(vals.length * bits / 64), la = new C.LongArray(longs);
  vals.forEach((v, i) => { for (let b = 0; b < bits; b++) if (v >> b & 1) { const k = i * bits + b; la.u32[k >> 5] |= 1 << (k & 31); } });
  // negative size: Position is the far corner (min = pos + size + 1)
  const pos = negative ? { x: W - 1, y: y1, z: L - 1 } : { x: 0, y: y0, z: 0 }, size = negative ? { x: -W, y: -sy, z: -L } : { x: W, y: sy, z: L };
  return { Position: pos, Size: size, BlockStatePalette: pal, BlockStates: la, TileEntities: [], Entities: [], PendingBlockTicks: [] };
}
fs.writeFileSync(path.join(out, 'hut.litematic'), gz(C.writeNbt({ Version: 6, MinecraftDataVersion: 4440,
  Metadata: { Name: 'Hut', Author: 'SimpleBuilding tests', RegionCount: 2, EnclosingSize: { x: W, y: H, z: L } },
  Regions: { walls: litRegion(0, 3, false), roof: litRegion(4, 7, true) } }, '')));

// MCEdit .schematic with 1.12 numeric ids: stone variants, wool, logs, stairs, slabs, torch, a door, a sunflower
const SW = 5, SH = 4, SL = 3, ids = new Int8Array(SW * SH * SL), meta = new Int8Array(SW * SH * SL);
const put = (x, y, z, id, m) => { const i = (y * SL + z) * SW + x; ids[i] = (id << 24) >> 24; meta[i] = m || 0; };
for (let x = 0; x < SW; x++) for (let z = 0; z < SL; z++) put(x, 0, z, 1, x % 7);       // stone, granite, polished granite, diorite, polished diorite
put(0, 1, 0, 35, 14); put(1, 1, 0, 35, 11);                                             // red and blue wool
put(2, 1, 0, 17, 4 | 1); put(3, 1, 0, 17, 12);                                          // spruce log axis x, oak wood (bark)
put(4, 1, 0, 53, 1 | 4);                                                                // oak stairs facing west, top
put(0, 1, 1, 44, 8 | 3); put(1, 1, 1, 126, 2);                                         // cobblestone slab top, birch slab bottom
put(2, 1, 1, 50, 3); put(3, 1, 1, 50, 5);                                               // wall torch facing south, standing torch
put(4, 1, 2, 64, 3 | 4); put(4, 2, 2, 64, 8 | 1);                                       // oak door lower (north, open) + upper (hinge right)
put(0, 1, 2, 175, 0); put(0, 2, 2, 175, 8);                                             // sunflower lower + upper
put(1, 1, 2, 159, 4); put(2, 1, 2, 98, 3); put(3, 3, 0, 9, 0);                         // yellow terracotta, chiseled stone bricks, water
fs.writeFileSync(path.join(out, 'mini.schematic'), gz(C.writeNbt({ Width: T(2, SW), Height: T(2, SH), Length: T(2, SL), Materials: 'Alpha',
  Blocks: ids, Data: meta, Entities: [], TileEntities: [] }, 'Schematic')));

// ---- meshes: a little house (box + gable roof), as .obj/.mtl, embedded .gltf and .glb ----
const V = [[0, 0, 0], [4, 0, 0], [4, 0, 4], [0, 0, 4], [0, 3, 0], [4, 3, 0], [4, 3, 4], [0, 3, 4], [2, 5, 0], [2, 5, 4]];
const WALL = [[0, 1, 5, 4], [1, 2, 6, 5], [2, 3, 7, 6], [3, 0, 4, 7], [0, 3, 2, 1]], ROOF = [[4, 5, 8], [6, 7, 9], [5, 6, 9, 8], [7, 4, 8, 9]];
let obj = '# own test mesh: house\nmtllib house.mtl\n' + V.map((v) => 'v ' + v.join(' ')).join('\n') + '\nusemtl wall\n' + WALL.map((f) => 'f ' + f.map((i) => i + 1).join(' ')).join('\n') +
  '\nusemtl roof\n' + ROOF.map((f) => 'f ' + f.map((i) => i + 1).join(' ')).join('\n') + '\n';
fs.writeFileSync(path.join(out, 'house.obj'), obj);
fs.writeFileSync(path.join(out, 'house.mtl'), 'newmtl wall\nKd 0.9 0.85 0.7\nnewmtl roof\nKd 0.6 0.15 0.1\n');
const tris = (faces) => faces.flatMap((f) => f.slice(1, -1).map((_, k) => [f[0], f[k + 1], f[k + 2]]));
const pos = new Float32Array(V.flat()), wi = new Uint16Array(tris(WALL).flat()), ri = new Uint16Array(tris(ROOF).flat());
const binBuf = Buffer.concat([Buffer.from(pos.buffer), Buffer.from(wi.buffer), Buffer.from(ri.buffer)]);
const gltf = (uri) => ({ asset: { version: '2.0', generator: 'SimpleBuilding conv51 tests' }, scene: 0, scenes: [{ nodes: [0] }],
  nodes: [{ mesh: 0, translation: [10, 0, 0], scale: [2, 2, 2] }],
  meshes: [{ primitives: [{ attributes: { POSITION: 0 }, indices: 1, material: 0 }, { attributes: { POSITION: 0 }, indices: 2, material: 1 }] }],
  materials: [{ name: 'wall', pbrMetallicRoughness: { baseColorFactor: [0.8, 0.7, 0.45, 1] } }, { name: 'roof', pbrMetallicRoughness: { baseColorFactor: [0.35, 0.02, 0.01, 1] } }],
  buffers: [uri ? { byteLength: binBuf.length, uri } : { byteLength: binBuf.length }],
  bufferViews: [{ buffer: 0, byteOffset: 0, byteLength: pos.byteLength }, { buffer: 0, byteOffset: pos.byteLength, byteLength: wi.byteLength }, { buffer: 0, byteOffset: pos.byteLength + wi.byteLength, byteLength: ri.byteLength }],
  accessors: [{ bufferView: 0, componentType: 5126, count: V.length, type: 'VEC3', min: [0, 0, 0], max: [4, 5, 4] },
    { bufferView: 1, componentType: 5123, count: wi.length, type: 'SCALAR' }, { bufferView: 2, componentType: 5123, count: ri.length, type: 'SCALAR' }] });
fs.writeFileSync(path.join(out, 'house.gltf'), JSON.stringify(gltf('data:application/octet-stream;base64,' + binBuf.toString('base64')), null, 1));
const j = Buffer.from(JSON.stringify(gltf(null))), jp = Buffer.concat([j, Buffer.alloc((4 - j.length % 4) % 4, 0x20)]), bp = Buffer.concat([binBuf, Buffer.alloc((4 - binBuf.length % 4) % 4)]);
const hdr = Buffer.alloc(12); hdr.writeUInt32LE(0x46546c67, 0); hdr.writeUInt32LE(2, 4); hdr.writeUInt32LE(12 + 8 + jp.length + 8 + bp.length, 8);
const ch = (len, type) => { const b = Buffer.alloc(8); b.writeUInt32LE(len, 0); b.writeUInt32LE(type, 4); return b; };
fs.writeFileSync(path.join(out, 'house.glb'), Buffer.concat([hdr, ch(jp.length, 0x4e4f534a), jp, ch(bp.length, 0x004e4942), bp]));
// a .bbmodel with one embedded 2x2 texture (own pixels)
const bbt = JSON.parse(JSON.stringify(bb)); bbt.name = 'stool_tex';
bbt.textures[0].source = 'data:image/png;base64,' + Buffer.from(C.png(2, 2, new Uint8Array([255, 0, 0, 255, 0, 255, 0, 255, 0, 0, 255, 255, 255, 255, 255, 255]))).toString('base64');
fs.writeFileSync(path.join(out, 'stool_tex.bbmodel'), JSON.stringify(bbt));
console.log('wrote', out);
