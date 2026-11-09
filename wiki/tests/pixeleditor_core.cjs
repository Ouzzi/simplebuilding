// Plain node (no browser): logic of wiki/assets/pixeleditor-core.js. Run by wiki/tests/test_pixeleditor.py.
//   node wiki/tests/pixeleditor_core.cjs                 self-tests
//   node wiki/tests/pixeleditor_core.cjs compose F...    prints {file: hex RGBA of the composition} (compared with layers.py)
//   node wiki/tests/pixeleditor_core.cjs decode P...     prints {file: hex RGBA} of PNGs (own decoder)
//   node wiki/tests/pixeleditor_core.cjs encode F OUT    writes the composition of F as PNG to OUT
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const C = require(path.resolve(__dirname, '../assets/pixeleditor-core.js'));
const rnd2 = x => Math.floor(x + 0.5);
const hex = a => Buffer.from(a).toString('hex');

async function selfTests() {
  // format round trip and text identical to layers.py dumps() for the pilot texture
  const pilot = path.resolve(__dirname, '../../src/main/resources/assets/simplebuilding/textures/item/iron_chisel.layers.json');
  if (fs.existsSync(pilot)) {
    const text = fs.readFileSync(pilot, 'utf8');
    const model = C.fromDoc(JSON.parse(text));
    assert.equal(C.dumps(C.toDoc(model)), text, 'toDoc(fromDoc(x)) writes the same text');
  }
  // composition: top wins, hidden ignored, opacity
  const m = C.blank(2, 1, 'a');
  C.plot(m.layers[0].px, 2, 1, [[0, 0], [1, 0]], [255, 0, 0, 255]);
  m.layers.push(C.newLayer(2, 1, 'b'));
  C.plot(m.layers[1].px, 2, 1, [[0, 0]], [0, 0, 0, 255]);
  m.layers[1].opacity = 50;
  assert.deepEqual(Array.from(C.compose(m)), [127, 0, 0, 255, 255, 0, 0, 255]);
  m.layers[1].visible = false;
  assert.deepEqual(Array.from(C.compose(m)), [255, 0, 0, 255, 255, 0, 0, 255]);
  // merge down keeps the look
  m.layers[1].visible = true;
  const before = Array.from(C.compose(m));
  assert.ok(C.mergeDown(m, 1));
  assert.equal(m.layers.length, 1);
  assert.deepEqual(Array.from(C.compose(m)), before);
  // palette order: loaded colours first, unused dropped
  const p = C.fromRgba(2, 1, new Uint8ClampedArray([1, 2, 3, 255, 0, 0, 0, 0]), 'x');
  assert.deepEqual(p.palette, ['#010203ff']);
  p.palette = ['#ffffffff', '#010203ff'];
  C.plot(p.layers[0].px, 2, 1, [[1, 0]], [255, 255, 255, 255]);
  assert.deepEqual(C.toDoc(p).palette, ['#ffffffff', '#010203ff']);
  assert.deepEqual(C.toDoc(p).layers[0].rows, ['0100']);
  // drawing helpers
  assert.deepEqual(C.linePoints(0, 0, 3, 1), [[0, 0], [1, 0], [2, 1], [3, 1]]);
  assert.equal(C.rectPoints(0, 0, 2, 2, false).length, 8);
  assert.equal(C.rectPoints(0, 0, 2, 2, true).length, 9);
  assert.deepEqual(C.pixelPerfect([[0, 0], [1, 0], [1, 1], [2, 1]]), [[0, 0], [1, 1], [2, 1]]);
  assert.deepEqual(C.mirror([[0, 0]], 4, 4, { x: true, y: true }).sort(), [[0, 0], [0, 3], [3, 0], [3, 3]].sort());
  // square brush 1/2/3 (2 grows right/down, 3 is centred, overlaps counted once)
  assert.deepEqual(C.brush([[2, 2]], 1), [[2, 2]]);
  assert.deepEqual(C.brush([[2, 2]], 2), [[2, 2], [3, 2], [2, 3], [3, 3]]);
  assert.equal(C.brush([[2, 2]], 3).length, 9);
  assert.deepEqual(C.brush([[2, 2]], 3)[0], [1, 1]);
  assert.equal(C.brush([[0, 0], [1, 0]], 2).length, 6);
  const f = new Uint8ClampedArray(3 * 3 * 4);
  C.plot(f, 3, 3, [[1, 0], [1, 1], [1, 2]], [9, 9, 9, 255]);
  assert.equal(C.fill(f, 3, 3, 0, 0, [5, 5, 5, 255], false).length, 3, 'wall stops the fill');
  assert.equal(C.fill(f, 3, 3, 1, 1, [7, 7, 7, 255], true).length, 3, 'global replaces the colour');
  const lock = new Uint8ClampedArray(2 * 4);
  C.plot(lock, 2, 1, [[0, 0], [1, 0]], [1, 1, 1, 255], true);
  assert.deepEqual(Array.from(lock), [0, 0, 0, 0, 0, 0, 0, 0], 'alpha lock paints only existing pixels');
  // region ops
  const r = new Uint8ClampedArray(2 * 2 * 4);
  C.plot(r, 2, 2, [[0, 0]], [1, 0, 0, 255]);
  assert.ok(C.transformRect(r, 2, 2, { x: 0, y: 0, w: 2, h: 2 }, C.rotateBuf));
  assert.equal(r[(0 * 2 + 1) * 4], 1, 'top-left goes top-right on a clockwise turn');
  assert.ok(C.transformRect(r, 2, 2, { x: 0, y: 0, w: 2, h: 2 }, b => C.flipBuf(b, true)));
  assert.equal(r[0], 1);
  assert.equal(C.transformRect(r, 2, 2, { x: 0, y: 0, w: 2, h: 1 }, C.rotateBuf), false, 'no rotation of a non-square');
  // history
  const h = new C.History();
  const doc = C.blank(1, 1);
  h.push(doc);
  C.plot(doc.layers[0].px, 1, 1, [[0, 0]], [1, 1, 1, 255]);
  const undone = h.undo(doc);
  assert.equal(undone.layers[0].px[3], 0);
  assert.equal(h.redo(undone).layers[0].px[3], 255);
  // PNG: compressed and stored deflate both decode to the same pixels
  const px = new Uint8ClampedArray([10, 20, 30, 40, 50, 60, 70, 128, 0, 0, 0, 0, 255, 255, 255, 255]);
  for (const def of [undefined, b => Promise.resolve(C.deflateStored(b))]) {
    const png = await C.pngEncode(2, 2, px, def);
    const back = await C.pngDecode(png);
    assert.deepEqual(Array.from(back.rgba), Array.from(px));
  }
  assert.equal(C.crc32(Buffer.from('IEND')), 0xAE426082);
  assert.deepEqual(C.validate({ format: 'x' }).length, 1);
  // b-texed16: blend modes = tools/textures/layers.py on the shared vectors (same JSON, every byte)
  const vectors = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../tools/textures/tests/blend_vectors.json'), 'utf8'));
  assert.deepEqual(new Set(vectors.map(v => v.mode)), new Set(C.BLENDS));
  for (const v of vectors) {
    const bm = C.blank(1, 1, 'b');
    bm.layers[0].px.set(v.back[3] ? v.back : [0, 0, 0, 0]);
    const top = C.newLayer(1, 1, 's'); top.px.set(v.src); top.opacity = v.opacity; top.blend = v.mode;
    bm.layers.push(top);
    assert.deepEqual(Array.from(C.compose(bm)), v.out, JSON.stringify(v));
  }
  // blend survives toDoc/fromDoc, missing blend = normal, unknown blend rejected
  const bd = C.blank(1, 1, 'x'); bd.layers[0].blend = 'soft-light'; C.plot(bd.layers[0].px, 1, 1, [[0, 0]], [9, 9, 9, 255]);
  const bdoc = C.toDoc(bd);
  assert.equal(bdoc.layers[0].blend, 'soft-light');
  assert.equal(C.fromDoc(bdoc).layers[0].blend, 'soft-light');
  delete bdoc.layers[0].blend; assert.equal(C.fromDoc(bdoc).layers[0].blend, 'normal');
  bdoc.layers[0].blend = 'dodge'; assert.ok(C.validate(bdoc).length);
  assert.equal(C.cloneModel(bd).layers[0].blend, 'soft-light');
  // painting with opacity mixes over the layer pixel (source-over), not replace
  const pa = new Uint8ClampedArray([255, 0, 0, 255, 0, 0, 0, 0]);
  C.plot(pa, 2, 1, [[0, 0], [1, 0], [0, 0]], [0, 0, 255, 128]);
  assert.deepEqual(Array.from(pa), [127, 0, 128, 255, 0, 0, 255, 128], 'over red: mixed; empty: colour with its alpha; once per pixel');
  const pb = new Uint8ClampedArray([255, 255, 255, 128]);
  C.plot(pb, 1, 1, [[0, 0]], [0, 0, 0, 128]);   // rest = 128*127/255 = 63.75, oa = 191.75
  assert.deepEqual(Array.from(pb), [rnd2(255 * 63.75 / 191.75), rnd2(255 * 63.75 / 191.75), rnd2(255 * 63.75 / 191.75), 192]);
  const pl = new Uint8ClampedArray([255, 0, 0, 100, 0, 0, 0, 0]);
  C.plot(pl, 2, 1, [[0, 0], [1, 0]], [0, 0, 255, 128], true);   // alpha lock: colour mixes, alpha stays, empty stays empty
  assert.deepEqual(Array.from(pl), [127, 0, 128, 100, 0, 0, 0, 0]);
  const pz = new Uint8ClampedArray([255, 0, 0, 255]);
  C.plot(pz, 1, 1, [[0, 0]], [0, 0, 255, 255]); assert.deepEqual(Array.from(pz), [0, 0, 255, 255], 'opaque colour replaces');
  C.plot(pz, 1, 1, [[0, 0]], [0, 0, 0, 0]); assert.deepEqual(Array.from(pz), [0, 0, 0, 0], 'transparent colour clears');
  // eraser reduces alpha by its strength; full strength clears
  const pe = new Uint8ClampedArray([10, 20, 30, 200, 10, 20, 30, 1]);
  C.erase(pe, 2, 1, [[0, 0], [1, 0], [0, 0]], 128);
  assert.deepEqual(Array.from(pe), [10, 20, 30, 100, 0, 0, 0, 0]);   // 200*127/255 = 99.6 -> 100, 1*127/255 -> 0 = empty
  C.erase(pe, 2, 1, [[0, 0]], 255); assert.deepEqual(Array.from(pe), [0, 0, 0, 0, 0, 0, 0, 0]);
  // fill with a translucent colour mixes too
  const pf = new Uint8ClampedArray([255, 0, 0, 255, 255, 0, 0, 255]);
  C.fill(pf, 2, 1, 0, 0, [0, 0, 255, 128], false, false);
  assert.deepEqual(Array.from(pf), [127, 0, 128, 255, 127, 0, 128, 255]);
  console.log('pixeleditor core: ok');
}

(async () => {
  const [cmd, ...args] = process.argv.slice(2);
  if (!cmd) return selfTests();
  if (cmd === 'compose') {
    const out = {};
    for (const f of args) out[f] = hex(C.compose(C.fromDoc(JSON.parse(fs.readFileSync(f, 'utf8')))));
    return console.log(JSON.stringify(out));
  }
  if (cmd === 'decode') {
    const out = {};
    for (const f of args) out[f] = hex((await C.pngDecode(new Uint8Array(fs.readFileSync(f)))).rgba);
    return console.log(JSON.stringify(out));
  }
  if (cmd === 'encode') {
    const m = C.fromDoc(JSON.parse(fs.readFileSync(args[0], 'utf8')));
    fs.writeFileSync(args[1], await C.pngEncode(m.w, m.h, C.compose(m)));
    return;
  }
  throw new Error('unknown command ' + cmd);
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
