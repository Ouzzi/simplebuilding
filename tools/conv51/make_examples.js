// Generates own example files for the N51 prototype (no third-party builds). Usage: node make_examples.js <outdir>
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const C = require('../../wiki/assets/conv/conv-core.js');
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
console.log('wrote', out);
